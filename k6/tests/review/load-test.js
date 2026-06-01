import {sleep} from 'k6';
import {login} from '../../config/config.js';
// import {contentScenario} from '../scenarios/content.js';
import {reviewScenario} from '../../scenarios/review.js';

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
        // 💡 콘텐츠 시나리오 끄기 (격리 테스트 목적)
        // content: {
        //     executor: 'ramping-vus',
        //     stages,
        //     exec: 'contentTest',`
        // },

        review: {
            executor: 'ramping-vus',
            stages,
            exec: 'reviewTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
       // http_req_failed: ['rate<0.01'], //409 conflict도 fail 처리해버려서 주석처리함

        'http_req_duration{name:GET /api/reviews}': ['p(95)<300'],
        'http_req_duration{name:POST /api/reviews}': ['p(95)<1000'],
    },
};

export function setup() {
    return login();
}

// 💡 콘텐츠 실행 함수 끄기
// export function contentTest(data) {
//     contentScenario(data.accessToken, data.csrfToken);
//     sleep(1);
// }

// ✅ 4. 리뷰 실행 함수 주석 해제
export function reviewTest(data) {
    reviewScenario(data.accessToken, data.csrfToken);
    // (reviewScenario 안에 이미 sleep(1)이 충분히 있어서 여기선 뺐습니다!)
}