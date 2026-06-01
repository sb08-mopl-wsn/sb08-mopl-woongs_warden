import {sleep} from 'k6';
import {login} from '../../config/config.js';
// import {contentScenario} from '../scenarios/content.js';
import {playlistScenario} from '../../scenarios/playlist.js';

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
        // 💡 콘텐츠 시나리오 끄기
        // content: {
        //     executor: 'ramping-vus',
        //     stages,
        //     exec: 'contentTest',
        // },

        playlist: {
            executor: 'ramping-vus',
            stages,
            exec: 'playlistTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        http_req_failed: ['rate<0.01'], // 💡 400 에러 뜨면 주석 처리해야 할 수도 있음

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

export function playlistTest(data) {
    playlistScenario(data.accessToken, data.csrfToken);
}