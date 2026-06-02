import { sleep } from 'k6';
import { login } from '../../config/user_config.js';
import { userScenario } from '../../scenarios/user.js';

/**
 * User Spike Test — 사용자 상세 조회/이름 변경의 급격한 트래픽 대응 확인
 *
 * 1. Baseline:  0 → 10 VU   (1분)
 * 2. Spike:     10 → 200 VU (30초)
 * 3. Peak:      200 VU      (1분)
 * 4. Recovery:  200 → 10 VU (30초)
 * 5. Normal:    10 VU       (1분)
 * 6. Ramp-down: 10 → 0 VU   (30초)
 */

const stages = [
    { duration: '1m', target: 10 },
    { duration: '30s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
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
    userScenario(data);
    sleep(1);
}