import {sleep} from 'k6';
import {login} from '../../config/config.js';
//import {contentScenario} from '../scenarios/content.js';
import {reviewScenario} from '../../scenarios/review.js';

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
        // 💡 콘텐츠 시나리오 끄기
        // content: {
        //     executor: 'ramping-vus',
        //     stages,
        //     exec: 'contentTest',
        // },

        review: {
            executor: 'ramping-vus',
            stages,
            exec: 'reviewTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
     //   http_req_failed: ['rate<0.01'],

        'http_req_duration{name:GET /api/reviews}': ['p(95)<300'],
        'http_req_duration{name:POST /api/reviews}': ['p(95)<1000'],
    },
};

export function setup() {
    return login();
}

// export function contentTest(data) {
//     contentScenario(data.accessToken, data.csrfToken);
//     sleep(1);
// }

export function reviewTest(data) {
    reviewScenario(data.accessToken, data.csrfToken);
}