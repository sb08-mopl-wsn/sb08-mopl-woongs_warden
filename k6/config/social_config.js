import http from 'k6/http';
import {check, fail} from 'k6';

// ─── 환경변수 ───
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

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
    // 1. CSRF 토큰 명시적 발급 요청 (Spring Security 6+ 대응)
    const csrfRes = http.get(`${BASE_URL}/api/auth/csrf-token`);
    let csrfToken = null;
    
    // 쿠키에서 추출 시도
    const initialCookies = csrfRes.headers['Set-Cookie'];
    if (initialCookies) {
        const cookieArr = Array.isArray(initialCookies) ? initialCookies : [initialCookies];
        for (const c of cookieArr) {
            const xsrf = c.match(/XSRF-TOKEN=([^;]+)/);
            if (xsrf && xsrf[1]) csrfToken = xsrf[1];
        }
    }
    
    // 바디에서 추출 시도 (만약 백엔드가 JSON으로 준다면)
    try {
        const csrfBody = JSON.parse(csrfRes.body);
        if (csrfBody.token) csrfToken = csrfBody.token;
    } catch (e) {}

    // 2. 로그인 요청
    const loginHeaders = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfToken) {
        loginHeaders['X-XSRF-TOKEN'] = csrfToken;
    }

    const res = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        { username: TEST_EMAIL, password: TEST_PASSWORD }, // k6가 알아서 x-www-form-urlencoded로 변환함
        { headers: loginHeaders }
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

    // Set-Cookie에서 REFRESH_TOKEN, (혹시 모를) XSRF-TOKEN 갱신 추출
    let refreshToken = null;
    const cookies = res.headers['Set-Cookie'];
    if (cookies) {
        const cookieArr = Array.isArray(cookies) ? cookies : [cookies];
        for (const c of cookieArr) {
            const xsrf = c.match(/XSRF-TOKEN=([^;]+)/);
            if (xsrf && xsrf[1]) csrfToken = xsrf[1]; // 최신 토큰으로 덮어쓰기
            const refresh = c.match(/REFRESH_TOKEN=([^;]+)/);
            if (refresh && refresh[1]) refreshToken = refresh[1];
        }
    }

    const body = JSON.parse(res.body);
    return {
        accessToken: body.accessToken,
        csrfToken: csrfToken,
        refreshToken: refreshToken,
    };
}