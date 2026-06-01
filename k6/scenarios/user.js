import http from 'k6/http';
import {check, sleep} from 'k6';
import encoding from 'k6/encoding';
import {__ITER, __VU} from 'k6';
import {BASE_URL, getHeaders} from '../config/config.js';

/**
 * 사용자 도메인 시나리오
 *
 * 흐름:
 * 1. Access Token의 userId 클레임으로 사용자 상세 조회
 * 2. 같은 사용자 이름 변경
 */
export function userScenario(token, csrfToken) {
    const userId = getUserIdFromToken(token);

    if (!userId) {
        console.error('[에러] Access Token에서 userId를 찾을 수 없습니다.');
        return;
    }

    const headers = getHeaders(token, csrfToken);

    const detailRes = http.get(`${BASE_URL}/api/users/${userId}`, {
        headers,
        tags: {name: 'GET /api/users/{userId}'},
    });

    check(detailRes, {
        '사용자 상세 조회 성공': (r) => r.status === 200,
        '사용자 상세 조회 응답에 userId 포함': (r) => r.json('id') === userId,
    });

    if (detailRes.status !== 200) {
        console.log(`[사용자 상세 조회 실패] status=${detailRes.status}, body=${detailRes.body}`);
        return;
    }

    sleep(1);

    const newName = `k6-user-${__VU}-${__ITER}-${Date.now()}`;
    const updatePayload = {
        request: http.file(JSON.stringify({name: newName}), 'request.json', 'application/json'),
    };
    const updateHeaders = {...headers};
    delete updateHeaders['Content-Type'];

    const updateRes = http.patch(`${BASE_URL}/api/users/${userId}`, updatePayload, {
        headers: updateHeaders,
        tags: {name: 'PATCH /api/users/{userId}'},
    });

    check(updateRes, {
        '사용자 이름 변경 성공': (r) => r.status === 200,
        '사용자 이름 변경 응답에 변경된 이름 포함': (r) => r.json('name') === newName,
    });

    if (updateRes.status !== 200) {
        console.log(`[사용자 이름 변경 실패] status=${updateRes.status}, body=${updateRes.body}`);
    }

    sleep(1);
}

function getUserIdFromToken(token) {
    if (!token) {
        return null;
    }

    try {
        const payload = token.split('.')[1];
        const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
        const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=');
        const decodedPayload = encoding.b64decode(paddedPayload, 's');
        const claims = JSON.parse(decodedPayload);

        return claims.userId || null;
    } catch (error) {
        console.error(`[에러] Access Token 파싱 실패: ${error}`);
        return null;
    }
}
