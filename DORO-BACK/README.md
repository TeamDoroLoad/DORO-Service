# DORO Load Backend (Local)

DORO Load 서비스의 Spring Boot Backend. 명세서 `DORO_Load_Backend_API_Specification_v1.0.md`,
`DORO_Load_Backend_Application_Implementation_Guide_v1.0.md`를 단일 계약·구현 기준으로 구현했다.

## 1. 기술 스택

- Java 21, Spring Boot 3.5.4 (Gradle Kotlin DSL)
- MySQL 8.0 (Spatial, Source of Truth) + Redis (TMAP Route Cache·Rate Limit·짧은 Lock 전용 보조 저장소)
- TMAP REST API (주소·POI 검색, 자동차 경로) 연동
- Resilience4j (Circuit Breaker·Retry·Bulkhead), springdoc-openapi

## 2. 로컬 실행

### 2.1 사전 준비

- Java 21
- Docker Desktop (MySQL·Redis 컨테이너 실행용)

### 2.2 MySQL·Redis 기동

```bash
docker compose up -d
```

- MySQL: 컨테이너 최초 기동 시 `docs/db/schema.sql` → `seed-reference.sql` 순서로 자동 적용된다 (14개 Table + `v_charger_latest_status` View + 샘플 데이터).
- Host 포트는 `3306`(MySQL), `6379`(Redis)로 매핑한다. 로컬에 이미 다른 MySQL이 3306을 사용 중이라면 실행 전 종료하거나, `docker-compose.yml`과 `application-local.yml`의 포트를 함께 다른 값(예: 3307)으로 바꾼다.
- 다시 초기화하려면 `docker compose down -v` 후 `docker compose up -d`.

### 2.3 TMAP App Key (선택)

`doro.tmap.app-key`는 환경변수 `TMAP_APP_KEY`로 주입한다. 키가 없어도 애플리케이션은 정상 기동하며,
위치 검색·경로 조회만 TMAP 실패 응답 규칙(502/503, `route.status=UNAVAILABLE`)에 따라 동작한다.

```bash
export TMAP_APP_KEY=발급받은_TMAP_REST_App_Key
```

### 2.4 애플리케이션 기동

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

기동 후 확인:

```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
curl "http://localhost:8080/api/v1/vehicle-trims?size=5"
```

Swagger UI(로컬 전용): http://localhost:8080/swagger-ui.html

## 3. 공개 API (Base Path `/api/v1`)

| Method | Path | 설명 |
|---|---|---|
| GET | /vehicle-trims | 차량 브랜드·모델·트림·지원 커넥터 검색 |
| GET | /charging-networks | 충전 네트워크·사업자 조회 |
| GET | /locations/search | TMAP 주소·POI 통합 검색 Proxy |
| POST | /stations/recommendations | 반경 3km 후보 검색·평가·상위 3개 추천 |
| GET | /stations/{stationId} | 충전소·충전기 상세 조회 |

모든 성공 응답은 `{ data, meta }`, 오류 응답은 `{ timestamp, status, code, message, path, requestId, details }` 형식이다.

## 4. 빌드·테스트

```bash
./gradlew build            # Compile + Test + Packaging
./gradlew bootJar           # 배포용 Jar 생성 (build/libs)
```

### Windows + 한글 경로 환경에서 `./gradlew test` 관련 안내

이 저장소 경로(`...\2차 프로젝트\...`)처럼 **한글이 포함된 절대 경로**에서 Windows 로컬 Region 설정이
"세계 언어 지원을 위한 유니코드 UTF-8 사용"(Beta)으로 켜져 있지 않으면, JDK가 Test Worker 프로세스의
Classpath를 원활히 해석하지 못해 `./gradlew test`가 `ClassNotFoundException`으로 실패할 수 있다
(Windows `sun.jnu.encoding`이 시스템 Locale에 종속되는 JDK/Gradle의 알려진 제약이며 애플리케이션 코드 결함이 아니다).
`compileJava`, `compileTestJava`, `bootJar`, 실제 애플리케이션 기동·API 호출은 모두 정상 동작함을 확인했다.

테스트를 직접 실행해야 한다면 다음 중 하나를 사용한다.

1. IntelliJ IDEA·Eclipse 등 IDE의 자체 Test Runner로 실행 (이 문제의 영향을 받지 않는 경우가 대부분이다).
2. Windows 설정 → 시간 및 언어 → 언어 및 지역 → 관리자 언어 설정에서
   "세계 언어 지원을 위한 유니코드 UTF-8 사용"을 켜고 재부팅 후 실행.
3. WSL2 또는 Linux 기반 CI 환경에서 실행.

## 5. 디렉터리 구조

```text
src/main/java/com/doroload/api/
├── common/          공통 응답·오류·RequestId·좌표·최신성·Redis Rate Limiter
├── pricing/          공통 예상 단가 Policy (350원/kWh)
├── vehicle/          차량 브랜드·모델·트림·커넥터
├── network/          충전 사업자·네트워크
├── location/         TMAP 주소·POI 검색
├── route/            TMAP 경로 Client + Redis Route Cache·Lock
├── recommendation/   추천 Orchestrator + Score Policy
└── station/          충전소·충전기·상태·원천 매칭

docs/db/
├── schema.sql          14개 Table + View 승인 DDL (Application은 자동 실행하지 않음)
├── seed-reference.sql  로컬 개발용 샘플 데이터
├── verification.sql    수동 스모크 테스트 SQL
└── CHANGELOG.md        Schema 변경 이력
```

## 6. 설계상 의도적 제약 (명세서 근거)

- MySQL이 유일한 비즈니스 데이터 원본이며 Redis는 TMAP Route Cache·Rate Limit·짧은 Lock에만 사용한다.
- `tariff` Table은 Schema에 유지하되 공개 API는 조회하지 않는다. 모든 충전소·충전 네트워크는 공통 예상 단가(350원/kWh)를 사용한다.
- `radiusMeters`(3000)·결과 개수(최대 3개)는 서버 정책으로 고정되며 Client가 재정의할 수 없다.
- TMAP 경로 조회 일부 실패는 전체 요청 실패로 처리하지 않고 `route.status=UNAVAILABLE` + `meta.partial=true`로 표현한다.
