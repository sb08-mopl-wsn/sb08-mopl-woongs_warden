import http from 'k6/http';
import {check, sleep} from 'k6';
import {BASE_URL, getHeaders} from '../config/user_config.js';

export function userScenario(auth) {
    const userId = auth.userId;
    const newName = `k6-user-${Date.now()}`;

    const payload = {
        request: http.file(
            JSON.stringify({
                name: newName,
            }),
            'request.json',
            'application/json'
        ),
    };

    const headers = getHeaders(
        auth.accessToken,
        auth.csrfToken
    );

    delete headers['Content-Type'];

    const updateRes = http.patch(
        `${BASE_URL}/api/users/${userId}`,
        payload,
        {
            headers,
            tags: {
                name: 'PATCH /api/users/{userId}',
            },
        }
    );

    check(updateRes, {
        '사용자 이름 변경 성공': (r) => r.status === 200,
        '사용자 이름 변경 응답 이름 일치': (r) => {
            try {
                return JSON.parse(r.body).name === newName;
            } catch {
                return false;
            }
        },
    });

    if (updateRes.status !== 200) {
        console.log(
            `[사용자 이름 변경 실패] status=${updateRes.status}, body=${updateRes.body}`
        );
    }

    sleep(1);
}

function requestUserDetail(auth, userId) {
    return http.get(`${BASE_URL}/api/users/${userId}`, {
        headers: getHeaders(auth.accessToken, auth.csrfToken),
        tags: {name: 'GET /api/users/{userId}'},
    });
}

function requestUserNameUpdate(auth, updatePayload) {
    const headers = getHeaders(auth.accessToken, auth.csrfToken);
    delete headers['Content-Type'];

    return http.patch(`${BASE_URL}/api/users/${auth.userId}`, updatePayload, {
        headers,
        tags: {name: 'PATCH /api/users/{userId}'},
    });
}

function refreshAuth(auth) {
    const refreshedAuth = renewAuth(auth);
    if (!refreshedAuth) {
        return false;
    }

    auth.accessToken = refreshedAuth.accessToken;
    auth.userId = refreshedAuth.userId;
    auth.csrfToken = refreshedAuth.csrfToken;
    auth.refreshToken = refreshedAuth.refreshToken;
    auth.targetUserId = refreshedAuth.targetUserId;
    auth.email = refreshedAuth.email;
    auth.password = refreshedAuth.password;
    return true;
}