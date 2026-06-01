import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, getHeaders } from '../config/social_config.js';

// 실제 DB에 존재하는 타겟 유저의 UUID
const TARGET_USER_ID = __ENV.TARGET_USER_ID || '57f0701b-022b-4e6c-840f-501d243265d5';

/**
 * 💡 [역할 1] 웜업 시나리오
 */
export function runWarmup(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);
    const res = http.get(`${BASE_URL}/api/conversations`, {
        headers,
        tags: { name: 'WARMUP /api/conversations' },
    });
    check(res, { '[Warmup] 예열 성공': (r) => r.status === 200 });
    sleep(1);
}

/**
 * 💡 [역할 2] 톰캣 스레드 및 인프라 압박 시나리오
 */
export function runSubscriber(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    // [도메인 1] 대화방 목록 조회 부하
    const convRes = http.get(`${BASE_URL}/api/conversations`, {
        headers,
        tags: { name: 'GET /api/conversations (Thread Pressure)' },
    });
    check(convRes, { '[Read 부하] 대화방 조회 성공': (r) => r.status === 200 });

    // [도메인 2] 알림 목록 조회 부하
    const notiRes = http.get(`${BASE_URL}/api/notifications?size=10`, {
        headers,
        tags: { name: 'GET /api/notifications (Thread Pressure)' },
    });
    check(notiRes, { '[Read 부하] 알림 목록 조회 성공': (r) => r.status === 200 });

    sleep(2);
}

/**
 * 💡 [역할 3] 순수 비즈니스 로직 성능 측정 시나리오
 */
export function runTrigger(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    const followPayload = JSON.stringify({
        followeeId: TARGET_USER_ID
    });

    // 1. 팔로우 요청 실행
    const followRes = http.post(`${BASE_URL}/api/follows`, followPayload, {
        headers,
        tags: { name: 'POST /api/follows' },
    });

    check(followRes, {
        '[Follow] 팔로우 요청 완료': (r) => r.status === 200 || r.status === 201,
    });

    // ── 💡 [백엔드 findById 버그 완벽 우회 치트키] ──
    // 서버가 리턴해준 응답 바디(FollowDto)에서 관계 레코드의 진짜 PK ID를 추출합니다.
    let followRecordId = TARGET_USER_ID; // 만약의 파싱 실패를 대비한 Fallback 처리
    try {
        const resBody = JSON.parse(followRes.body);
        if (resBody.id) {
            followRecordId = resBody.id; // FollowDto의 일련번호 ID 추출
        } else if (resBody.followId) {
            followRecordId = resBody.followId;
        }
    } catch (e) {}

    if (followRes.status !== 200 && followRes.status !== 201) {
        console.log(`[🚨 팔로우 실패 원인] Status=${followRes.status} | Body=${followRes.body}`);
    }

    sleep(1);

    // 2. 대화방 목록 조회 및 DM 발송
    const convRes = http.get(`${BASE_URL}/api/conversations`, { headers });
    let conversationId = null;
    try {
        const body = JSON.parse(convRes.body);
        const list = body.data || body.content || [];
        if (list.length > 0) {
            conversationId = list[0].id || list[0].conversationId;
        }
    } catch (e) {}

    if (conversationId) {
        const dmPayload = JSON.stringify({ content: 'k6 우회 테스트 메시지' });
        const dmRes = http.post(`${BASE_URL}/api/conversations/${conversationId}/direct-messages`, dmPayload, {
            headers,
            tags: { name: 'POST /api/conversations/{id}/direct-messages' },
        });
        check(dmRes, { '[DM] 메시지 발송 완료': (r) => r.status === 200 || r.status === 201 });
    }

    sleep(1);

    // ── 💡 [우회 적용] 상대방 User ID가 아닌, 동적으로 추출한 Follow Record PK ID를 주소창에 바인딩 ──
    const deleteRes = http.del(`${BASE_URL}/api/follows/${followRecordId}`, null, {
        headers,
        tags: { name: 'DELETE /api/follows/{followId} (State Reset)' }
    });

    if (deleteRes.status !== 204 && deleteRes.status !== 200) {
        console.log(`[🚨 언팔로우 실패 원인] Status=${deleteRes.status} | Body=${deleteRes.body}`);
    }

    check(deleteRes, {
        '[Clear] 언팔로우 상태 초기화 성공': (r) => r.status === 200 || r.status === 204,
    });

    sleep(1);
}