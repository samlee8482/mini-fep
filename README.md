# Mini FEP Framework

Java 21과 Spring Boot 기반으로 구현한 **금융 대외기관 연계(FEP, Front-End Processor) 학습용 PoC**입니다. 업무 애플리케이션과 대외 통신 모듈을 분리하고, 기관별 전문 변환·TCP 통신·연결 풀·오류 처리·거래 추적·동시 거래 테스트를 구현했습니다.

> 본 저장소는 학습·기술 검증용 프로젝트이며 실제 금융기관 또는 KB증권의 내부 시스템 구현물이 아닙니다.

## 1. 아키텍처

```text
[HTTP Client / Python Test]
           |
           v
[Business App :8083]  업무 API, 거래 traceId 발급
           | HTTP POST + X-Trace-Id
           v
[FEP Server :8082]    REST 진입점, 표준 오류 응답
           |
           v
[fep-core]           기관 설정(YAML), 공통 Mapper, TCP Connection Pool
           |                            |
           +---------- TCP ------------+-------------+
           |                                          |
           v                                          v
[Mock Institution A :9001]               [Mock Institution B :9002]
```

업무 앱은 FEP 서버가 중지되어 있어도 독립적으로 기동할 수 있으며, **업무 요청이 들어올 때에만** FEP를 호출합니다. 기관 접속 주소·타임아웃·풀 크기·전문 규격은 `config/fep.yml`에서 관리합니다.

## 2. 기술 스택 및 구성

- Java 21, Spring Boot 3.5.7, Maven 멀티 모듈
- HTTP REST(JSON), TCP 소켓, 4자리 ASCII 길이 헤더 기반 전문
- Python 3 표준 라이브러리 기반 통합·부하 테스트(별도 패키지 설치 불필요)

| 경로 | 역할 |
| --- | --- |
| `business-app/` | 업무 요청 수신, FEP HTTP 호출, `traceId` 생성·전달 |
| `fep-server/` | FEP REST API, 공통 서비스 호출, 오류 응답 매핑 |
| `fep-core/` | YAML 설정 로딩, 기관별 라우팅, Mapper, TCP 통신 및 Connection Pool |
| `mock-institution/` | 가상기관 A/B TCP 응답 서버 및 장애 모드 |
| `config/fep.yml` | 기관별 주소, 연결/읽기 타임아웃, 풀 설정, Mapper 설정 |
| `tools/integration_test.py` | 정상·오류·traceId 관련 10개 통합 시나리오 |
| `tools/load_test.py` | 동시 요청, 성공률, TPS, 응답시간 측정 |

## 3. 구현 기능

- **기관별 라우팅:** 기관 코드 A/B에 따라 TCP 목적지 분기
- **YAML 기반 공통 Mapper:** 일반 기관은 설정 기반으로 전문 규격 적용. 특수 기관은 별도 Mapper 확장이 가능하도록 구조 분리
- **TCP 전문 처리:** 요청·응답에 4바이트 ASCII 길이 헤더 사용, 본문 길이·기관 구분 검증
- **Connection Pool:** 기관별 독립 풀, 최대 5개 연결, 연결 재사용, 대기시간·유휴시간 관리, 오류 연결 폐기
- **오류 표준화:** 요청 오류, 기관 오류, 통신 오류 등을 FEP 오류 코드와 HTTP 응답으로 변환
- **거래 추적:** `X-Trace-Id`, SLF4J MDC 및 처리시간(`elapsedMs`) 로그
- **검증 도구:** 통합 회귀 테스트 및 동시 부하 테스트

### 기본 설정

| 항목 | 기관 A | 기관 B |
| --- | --- | --- |
| TCP 포트 | 9001 | 9002 |
| Connect Timeout | 3,000ms | 3,000ms |
| Read Timeout | 5,000ms | 5,000ms |
| 최대 연결 | 5 | 5 |
| 풀 대기 제한 | 2,000ms | 2,000ms |
| 최대 유휴시간 | 20,000ms | 20,000ms |
| 정상 응답 데이터 | `SUCCESS001` | `SUCCESS002` |

## 4. 빌드 및 실행

### 준비 사항

- JDK 21 (`java -version`)
- Maven (`mvn -version`)
- Python 3 (`python3 --version`, 테스트 실행 시)
- 8082, 8083, 9001, 9002 포트 사용 가능

**모든 명령어는 프로젝트 최상위 디렉터리에서 실행합니다.** 먼저 전체 모듈을 빌드합니다.

```bash
mvn clean package
```

아래 명령어는 **각각 별도의 터미널**에서 실행하고, 종료하지 않은 채 다음 서버를 실행합니다.

**터미널 1 — 가상기관 A**

```bash
java -cp mock-institution/target/classes com.minifep.mock.tcp.MockTcpServer A
```

**터미널 2 — 가상기관 B**

