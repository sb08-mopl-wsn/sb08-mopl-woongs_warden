import http from 'k6/http';
import {check, fail} from 'k6';

// ─── 환경변수 ───
// export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const BASE_URL = __ENV.BASE_URL || 'https://woongsanam.kro.kr';

if (!__ENV.TEST_EMAIL || !__ENV.TEST_PASSWORD) {
    throw new Error(
        '환경변수 BASE_URL(default: localhost), TEST_EMAIL, TEST_PASSWORD를 설정하세요.\n' +
        '예: k6 run -e BASE_URL=xxx -e TEST_EMAIL=xxx -e TEST_PASSWORD=xxx k6/tests/load-test.js'
    );
}

const TEST_EMAIL = __ENV.TEST_EMAIL;
const TEST_PASSWORD = __ENV.TEST_PASSWORD;

// ─── 공통 헤더 ───
export function getHeaders(token, csrfToken) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
        headers['Cookie'] = `XSRF-TOKEN=${csrfToken}`;
    }
    return headers;
}

// ─── 인증 ───
export function login() {
    const res = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        { username: TEST_EMAIL, password: TEST_PASSWORD }
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

    // 💡 이 아래 쿠키 파싱 부분만 k6 내장 객체를 사용하도록 유지
    let csrfToken = null;
    if (res.cookies['XSRF-TOKEN'] && res.cookies['XSRF-TOKEN'].length > 0) {
        csrfToken = res.cookies['XSRF-TOKEN'][0].value;
    }

    let refreshToken = null;
    if (res.cookies['REFRESH_TOKEN'] && res.cookies['REFRESH_TOKEN'].length > 0) {
        refreshToken = res.cookies['REFRESH_TOKEN'][0].value;
    }

    const body = JSON.parse(res.body);
    return {
        accessToken: body.accessToken,
        csrfToken: csrfToken,
        refreshToken: refreshToken,
    };
}