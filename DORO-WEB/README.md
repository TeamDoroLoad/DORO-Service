# DORO Load Web (Frontend)

DORO Load(전기차 충전소 통합 검색 Web Service)의 프론트엔드입니다.
`DORO_Load_MVP_및_우선순위.md`의 **Priority 0** 10개 항목 중 프론트엔드 영역인
5(차량 설정·Local Storage) · 6(도로명 검색·Pin) · 7(반경 3km 조회 연동) · 8(지도·목록·상세 View) ·
9(커넥터 호환·최신 상태 표시)를 구현했습니다.

RDB/수집기/AWS 배포(Priority 0의 1·2·3·4·10번)는 이 저장소의 범위가 아니며, Spring Boot 백엔드 쪽 작업입니다.

---

## 1. 참고한 문서

| 문서 | 이 프로젝트에 반영한 내용 |
|---|---|
| `도로교통공사_2차_프로젝트_설명_0720.md` | 전체 서비스 흐름(4-1 사용자 이용 흐름, 4-2 주요 화면 구성) — 차량 설정 → 주소 검색 → Pin 확정 → 3km 조회 → 상세 조회 순서를 그대로 구현 |
| `DORO_Load_MVP_및_우선순위.md` | 구현 범위(Priority 0)와 완료 조건(Definition of Done) 기준 |
| `DORO_Load_AWS_Endpoint_API_명세서.md` | REST API Contract(§4~7) — Endpoint 경로, Query Parameter, Response 필드명(camelCase)을 `src/types.ts`·`src/api/client.ts`에 그대로 반영 |
| `DORO-Load-Charge-ERD.png`, `DORO-Load-Vehicle-ERD.png` | `STATION`/`CHARGER`/`CONNECTOR_TYPE`/`VEHICLE_TRIM` 등 테이블 구조 — 프론트 타입과 mock 데이터의 필드 구성 근거 |
| `DORO Load.dc.html` (와이어프레임) | 레이아웃(헤더+검색바, 좌측 지도/목록, 우측 차량 설정 사이드바)과 배지·아코디언 스타일의 시각적 기준 |
| `ev-charging-station.integrated.schema.json` | 커넥터·상태 코드 표준화 방향(예: `UNKNOWN` 처리) 참고 |

---

## 2. 기술 스펙

- **Vue 3** (`<script setup>` Composition API) + **TypeScript** + **Vite**
- 상태 관리 라이브러리(Pinia 등) 없이 모듈 스코프 `ref` 싱글턴 composable로 처리 (`useVehicleSettings`, `useStationSearch`) — 화면이 단일 페이지라 별도 store가 필요하지 않다고 판단
- 라우터 없음 — 와이어프레임처럼 지도/목록/상세가 한 화면에서 아코디언·확장 카드로 전환
- 외부 UI 라이브러리 없음 — 컴포넌트별 `<style scoped>` 인라인 CSS

## 3. API 연동

`src/api/client.ts`가 `VITE_USE_MOCK` 환경변수로 두 모드를 전환합니다.

| 모드 | 동작 |
|---|---|
| `VITE_USE_MOCK=true` (기본값) | `src/api/mockData.ts`의 목데이터로 응답 (지연시간 흉내만 냄) |
| `VITE_USE_MOCK=false` | `VITE_API_BASE_URL` + Endpoint 경로로 실제 `fetch` 호출, 응답의 `data` 필드를 그대로 반환 |

사용 중인 Endpoint (모두 API 명세서 §4.1 기준):

| Method | Path | 용도 |
|---|---|---|
| `GET` | `/vehicle-trims` | 차량 Trim 목록 (브랜드·모델·트림·지원 커넥터) |
| `GET` | `/connector-types` | 커넥터 Master (현재 화면에서 직접 노출하진 않고 타입 정의용) |
| `GET` | `/charging-networks` | 충전 플랫폼·사업자 선택 목록 |
| `GET` | `/locations/geocode?query=` | 도로명 주소 → 좌표 후보 |
| `GET` | `/stations/nearby?latitude=&longitude=&radiusMeters=&vehicleTrimId=&connectorCodes=` | 반경 3km 충전소 검색 |
| `GET` | `/stations/{stationId}` | 충전소 상세(충전기별 커넥터·최신 상태) — 목록에서 카드를 펼칠 때 조회 |

응답 스키마가 명세서와 동일하게 유지되는 한, 백엔드를 실제로 붙여도 컴포넌트 코드는 수정할 필요가 없습니다.

