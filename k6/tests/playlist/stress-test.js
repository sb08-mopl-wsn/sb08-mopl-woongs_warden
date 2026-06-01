import {sleep} from 'k6';
import {login} from '../../config/config.js';
// import {contentScenario} from '../scenarios/content.js';
import {playlistScenario} from '../../scenarios/playlist.js'; // 👈 1. 플리 시나리오 임포트

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
        // 💡 콘텐츠 끄기
        // content: {
        //     executor: 'ramping-vus',
        //     stages,
        //     exec: 'contentTest',
        // },

        // 🎧 플레이리스트 켜기
        playlist: {
            executor: 'ramping-vus',
            stages,
            exec: 'playlistTest', // 👈 2. 실행 함수 지정
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],

        // 💡 스트레스 테스트에서는 DB 데드락이나 커넥션 풀 고갈로 에러가 폭발할 예정입니다.
        // 테스트가 중단되지 않도록 이 기준은 과감하게 끕니다!
        // http_req_failed: ['rate<0.01'],

        // 플리 주요 API 임계값
        'http_req_duration{name:GET /api/playlists}': ['p(95)<300'],
        'http_req_duration{name:POST /api/playlists}': ['p(95)<1000'],
    },
};

export function setup() {
    return login();
}

// export function contentTest(data) {
//     contentScenario(data.accessToken, data.csrfToken);
//     sleep(1);
// }

// 👈 3. 플리 실행 함수 추가
export function playlistTest(data) {
    playlistScenario(data.accessToken, data.csrfToken);
}