```bash
java -cp mock-institution/target/classes com.minifep.mock.tcp.MockTcpServer B
```

**터미널 3 — FEP 서버**

```bash
java -jar fep-server/target/fep-server-1.0.0-SNAPSHOT.jar
```

**터미널 4 — 업무 앱**

```bash
java -jar business-app/target/business-app-1.0.0-SNAPSHOT.jar
```

> `config/fep.yml`은 프로젝트 루트 기준 상대 경로로 로딩되므로, FEP 서버는 반드시 프로젝트 루트에서 실행하세요. 서버는 각각 `Ctrl+C`로 종료할 수 있습니다.

## 5. 거래 호출 및 테스트

### 정상 거래 — 기관 A

```bash
curl -i -X POST http://127.0.0.1:8083/api/business/send \
  -H 'Content-Type: application/json' \
  -d '{"institutionCode":"A","data":"HELLO12345"}'
```

예상 응답: HTTP 200, JSON `{"data":"SUCCESS001"}` 및 응답 헤더 `X-Trace-Id`.

### 정상 거래 — 기관 B

```bash
curl -i -X POST http://127.0.0.1:8083/api/business/send \
  -H 'Content-Type: application/json' \
  -d '{"institutionCode":"B","data":"HELLO12345"}'
```

예상 응답: HTTP 200, JSON `{"data":"SUCCESS002"}`.

### Chapter 8 — 통합 회귀 테스트

가상기관 A/B, FEP 서버, 업무 앱이 모두 정상 기동된 상태에서 실행합니다.

```bash
python3 tools/integration_test.py
```

정상 거래, 재거래, 잘못된 요청, FEP 직접 호출 및 traceId 전달 등 **10개 시나리오**를 검증합니다. 모든 시나리오 통과 시 `10/10 PASS`, 실패 시 종료 코드 1을 반환합니다. 풀 내부 재사용·폐기와 장애 모드는 이 스크립트의 검증 범위에 포함되지 않으므로 서버 로그와 별도 테스트가 필요합니다.

### Chapter 7 — 동시 부하 테스트

```bash
python3 tools/load_test.py --count 200 --concurrency 20 --institution AB
```

- `--count`: 전체 거래 건수
- `--concurrency`: 동시에 요청하는 작업자 수
- `--institution`: `A`, `B`, `AB`(교차 호출)
- 결과: 성공/실패 건수, 성공률, 전체 소요시간, TPS, 평균·최소·최대·P95 응답시간

**Chapter 7 관측 결과(개발 환경에서의 1회 테스트):** 200건 / 동시성 20 / 성공률 100% / **1,071.75 TPS**. 해당 수치는 운영환경 보장 성능이나 지속 처리량을 의미하지 않으며 실행 환경·부하 조건에 따라 달라질 수 있습니다.

### 장애 모드 예시

가상기관을 종료한 뒤 아래처럼 특정 모드로 다시 기동할 수 있습니다.

```bash
java -cp mock-institution/target/classes com.minifep.mock.tcp.MockTcpServer A delay
```

지원 모드: `normal`, `delay`(응답 지연), `wrong-prefix`(기관 구분 불일치), `malformed`(길이 헤더 오류). 장애 테스트를 마친 뒤 `normal` 모드로 재기동하고 통합 테스트를 수행하세요.

### 로그 확인

```bash
tail -f logs/business-app.log
tail -f logs/fep-server.log
```

로그의 `traceId`로 업무 앱과 FEP 서버에서 동일 거래를 추적할 수 있습니다.

## 6. 개발 단계

| Chapter | 주요 구현 |
| --- | --- |
| 1 | Java/Maven 개발환경 및 멀티 모듈 프로젝트 구성 |
| 2 | TCP 통신 모듈 및 가상 외부기관 구축 |
| 2-1 | FEP 공통 설정을 업무 앱에서 분리 |
| 3 | 전문 처리 및 기관별 Mapper |
| 3-1 | YAML 기반 공통 Mapper 개선 |
| 3-2 | 독립 FEP 서버 아키텍처 전환 |
| 4 | traceId 기반 거래 추적 및 처리시간 로깅 |
| 5 | TCP 통신 오류 처리 고도화 |
| 6 | 기관별 TCP Connection Pool 및 유휴 연결 관리 |
| 7 | Python 동시 거래 부하 테스트 |
| 8 | 통합 회귀 테스트 도구 |

## 7. 프로젝트 범위와 한계

이 프로젝트는 로컬 단일 개발 환경에서 FEP 핵심 개념을 검증하기 위한 PoC입니다. 운영환경 적용에는 TLS/mTLS, 인증·인가, 전문 암호화, 장애 복구, 관측·알림, 배포 자동화, 장시간 부하·복원력 테스트 등 별도 설계와 검증이 필요합니다.
