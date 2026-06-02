import {sleep} from 'k6';
import {watchingSessionScenario} from "../../scenarios/watchingSession.js";
import {login} from "../../config/config.js";

/**
 * Spike Test — 급격한 트래픽 증가 대응 확인
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
        watchingSession: {
            executor: 'ramping-vus',
            stages,
            exec: 'watchingSessionTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        http_req_failed: ['rate<0.01'],

        'http_req_duration{name:GET /api/contents}': ['p(95)<300'],
        'http_req_duration{name:GET /api/contents/{contentId}}': ['p(95)<300'],
        'http_req_duration{name:GET /api/contents?keyword=}': ['p(95)<1000'],
    },
};

export function setup() {
    return login();
}

export function watchingSessionTest(data) {
    watchingSessionScenario(data.accessToken, data.csrfToken);
    sleep(1);
}