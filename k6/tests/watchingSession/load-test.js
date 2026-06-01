import {sleep} from 'k6';
import {watchingSessionScenario} from "../../scenarios/watchingSession.js";
import {login} from "../../config/config.js";

/**
 * Load Test — 예상 트래픽 수준에서 정상 동작 확인
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
