# MOLE 부하테스트 (k6)
AWS 배포 서버를 대상으로 Load/Stress/Spike 테스트 수행.

## 1. k6 설치
```bash
# mac
brew install k6

# windows
choco install k6

# 설치 확인
k6 version
```

## 2. 프로젝트 구조
```text
k6/
├── config/
│   └── config.js               # 환경변수, 공통 헤더, 인증(로그인/토큰 재발급)
├── scenarios/
│   ├── _template.js            # 시나리오 작성 템플릿
│   └── content.js              # 콘텐츠 도메인
├── tests/
│   ├── load-test.js            # Load Test (정상 트래픽)
│   ├── stress-test.js          # Stress Test (한계 탐색)
│   ├── spike-test.js           # Spike Test (급격한 트래픽)
│   └── soak-test.js            # Soak Test (장시간 안정성)
└── README.md
```

## 3. 실행 방법
1. **TEST_EMAIL, TEST_PASSWORD 필수**  
2. **비밀번호에 특수문자가 포함되어 있으면 (' ')로 감싸야 함**
```bash
# Load Test
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=k6/results/load-test-report.html k6 run -e BASE_URL=URL -e TEST_EMAIL=your@email.com -e TEST_PASSWORD='yourpassword' k6/tests/load-test.js

# Stress Test
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=k6/results/stress-test-report.html k6 run -e BASE_URL=URL -e TEST_EMAIL=your@email.com -e TEST_PASSWORD='yourpassword' k6/tests/stress-test.js

# Spike Test
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=k6/results/spike-test-report.html k6 run -e BASE_URL=URL -e TEST_EMAIL=your@email.com -e TEST_PASSWORD='yourpassword' k6/tests/spike-test.js

# Soak Test
K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_EXPORT=k6/results/soak-test-report.html k6 run -e BASE_URL=URL -e TEST_EMAIL=your@email.com -e TEST_PASSWORD='yourpassword' k6/tests/soak-test.js
```

## 4. 시나리오 추가 가이드
### step1 - 시나리오 파일 생성
```bash
cp k6/scenarios/_template.js k6/scenarios/review.js
```

### step2 - 시나리오 함수 작성
```javascript
// 기존 시나리오 함수 참고
```

### step3 - 테스트 파일에 등록
```javascript
import { reviewScenario } from '../scenarios/review.js';

// 1. scenarios 옵션에 추가
export const options = {
    scenarios: {
        content: { executor: 'ramping-vus', stages, exec: 'contentTest' },
        review: { executor: 'ramping-vus', stages, exec: 'reviewTest' },  // ← 추가
    },
};

// 2. exec 함수 추가
export function reviewTest(data) {
    reviewScenario(data.accessToken, data.csrfToken);
    sleep(1);
}
```

## 5. 임계값 기준
| 지표                      | 기준       | 설명                  |
|-------------------------|----------|---------------------|
| http_req_duration p(95) | < 500ms  | 95% 요청이 500ms 이내 응답 |
| http_req_duration p(99) | < 1500ms | 99% 요청이 1.5초 이내 응답  |
| http_req_failed         | < 1%     | 에러율 1% 미만           |