## 4. 구현된 기능

- **차량 설정 (P0-5)**: 브랜드→모델→트림 순으로 선택, 트림에 종속된 지원 커넥터를 자동 표시(읽기 전용). 충전 플랫폼·회원 여부·현재/목표 SOC는 선택 입력. 저장 시 `localStorage`에 보관하고 새로고침해도 복원됨. **계정 ID·비밀번호·인증 Token은 저장하지 않음**(요구사항 그대로 반영).
- **도로명 검색 + 지도 클릭 + 차량정보 저장 — 검색 트리거 3가지 (P0-6)**: 페이지를 열거나 브라우저를 다시 켜면(이전에 차량정보를 저장해뒀어도) **어떤 경우에도 자동으로 검색이 실행되지 않고 지도에 확정된 Pin도 없는 완전 대기 상태**이며, 오른쪽 차량 설정 폼만 이전 값으로 복원됨. 이후 다음 셋 중 하나를 실행해야 검색이 확정됨: ① 상단 주소 검색에서 후보를 선택(즉시 그 좌표로 확정, 추가 지도 클릭 불필요) ② 지도 빈 영역 직접 클릭 ③ 차량정보 저장 버튼(이 세션에서 위치를 한 번도 확정한 적 없을 때만 기본 좌표로 첫 검색 실행).
- **반경 3km 조회 연동 (P0-7)**: 확정 좌표 + 차량 커넥터로 `/stations/nearby` 호출. 차량 설정을 바꾸면 같은 위치 기준으로 자동 재조회.
- **지도·목록·상세 View (P0-8)**: 지도는 TMAP 미연동 상태의 자리표시(placeholder)이며, 실제 위경도 ↔ 화면 좌표 변환·3km 원·클릭 좌표 계산 로직은 실좌표 기준으로 동작. 목록은 거리순/추천순 정렬, 카드를 펼치면 상세(운영시간·사업자·충전기별 상태) 표시. **지도의 충전소 마커를 클릭하면 새 검색 없이 그 충전소가 목록 최상단으로 이동하며 상세가 자동으로 펼쳐짐**.
- **호환·최신 상태 표시 (P0-9)**: 차량 커넥터와 충전기 커넥터 교집합으로 호환 배지 표시, 상태별 배지(이용 가능/충전 중/점검 중 등), 데이터 출처와 마지막 수집 시각(상대 시간) 표시.

## 5. 폴더 구조와 파일 역할

```
doro-load-web/
├─ index.html            Vite entry HTML
├─ .env.example          VITE_API_BASE_URL, VITE_USE_MOCK 예시
├─ src/
│  ├─ main.ts            Vue 앱 부트스트랩
│  ├─ App.vue            레이아웃 조립 + 검색 트리거 연결(주소 선택/지도 클릭/차량정보 저장 → confirmAt, 마커 선택 → selectStation)
│  ├─ style.css           전역 CSS 변수(색상 팔레트)와 공통 배지 스타일
│  ├─ env.d.ts            import.meta.env 타입 선언
│  ├─ types.ts            API 명세서 §5~7 기준 camelCase 타입 (VehicleTrim, NearbyStation 등)
│  ├─ api/
│  │  ├─ client.ts        mock/실제 API 전환 fetch 클라이언트 (Endpoint별 함수)
│  │  └─ mockData.ts      목 차량/커넥터/충전망 데이터 + 좌표 기반 목 충전소 생성 로직
│  ├─ composables/
│  │  ├─ useVehicleSettings.ts  차량·회원·SOC 설정의 localStorage 저장/복원 (모듈 싱글턴 ref)
│  │  └─ useStationSearch.ts    지도 중심·확정 Pin·검색 결과·정렬·선택된 충전소 상태 관리. `confirmAt()` 호출 시에만 3km 조회 실행(자동 실행 없음)
│  ├─ components/
│  │  ├─ AddressSearchBar.vue     헤더의 도로명 주소 검색창 + 후보 목록 드롭다운(선택 즉시 검색 확정)
│  │  ├─ StationMap.vue           지도 자리표시, 반경 원, 충전소 마커(클릭 시 목록과 연동), 빈 영역 클릭→좌표 변환
│  │  ├─ StationList.vue          정렬 툴바 + 충전소 카드 목록(마커 선택 시 자동 최상단 이동·펼침) + 펼침 상세(충전기별 상태)
│  │  └─ VehicleSettingsPanel.vue 우측 사이드바: 차량/플랫폼/배터리 입력 폼, 저장 시 `saved` 이벤트로 최초 검색 트리거
│  └─ utils/
│     ├─ geo.ts            위경도 ↔ 미터 오프셋 변환, 거리/상대시간 포맷터
│     ├─ seededRandom.ts   mock 데이터용 결정적 의사난수 (같은 좌표 → 같은 배치)
│     └─ statusMeta.ts     충전기 상태(ChargerStatus)별 라벨·배지 색상 매핑
```

