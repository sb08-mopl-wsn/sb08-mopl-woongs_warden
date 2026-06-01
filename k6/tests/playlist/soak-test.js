import {sleep} from 'k6';
import {login} from '../../config/config.js';
// import {contentScenario} from '../scenarios/content.js';
import {playlistScenario} from '../../scenarios/playlist.js';

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

const stages = [
    { duration: '1m', target: 15 },
    { duration: '12m', target: 15 },
    { duration: '1m', target: 0 },
];

export const options = {
    scenarios: {
        // 🎧 플레이리스트 켜기
        playlist: {
            executor: 'ramping-vus',
            stages,
            exec: 'playlistTest', // 👈 2. 실행 함수 지정
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],

        // 💡 15 VU 정도는 서버의 정상 방어 로직 내에서 에러율 1% 미만으로
        // 완벽하게 통과(합격 마크)하는지 확인하기 위해 켜둡니다!
        http_req_failed: ['rate<0.01'],

        // 플리 주요 API 임계값
        'http_req_duration{name:GET /api/playlists}': ['p(95)<300'],
        'http_req_duration{name:POST /api/playlists}': ['p(95)<1000'],
    },
};

export function setup() {
    return login();
}

// 👈 3. 플리 실행 함수 추가
export function playlistTest(data) {
    playlistScenario(data.accessToken, data.csrfToken);
}