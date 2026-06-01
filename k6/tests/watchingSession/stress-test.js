import {sleep} from 'k6';
import {watchingSessionScenario} from "../../scenarios/watchingSession.js";
import {login} from "../../config/config.js";

/**
 * Stress Test — 한계점까지 점진적으로 올려서 어디서 깨지는지 확인
 *
 * 1. Warm-up:    0 → 30 VU   (1분)
 * 2. Normal:    30 VU        (3분)
 * 3. High:      30 → 100 VU  (1분)
 * 4. Peak:      100 VU       (3분)
 * 5. Overload:  100 → 200 VU (1분)
 * 6. Max:       200 VU       (3분)
 * 7. Recovery:  200 → 0 VU   (1분)
 */

const stages = [
    { duration: '1m', target: 30 },
    { duration: '3m', target: 30 },
    { duration: '1m', target: 100 },
    { duration: '3m', target: 100 },
    { duration: '1m', target: 200 },
    { duration: '3m', target: 200 },
    { duration: '1m', target: 0 },
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