## 6. 데이터 저장 방식

사용자 설정(차량·회원 여부·SOC)은 **쿠키가 아닌 `localStorage`**에 저장합니다. 서버로 자동 전송될 필요가 없는 값이라 쿠키의 자동 전송·만료·용량 제한(≈4KB)이 오히려 불필요하며, 문서(`도로교통공사_2차_프로젝트_설명_0720.md` 4-1-0, API 명세서 §1)에도 Local Storage로 명시되어 있습니다. 계정 ID·비밀번호·인증 Token은 어떤 방식으로도 저장하지 않습니다.

---

## 7. 차후 대체되어야 할 값 / 파일

| 항목 | 위치 | 대체 방법 |
|---|---|---|
| Mock ↔ 실제 API 전환 | `.env` (`.env.example` 복사해서 사용) | `VITE_USE_MOCK=false`, `VITE_API_BASE_URL=https://{실제 도메인}/api/v1` 로 변경 |
| 목데이터 전체 | `src/api/mockData.ts` | 백엔드 연동 후에는 호출되지 않음. 개발용 fallback으로 남겨두거나, 필요 없으면 파일과 `client.ts`의 `USE_MOCK` 분기 삭제 |
| TMAP 지도 렌더링 | `src/components/StationMap.vue` | TMAP JS SDK appKey 발급 후 `.map-bg`(줄무늬 배경)와 마커 `<div>` 렌더링 부분을 TMAP 지도/마커 API 호출로 교체. **좌표 변환 로직(`src/utils/geo.ts`, `toPercent`/클릭 핸들러)은 그대로 유지 가능** — TMAP이 직접 픽셀 변환을 제공하면 이 부분도 대체 |
| "지도 영역 자리표시 · TMAP SDK 연동 예정" 배지 | `src/components/StationMap.vue` (`.sdk-badge`) | TMAP 연동 완료 시 제거 |
| 기본 지도 중심 좌표 | `src/composables/useStationSearch.ts`의 `DEFAULT_CENTER` (서울시청 하드코딩) | 초기 지도 중심뿐 아니라 "차량정보 최초 저장 시 위치가 없으면 이 좌표로 검색"의 fallback으로도 쓰임. 실제 서비스 정책에 맞게 변경하거나, Browser Geolocation API(선택 사항, 기술 스택 문서 6번) 연동 여부 결정 |
| 차량 Trim/커넥터/충전망 마스터 데이터 | `src/api/mockData.ts`의 `MOCK_VEHICLE_TRIMS`/`MOCK_CONNECTOR_TYPES`/`MOCK_CHARGING_NETWORKS` | 실제 DB(`VEHICLE_TRIM`, `CONNECTOR_TYPE`, `CHARGING_NETWORK`) 값과 `connector_code` 체계가 다르면 호환성 계산이 틀어지므로, 실제 API 연동 시 코드 값 일치 여부 확인 필요 |
| CORS 허용 Origin | 백엔드 Spring Boot 설정 (이 저장소 범위 아님) | 배포된 프론트 Origin을 백엔드 CORS 허용 목록에 추가해야 실제 API 호출이 성공함 |

## 8. 알려진 제한사항 (P0 범위 밖)

- 추천 API(`POST /recommendations`), 경로 예상 API(`POST /routes/estimate`)는 Priority 1이라 미구현
- 실제 사용자 현재 위치(Browser Geolocation API)는 사용하지 않음 — 항상 주소 검색/지도 클릭으로 위치 지정
- 반경 값(3000m)은 하드코딩되어 있으며 사용자가 조정하는 UI는 없음(문서상 "최대 허용 반경은 Backend에서 제한"이라 프론트에서 노출하지 않음)

## 9. 미구현 기능 (Priority 1 · 2)

