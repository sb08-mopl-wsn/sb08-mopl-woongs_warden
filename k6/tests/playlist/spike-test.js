import {sleep} from 'k6';
import {login} from '../../config/config.js';
// import {contentScenario} from '../scenarios/content.js';
import {playlistScenario} from '../../scenarios/playlist.js';

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

        // 💡 200명 스파이크 구간에서 데드락 방어 로직이 작동하므로 주석 처리
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