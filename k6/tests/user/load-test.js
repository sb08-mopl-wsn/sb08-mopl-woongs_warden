import {sleep} from 'k6';
import {login} from '../../config/config.js';
import {userScenario} from '../../scenarios/user.js';

/**
 * User Load Test — 예상 트래픽 수준에서 사용자 상세 조회/이름 변경 확인
 *
 * 1. Ramp-up:  0 → 30 VU (1분)
 * 2. Steady:  30 VU 유지  (5분)
 * 3. Ramp-down: 30 → 0 VU (30초)
 */

const stages = [
    { duration: '1m', target: 30 },
    { duration: '5m', target: 30 },
    { duration: '30s', target: 0 },
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