`DORO_Load_MVP_및_우선순위.md` §4 기준, 이번 P0 구현에 포함되지 않은 기능을 우선순위 순으로 정리합니다. "프론트 영향" 열은 이 저장소(`doro-load-web`) 기준으로 구현 시 화면/코드 변경이 필요한지, 백엔드·인프라만으로 완결되는지 구분한 것입니다.

### Priority 1 — 핵심 완성도를 높이는 기능

| 순위 | 기능 | MVP 문서상 적용 조건 | 프론트 영향 |
|---:|---|---|---|
| 1 | 규칙 기반 추천 정렬 | 호환성·가용 상태·거리·데이터 최신성을 종합 지표로 정의 | `StationList.vue`의 `recommendScore()`(현재는 임시 로직)를 백엔드 `/recommendations` 응답 기준으로 교체 필요 |
| 2 | 충전 사업자 회원 여부 연동 | `CHARGING_NETWORK` Mapping과 회원정보가 있으면 추천에 가점 반영 | `VehicleSettingsPanel.vue`에서 이미 `memberNetworkId`·`isMember`를 저장 중이나, 현재 `/stations/nearby` 요청에는 포함해 보내지 않음 — 추천 API 연동 시 함께 전송 필요 |
| 3 | 실제 경로 거리·예상 이동시간 | TMAP 경로 API가 준비되면 직선거리와 별도로 표시 | `POST /routes/estimate` 연동 및 `StationList.vue`에 "예상 이동시간" 항목 추가 필요 (현재는 `straightDistanceMeters` 직선거리만 표시) |
| 4 | Redis 검색 Cache | 반복되는 주변 검색과 최신 상태 조회의 응답시간 개선 | 백엔드·인프라 전용 — 프론트 코드 변경 없음 |
| 5 | 수집·수집 시각 표시 | 모든 검색·상세 응답에서 `sourceName`, `sourceUpdatedAt`, `collectedAt` 제공 | **부분 반영됨**: `source.sourceName`/`source.collectedAt`은 이미 표시 중. `sourceUpdatedAt`(응답 필드 `latestStatusUpdatedAt`)은 아직 화면에 노출하지 않음(§7 예약 필드) |
| 6 | 수집 실패·재시도 처리 | API 실패, XML Parsing 오류, 중복 이벤트를 기록하고 재실행 가능 | 백엔드·인프라 전용 — 프론트는 현재도 API 실패 시 `error` 상태로 사용자에게 안내만 하면 되므로 추가 변경 없음 |

### Priority 2 — 일정 여유가 있을 때 적용

| 순위 | 기능 | MVP 문서상 적용 조건 | 프론트 영향 |
|---:|---|---|---|
| 1 | 예상 충전시간 | 차량 배터리 용량·목표 SOC·차량/충전기 최대출력으로 예상 범위 계산 | `VehicleSettingsPanel.vue`가 이미 `currentSoc`/`targetSoc`을 저장 중이나 계산·표시 UI는 없음. `VehicleTrim`의 `batteryKwh`/`maxAcKw`/`maxDcKw`(현재 미사용 예약 필드, §7)를 활용해 `StationList.vue` 상세 패널에 범위 표시 예정 |
| 2 | 예상 충전비용 | `TARIFF` 데이터가 확보된 충전망에 한해 계산 | 위와 동일 위치에 "예상 비용" 항목 추가 필요 (요금 데이터가 없는 충전망은 표시하지 않아야 함) |
| 3 | 배터리 도달 가능성 | 현재 SOC와 차량 주행거리, 경로 거리를 결합한 규칙 판단 | 목록/상세에 "도달 가능" 배지 추가, Priority 1의 경로 API 연동이 선행되어야 함 |
| 4 | Kafka 수집 Pipeline | Raw Topic, 정규화 Consumer와 실패 Topic 구현 | 백엔드·인프라 전용 — 프론트 코드 변경 없음 |
| 5 | S3 원본 보존 | 원본 XML과 오류 Payload 저장 | 백엔드·인프라 전용 — 프론트 코드 변경 없음 |
| 6 | 다중 AZ Application | App Instance 추가 및 ALB Target Group 다중화 | 백엔드·인프라 전용 — 프론트 코드 변경 없음 |
| 7 | 관측성 강화 | CloudWatch Dashboard, 지표·Alarm과 분산 Trace ID 적용 | 백엔드·인프라 전용. 다만 프론트가 보내는 `X-Request-Id`가 있다면 Trace 연결에 활용될 수 있음(현재 `client.ts`는 이 Header를 보내지 않음 — 필요 시 추가) |
