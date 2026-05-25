# Kafka/Redis 배포 준비도 점검 결과 (2026-05-25)

## 결론
- **Redis: 부분 적용됨** (JWT registry 및 cache 구성 존재)
- **Kafka: 설정만 존재, 실사용 미적용** (producer/consumer 비즈니스 코드 부재)
- **즉시 배포 권장 여부: 비권장**
  - 이유: Kafka가 실제 기능 경로에서 사용되지 않으며, 통합 검증(로컬/CI)도 현재 환경에서 수행 불가.

## 근거 요약
1. Redis 관련
   - Spring Redis 의존성 포함
   - RedisTemplate/CacheManager 설정 존재
   - JWT registry 구현이 Redis 기반으로 연결됨
2. Kafka 관련
   - spring-kafka 의존성과 공통 ConsumerFactory 설정은 존재
   - 하지만 `@KafkaListener`, `KafkaTemplate` 기반 송수신 코드가 코드베이스에서 확인되지 않음
3. 배포/검증
   - docker-compose에 kafka/redis 서비스 정의는 존재
   - 다만 애플리케이션 기능 관점의 Kafka E2E 시나리오 코드가 없어 운영 전 검증 포인트가 부족함

## 권장 조치
- Kafka 실제 사용 경로(이벤트 발행/소비) 구현 후 통합테스트 추가
- 운영 전 점검 항목
  - 토픽 생성/권한
  - consumer group lag 모니터링
  - 재처리/DLQ 정책
  - Redis 메모리 정책(maxmemory-policy), 영속화(AOF/RDB) 정책
- CI에서 docker compose 기반 smoke test 도입
