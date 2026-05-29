import http from 'k6/http';
import {check, sleep} from 'k6';
import {BASE_URL, getHeaders} from '../config/config.js';

/**
 * [도메인명] 시나리오 템플릿
 *
 * 이 파일을 복사하여 본인 도메인의 시나리오를 작성하세요.
 *
 * 작성 규칙:
 * 1. 함수명: [도메인]Scenario (예: reviewScenario)
 * 2. 매개변수: token, csrfToken
 * 3. tags.name: 'METHOD /api/경로' 형식 (결과에서 API별 구분)
 * 4. check(): 각 API 호출마다 성공 여부 검증
 * 5. sleep(1): API 호출 사이에 추가 (실제 사용자 패턴 시뮬레이션)
 *
 * 작성 후 tests/ 파일에 아래 두 가지를 추가합니다.
 *
 * 1. scenarios 옵션에 추가:
 *      review: { executor: 'ramping-vus', stages, exec: 'reviewTest' }
 * 2. exec 함수 추가:
 *      export function reviewTest(data) {
 *          reviewScenario(data.accessToken, data.csrfToken);
 *          sleep(1);
 *      }
 */
export function templateScenario(token, csrfToken) {
    const headers = getHeaders(token, csrfToken);

    // 1. 목록 조회
    const listRes = http.get(`${BASE_URL}/api/your-endpoint?limit=20`, {
        headers,
        tags: { name: 'GET /api/your-endpoint' },
    });

    check(listRes, {
        '목록 조회 성공': (r) => r.status === 200,
    });

    sleep(1);

    // 2. 단건 조회
    // const detailRes = http.get(`${BASE_URL}/api/your-endpoint/{id}`, {
    //   headers,
    //   tags: { name: 'GET /api/your-endpoint/{id}' },
    // });

    // 3. 생성
    // const createRes = http.post(
    //   `${BASE_URL}/api/your-endpoint`,
    //   JSON.stringify({ field: 'value' }),
    //   { headers, tags: { name: 'POST /api/your-endpoint' } }
    // );
}