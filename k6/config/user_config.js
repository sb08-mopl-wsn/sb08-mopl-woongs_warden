import http from 'k6/http';
import { check, fail } from 'k6';

// ─── 환경변수 ───
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function parseCsv(value) {
    if (!value) {
        return [];
    }

    return value
        .split(',')
        .map(v => v.trim())
        .filter(v => v.length > 0);
}

const TEST_EMAILS = parseCsv(__ENV.TEST_EMAILS);
const TEST_PASSWORDS = parseCsv(__ENV.TEST_PASSWORDS);

if (
    (!__ENV.TEST_EMAIL && TEST_EMAILS.length === 0) ||
    (!__ENV.TEST_PASSWORD && TEST_PASSWORDS.length === 0)
) {
    throw new Error(
        '환경변수 BASE_URL(default: localhost), TEST_EMAIL, TEST_PASSWORD를 설정하세요.\n' +
        '여러 VU가 사용자 시나리오를 실행할 때는 TEST_EMAILS, TEST_PASSWORDS 사용을 권장합니다.'
    );
}

const TEST_EMAIL = __ENV.TEST_EMAIL || TEST_EMAILS[0];
const TEST_PASSWORD = __ENV.TEST_PASSWORD || TEST_PASSWORDS[0];

// ─── 공통 헤더 ───
export function getHeaders(token, csrfToken) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
        headers['Cookie'] = `XSRF-TOKEN=${csrfToken}`;
    }

    return headers;
}

// ─── 로그인 ───
export function login() {
    const res = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        {
            username: TEST_EMAIL,
            password: TEST_PASSWORD,
        }
    );

    const success = check(res, {
        '로그인 성공 (200)': (r) => r.status === 200,
        '응답에 토큰 포함': (r) => {
            try {
                return JSON.parse(r.body).accessToken !== undefined;
            } catch {
                return false;
            }
        },
    });

    if (!success) {
        fail(`로그인 실패: status=${res.status}, body=${res.body}`);
    }

    let csrfToken = null;

    if (
        res.cookies['XSRF-TOKEN'] &&
        res.cookies['XSRF-TOKEN'].length > 0
    ) {
        csrfToken = res.cookies['XSRF-TOKEN'][0].value;
    }

    let refreshToken = null;

    if (
        res.cookies['REFRESH_TOKEN'] &&
        res.cookies['REFRESH_TOKEN'].length > 0
    ) {
        refreshToken = res.cookies['REFRESH_TOKEN'][0].value;
    }

    const body = JSON.parse(res.body);

    return {
        accessToken: body.accessToken,
        csrfToken,
        refreshToken,
        userId: body.userDto.id,
        email: TEST_EMAIL,
        password: TEST_PASSWORD,
    };
}

// ─── 토큰 재발급 ───
export function renewAuth(auth) {

    const headers = {
        'Content-Type': 'application/json',
    };

    if (auth.csrfToken) {
        headers['X-XSRF-TOKEN'] = auth.csrfToken;

        headers['Cookie'] =
            `XSRF-TOKEN=${auth.csrfToken}; REFRESH_TOKEN=${auth.refreshToken}`;
    }

    const res = http.post(
        `${BASE_URL}/api/auth/refresh`,
        null,
        {
            headers,
        }
    );

    if (res.status !== 200) {
        console.log(
            `[토큰 재발급 실패] status=${res.status}, body=${res.body}`
        );
        return null;
    }

    const body = JSON.parse(res.body);

    let csrfToken = auth.csrfToken;
    let refreshToken = auth.refreshToken;

    if (
        res.cookies['XSRF-TOKEN'] &&
        res.cookies['XSRF-TOKEN'].length > 0
    ) {
        csrfToken = res.cookies['XSRF-TOKEN'][0].value;
    }

    if (
        res.cookies['REFRESH_TOKEN'] &&
        res.cookies['REFRESH_TOKEN'].length > 0
    ) {
        refreshToken = res.cookies['REFRESH_TOKEN'][0].value;
    }

    return {
        accessToken: body.accessToken,
        refreshToken,
        csrfToken,
        userId: auth.userId,
        email: auth.email,
        password: auth.password,
    };
}