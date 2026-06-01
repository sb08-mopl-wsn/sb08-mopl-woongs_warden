import {sleep} from 'k6';
import {login} from '../../config/config.js';
import {userScenario} from '../../scenarios/user.js';

/**
 * User Soak Test — 장시간 낮은 부하에서 사용자 상세 조회/이름 변경 안정성 확인
 *
 * 목적:
 * - 메모리 누수 감지
 * - 커넥션 풀 고갈 확인
 * - 장시간 운영 시 응답시간 저하 여부 확인
 *
 * 1. Ramp-up:   0 → 15 VU  (1분)
 * 2. Soak:     15 VU       (12분)
 * 3. Ramp-down: 15 → 0 VU  (1분)
 */

const stages = [
    { duration: '1m', target: 15 },
    { duration: '12m', target: 15 },
    { duration: '1m', target: 0 },
];

export const options = {
    scenarios: {
        user: {
            executor: 'ramping-vus',
            stages,
            exec: 'userTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        http_req_failed: ['rate<0.01'],

        'http_req_duration{name:GET /api/users/{userId}}': ['p(95)<300'],
        'http_req_duration{name:PATCH /api/users/{userId}}': ['p(95)<500'],
    },
};

export function setup() {
    return login();
}

export function userTest(data) {
    userScenario(data.accessToken, data.csrfToken);
    sleep(1);
}
