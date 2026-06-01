import http from 'k6/http';
import {check, sleep} from 'k6';
import {BASE_URL, getHeaders} from '../config/config.js';

const DEFAULT_LIMIT = Number(__ENV.USER_LIST_LIMIT || 20);
const SORT_BY = __ENV.USER_SORT_BY || 'createdAt';
const SORT_DIRECTION = __ENV.USER_SORT_DIRECTION || 'DESCENDING';
const TEST_USER_ID = __ENV.TEST_USER_ID || __ENV.USER_ID;
const RUN_ADMIN_USER_LIST = (__ENV.RUN_ADMIN_USER_LIST || 'false').toLowerCase() === 'true';

if (!TEST_USER_ID) {
    throw new Error('환경변수 TEST_USER_ID를 설정하세요. 예: -e TEST_USER_ID=user-uuid');
}

function parseJson(body) {
    try {
        return JSON.parse(body);
    } catch {
        return null;
    }
}

/**
 * 사용자 도메인 시나리오
 *
 * 흐름:
 * 1. TEST_USER_ID 대상 사용자 상세 조회
 * 2. TEST_USER_ID 대상 현재 시청 세션 조회
 * 3. RUN_ADMIN_USER_LIST=true인 경우 사용자 목록 조회 및 목록 내 사용자 상세 조회
 */
export function userScenario(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    // 1. 사용자 상세 조회
    const detailRes = http.get(`${BASE_URL}/api/users/${TEST_USER_ID}`, {
        headers,
        tags: { name: 'GET /api/users/{userId}' },
    });

    check(detailRes, {
        '사용자 상세 조회 성공': (r) => r.status === 200,
        '사용자 상세 조회 ID 일치': (r) => {
            const body = parseJson(r.body);
            return body && body.id === TEST_USER_ID;
        },
    });

    if (detailRes.status !== 200) {
        console.log('[사용자 상세 조회 실패] status=' + detailRes.status);
    }

    sleep(1);

    // 2. 현재 시청 세션 조회 (세션이 없으면 null 응답도 정상)
    const watchingSessionRes = http.get(`${BASE_URL}/api/users/${TEST_USER_ID}/watching-sessions`, {
        headers,
        tags: { name: 'GET /api/users/{userId}/watching-sessions' },
    });

    check(watchingSessionRes, {
        '현재 시청 세션 조회 성공': (r) => r.status === 200,
    });

    if (watchingSessionRes.status !== 200) {
        console.log('[현재 시청 세션 조회 실패] status=' + watchingSessionRes.status);
    }

    sleep(1);

    // 3. 관리자 계정으로 실행할 때만 사용자 목록 조회
    if (!RUN_ADMIN_USER_LIST) {
        return;
    }

    const listUrl = `${BASE_URL}/api/users?limit=${DEFAULT_LIMIT}&sortBy=${SORT_BY}&sortDirection=${SORT_DIRECTION}`;
    const listRes = http.get(listUrl, {
        headers,
        tags: { name: 'GET /api/users' },
    });

    check(listRes, {
        '사용자 목록 조회 성공': (r) => r.status === 200,
    });

    if (listRes.status !== 200) {
        console.log('[사용자 목록 조회 실패] status=' + listRes.status);
        return;
    }

    const listBody = parseJson(listRes.body);
    const users = listBody && Array.isArray(listBody.data) ? listBody.data : [];
    if (users.length === 0) {
        return;
    }

    sleep(1);

    // 4. 목록에서 임의 사용자 상세 조회
    const randomUser = users[Math.floor(Math.random() * users.length)];
    const targetUserId = randomUser.id;
    if (!targetUserId) {
        return;
    }

    const listedDetailRes = http.get(`${BASE_URL}/api/users/${targetUserId}`, {
        headers,
        tags: { name: 'GET /api/users/{userId}' },
    });

    check(listedDetailRes, {
        '목록 사용자 상세 조회 성공': (r) => r.status === 200,
    });

    if (listedDetailRes.status !== 200) {
        console.log('[목록 사용자 상세 조회 실패] status=' + listedDetailRes.status);
    }

    sleep(1);
}
