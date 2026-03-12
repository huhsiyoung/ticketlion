# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

티켓사자(TicketLion)는 Redis Lua 스크립트를 활용한 동시성 좌석 예약 처리 기능을 갖춘 Spring Boot 4.0.2 티켓팅 플랫폼입니다. 기본 활성 프로파일은 `dev` (H2 + Redis)입니다.

## 빌드 및 실행 명령어

```bash
# 빌드
./gradlew build
./gradlew build -x test      # 테스트 제외

# 실행 (기본: dev 프로파일)
./gradlew bootRun

# 프로덕션 프로파일로 실행
java -jar build/libs/ticketlion-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 테스트
./gradlew test
./gradlew test --tests ClassName   # 단일 테스트 클래스
```

## 로컬 개발 환경 요구사항

- **Java 17** (Temurin)
- **Redis** `localhost:6379`, 비밀번호 `systempass` — dev 환경에서도 필수
  ```bash
  brew services start redis
  ```
- **H2 콘솔**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./h2db/testdb`, user: `sa`, password: (없음)

## 아키텍처

레이어드 MVC: `web → service → repository → domain`

**패키지 구조** (`src/main/java/site/ticketlion/`):
- `config/` — `RedisConfig` (Lua 스크립트 빈 + `RedisTemplate`/`StringRedisTemplate` 설정), `SecurityConfig` (prod), `SecurityConfigDev` (dev, H2 콘솔 활성화, API CSRF 비활성화)
- `domain/` — JPA 엔티티: `Event`, `Seat`, `Reservation`, `Payment`, `Member` + 열거형
- `repository/` — Spring Data JPA 리포지토리 + `PaymentGateway` 인터페이스 / `MockPaymentGateway` (항상 성공, 0₩)
- `service/` — 비즈니스 로직; `SeatHoldService` (단일 좌석 홀드용 유틸), `ReservationService` (예약 오케스트레이션), `ReservationCleanupScheduler` (1분마다 PENDING → CANCELLED 정리)
- `web/controller/api/` — REST 엔드포인트 (`ReservationController`)
- `web/controller/page/` — Thymeleaf 페이지 컨트롤러 (`PageController`, `AuthController`)

**엔티티 관계:**
```
Event (1) → (many) Seat
Member (1) → (many) Reservation → (1) Payment
Reservation → (1) Seat
```

## 예약 흐름 (3단계)

`ReservationController`와 `ReservationService`가 전체 흐름을 담당합니다.

1. **좌석 홀드** — `POST /events/{eventId}/holds`
   - `ReservationService.holdSeats()` 호출
   - 1인 2매 제한 검사 (기존 홀드 포함)
   - DB에서 RESERVED 상태 검사 후 Redis Lua 스크립트로 원자적 홀드 (TTL 10분)
   - Redis 키 형식: `seat:hold:{eventId}:{seatNo}`

2. **결제** — `POST /reservations/payments`
   - `reserveSeatsWithPessimisticLock()`: 비관적 락으로 좌석 조회 → AVAILABLE→RESERVED 변경 → Reservation 생성 (PENDING)
   - `PaymentService.pay()`: 0원 모의 결제 처리, Payment 이력 저장, Reservation → PAID
   - Redis 홀드 키는 이 단계에서 삭제하지 않음

3. **좌석 확정** — `POST /reservations/{reservationId}/assign`
   - `cleanupHeldSeats()`: 동일 사용자+이벤트의 PENDING 예약 전체 CONFIRMED로 변경 + Redis 홀드 키 삭제

**정리 스케줄러**: 1분마다 실행 — 10분 이상 경과한 PENDING 예약 → 좌석 AVAILABLE 복구 → 예약 CANCELLED

## API 엔드포인트 목록

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/events/{eventId}/holds` | 좌석 선점 |
| GET | `/api/events/{eventId}/seats` | 좌석 상태 조회 (Redis 홀드 반영) |
| POST | `/reservations/payments` | 결제 + Reservation 생성 |
| POST | `/reservations/{id}/assign` | 좌석 최종 배정 (Redis 키 삭제) |
| GET | `/reservations/{id}/results` | 예매 결과 페이지 |
| GET | `/api/reservations/my` | 내 예매 내역 |
| PATCH | `/api/reservations/{id}/cancel` | 예매 취소 |

## Redis 핵심 구현

- `RedisConfig.holdSeatsScript()`: 다중 좌석 원자적 선점을 위한 Lua 스크립트 빈
  - 2단계: 전체 키 선점 가능 여부 확인 → 전체 `SETEX` 설정
  - 반환: 1(성공) / 0(실패 — 다른 사용자가 선점 중)
- `StringRedisTemplate`: 홀드 키 문자열 조회/삭제용
- `RedisTemplate<String, Object>`: JSON 직렬화 (`GenericJacksonJsonRedisSerializer`)

## 프로파일

| 프로파일 | DB | 비고 |
|---|---|---|
| `dev` | H2 파일 (`./h2db/testdb`) | `ddl-auto: update`, SQL 로깅 활성화, H2 콘솔 사용 가능 |
| `prod` | PostgreSQL (Supabase) | `ddl-auto: validate`, SQL 로깅 비활성화 |

## CI/CD

GitHub Actions (`.github/workflows/deploy.yml`) — `main` 브랜치 푸시 시 실행:
1. `./gradlew test`
2. `./gradlew build -x test`
3. SCP로 JAR 전송 → SSH로 `prod` 프로파일 재시작

필요한 GitHub Secrets: `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`