# 티켓사자
  
<img src="https://github.com/user-attachments/assets/4af40ad0-4081-412f-9678-7b111e56bca1" width="200"/>
  
https://ticketlion.site

좌석 선택 및 예매가 가능한 티켓 예매 서비스입니다.  
링크를 통해 예매 과정을 체험해보실 수 있습니다.

## 기술 스택

- **Backend**: Spring Boot 4.0.2, Java 17
- **Database**: H2 (dev) / PostgreSQL - Supabase (prod)
- **Cache**: Redis (좌석 선점, 동시성 제어)
- **Template Engine**: Thymeleaf
- **Security**: Spring Security
- **Build**: Gradle

## 주요 기능

- 회원가입 / 로그인
- 이벤트 목록 조회 및 좌석 선택
- Redis Lua 스크립트를 통한 원자적 좌석 선점 (TTL 10분)
- 비관적 락(Pessimistic Lock) 기반 결제 처리
- 좌석 최종 배정 및 예매 확정
- 만료된 선점 자동 정리 (스케줄러, 1분 주기)
- 예매 취소

## 예약 흐름

```
1. 좌석 선점  →  POST /events/{eventId}/holds
      ↓ Redis Lua 스크립트로 원자적 선점 (TTL 10분)

2. 결제       →  POST /reservations/payments
      ↓ 비관적 락으로 좌석 조회 → 모의 결제(0원) → Reservation PENDING 생성

3. 좌석 확정  →  POST /reservations/{reservationId}/assign
      ↓ Reservation CONFIRMED → Redis 홀드 키 삭제
```

1인 최대 2매 제한이 적용됩니다.

## 로컬 실행 방법

### 요구사항

- Java 17 (Temurin)
- Redis (`localhost:6379`)

### Redis 실행

```bash
brew services start redis
```

### 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행 (dev 프로파일: H2 + Redis)
./gradlew bootRun
```

서버 기동 후 `http://localhost:8080` 접속

**H2 콘솔**: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./h2db/testdb`
- User: `sa` / Password: (없음)

## API 엔드포인트

| 메서드 | 경로 | 설명                    |
|--------|------|-----------------------|
| POST | `/events/{eventId}/holds` | 좌석 선점                 |
| GET | `/api/events/{eventId}/seats` | 좌석 상태 조회 (Redis 홀드 반영) |
| POST | `/reservations/payments` | 결제 + 예약 생성            |
| POST | `/reservations/{id}/assign` | 좌석 최종 배정              |
| GET | `/reservations/{id}/results` | 예매 결과 페이지             |
| GET | `/api/reservations/my` | 예매 내역                 |
| PATCH | `/api/reservations/{id}/cancel` | 예매 취소                 |

## 아키텍처

레이어드 MVC: `Controller → Service → Repository → Domain`

### 엔티티 관계

```
Event (1) ─→ (N) Seat
Member (1) ─→ (N) Reservation ─→ (1) Payment
Reservation ─→ (1) Seat
```

### 동시성 제어

- **선점 단계**: Redis Lua 스크립트로 다수 좌석을 원자적으로 처리
  - 키 형식: `seat:hold:{eventId}:{seatNo}`, 값: memberId, TTL 10분
  - 2단계 Lua: 전체 키 선점 가능 여부 확인 → 전체 `SETEX` 설정
- **결제 단계**: JPA 비관적 쓰기 락(`PESSIMISTIC_WRITE`)으로 동시 결제 방지

### 정리 스케줄러

매 분마다 실행되어 10분 이상 경과한 `PENDING` 예약을 자동 취소하고 좌석을 `AVAILABLE`로 복구합니다.

## 프로파일

| 프로파일 | DB | 비고 |
|----------|----|------|
| `dev` (기본) | H2 파일 (`./h2db/testdb`) | SQL 로깅 활성화, H2 콘솔 사용 가능 |
| `prod` | PostgreSQL (Supabase) | SQL 로깅 비활성화, `ddl-auto: validate` |

## 테스트

```bash
./gradlew test

# 단일 클래스
./gradlew test --tests ClassName
```

## CI/CD

GitHub Actions (`.github/workflows/deploy.yml`) — `main` 브랜치 푸시 시 자동 배포

1. `./gradlew test`
2. `./gradlew build -x test`
3. SCP로 JAR 전송 → SSH로 `prod` 프로파일 재시작

필요한 GitHub Secrets: `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`
