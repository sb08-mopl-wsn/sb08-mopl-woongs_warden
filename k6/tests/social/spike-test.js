import {sleep} from 'k6';
import {login} from '../../config/social_config.js';
import { runWarmup, runSubscriber, runTrigger } from '../../scenarios/_social.js';

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

// 💡 [수정 포인트 1] 30초 만에 200명으로 폭발적인 조회를 퍼부을 스케줄 (인프라 충격 유도)
const subscriberStages = [
    { duration: '1m', target: 10 },
    { duration: '30s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 0 },
];

// 💡 [수정 포인트 2] 데이터 오염을 막고 순수 비즈니스 레이턴시 변동을 관측할 1인 스케줄
const triggerStages = [
    { duration: '1m', target: 1 },
    { duration: '30s', target: 1 },
    { duration: '1m', target: 1 },
    { duration: '30s', target: 1 },
    { duration: '1m', target: 1 },
    { duration: '30s', target: 0 },
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
            stages: subscriberStages, // 💡 분리된 폭발적 조회 스케줄 매핑
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
        // [글로벌 기준] 전체 요청에 대한 임계치 (스트레스/스파이크 상황을 고려해 p95 기준을 1~2초로 완화)
        http_req_duration: ['p(95)<2000', 'p(99)<4000'],
        http_req_failed: ['rate<0.01'], // 전체 에러율 1% 미만 격리

        // 💡 [작성자님 도메인 전용 상세 임계치 매핑]
        // 대량 조회가 일어나는 압박조 API (목표: 부하 속에서도 1초 이내 방어)
        'http_req_duration{name:GET /api/conversations (Thread Pressure)}': ['p(95)<1000'],
        'http_req_duration{name:GET /api/notifications (Thread Pressure)}': ['p(95)<1000'],

        // 핵심 비즈니스 로직을 수행하는 쓰기 API (목표: 0.5초 이내 빠른 처리)
        'http_req_duration{name:POST /api/follows}': ['p(95)<500'],
        'http_req_duration{name:POST /api/conversations/{id}/direct-messages}': ['p(95)<500'],
        'http_req_duration{name:DELETE /api/follows/{followId} (State Reset)}': ['p(95)<500'],
    },
};

export function setup() {
    return login();
}

// ── 테스트 매핑 함수 ──
export function socialWarmupTest(data) {
    runWarmup(data.accessToken, data.csrfToken);
    sleep(1);
}

// 수정된 _social.js 배관에 맞춰 대화방 + 알림 목록 복합 조회를 수행합니다.
export function socialSubscriberTest(data) {
    runSubscriber(data.accessToken, data.csrfToken);
    sleep(1);
}

// 수정된 _social.js 배관에 맞춰 충돌 없이 [팔로우 -> DM -> 언팔로우]를 수행합니다.
export function socialTriggerTest(data) {
    runTrigger(data.accessToken, data.csrfToken);
    sleep(1);
}