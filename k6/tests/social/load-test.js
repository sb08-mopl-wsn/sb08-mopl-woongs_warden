import { sleep } from 'k6';
import { login } from '../../config/social_config.js';
import { runWarmup, runSubscriber, runTrigger } from '../../scenarios/_social.js';

// 톰캣 스레드를 흔들 30명의 공통 스테이지
const subscriberStages = [
    { duration: '1m', target: 30 },
    { duration: '5m', target: 30 },
    { duration: '30s', target: 0 },
];

// 💡 [우회 핵심] 데이터 충돌을 피하기 위해 쓰기 행위자는 무조건 '최대 1명'으로 락을 겁니다.
const triggerStages = [
    { duration: '1m', target: 1 },
    { duration: '5m', target: 1 },
    { duration: '30s', target: 0 },
];

export const options = {
    scenarios: {
        // 1. 커넥션 풀 예열 (1분)
        social_warmup: {
            executor: 'ramping-vus',
            stages: [{ duration: '1m', target: 10 }],
            exec: 'socialWarmupTest',
        },
        // 2. 대화방 조회 API를 무한 연사하여 톰캣 스레드 고갈을 유도하는 대기조 (최대 30명)
        social_subscribers: {
            executor: 'ramping-vus',
            stages: subscriberStages,
            exec: 'socialSubscriberTest',
            startTime: '1m',
        },
        // 3. 톰캣이 압박받는 와중에 단 1명이 진입하여 팔로우/DM 비즈니스 속도를 측정하는 공격조
        social_triggers: {
            executor: 'ramping-vus',
            stages: triggerStages,
            exec: 'socialTriggerTest',
            startTime: '1m',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        http_req_failed: ['rate<0.01'], // 이제 에러가 우회되므로 에러율 1% 미만 통과 타겟 설정 가능
    },
};

export function setup() {
    return login();
}

export function socialWarmupTest(data) {
    runWarmup(data.accessToken, data.csrfToken);
}

export function socialSubscriberTest(data) {
    runSubscriber(data.accessToken, data.csrfToken);
}

export function socialTriggerTest(data) {
    runTrigger(data.accessToken, data.csrfToken);
}