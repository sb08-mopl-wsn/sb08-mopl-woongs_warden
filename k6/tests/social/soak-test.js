import {sleep} from 'k6';
import {login} from '../../config/social_config.js';
import { runWarmup, runSubscriber, runTrigger } from '../../scenarios/_social.js';

/**
 * Soak Test — 장시간 낮은 부하에서 안정성 확인
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

// 💡 [수정 포인트 1] 대화방 + 알림 조회를 12분간 지속하여 스레드 및 메모리 누수를 감지할 스케줄
const subscriberStages = [
    { duration: '1m', target: 15 },
    { duration: '12m', target: 15 },
    { duration: '1m', target: 0 },
];

// 💡 [수정 포인트 2] 데이터 충돌을 완전히 피하면서 장시간 비즈니스 로직 안정성을 관측할 1인 스케줄
const triggerStages = [
    { duration: '1m', target: 1 },
    { duration: '12m', target: 1 },
    { duration: '1m', target: 0 },
];

export const options = {
    scenarios: {
        social_warmup: {
            executor: 'ramping-vus',
            stages: [{ duration: '1m', target: 10 }], // 1분 동안 10명으로 가볍게 예열
            exec: 'socialWarmupTest',
        },
        social_subscribers: {
            executor: 'ramping-vus',
            stages: subscriberStages, // 💡 분리된 조회용 지속 압박 스케줄 매핑
            exec: 'socialSubscriberTest',
            startTime: '1m', // 웜업이 끝나는 1분 뒤 정식 출발
        },
        social_triggers: {
            executor: 'ramping-vus',
            stages: triggerStages, // 💡 분리된 1인 코어 로직 측정 스케줄 매핑
            exec: 'socialTriggerTest',
            startTime: '1m',
        },
    },
    thresholds: {
        // 장시간 테스트이므로 가끔 일어나는 튐(Spike) 현상을 제외하고 평균적인 평탄도 유지 관측
        http_req_duration: ['p(95)<1000', 'p(99)<2500'],
        http_req_failed: ['rate<0.01'],

        // 💡 [수정 포인트 3] 컨텐츠 도메인을 제거하고 작성자님 도메인 전용 상세 임계치 정밀 매핑
        'http_req_duration{name:GET /api/conversations (Thread Pressure)}': ['p(95)<500'],
        'http_req_duration{name:GET /api/notifications (Thread Pressure)}': ['p(95)<500'],
        'http_req_duration{name:POST /api/follows}': ['p(95)<300'],
        'http_req_duration{name:POST /api/conversations/{id}/direct-messages}': ['p(95)<300'],
        'http_req_duration{name:DELETE /api/follows/{followId} (State Reset)}': ['p(95)<300'],
    },
};

export function setup() {
    return login();
}

// ── 💡 외부 노출 및 인식을 위한 정석 export 함수 매핑 ──
export function socialWarmupTest(data) {
    runWarmup(data.accessToken, data.csrfToken);
    sleep(1);
}

export function socialSubscriberTest(data) {
    runSubscriber(data.accessToken, data.csrfToken);
    sleep(1);
}

export function socialTriggerTest(data) {
    runTrigger(data.accessToken, data.csrfToken);
    sleep(1);
}