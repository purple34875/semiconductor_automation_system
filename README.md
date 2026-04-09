# Process Log Chatbot

반도체 공정 로그를 자연어로 조회하고 답변받을 수 있는 Spring Boot 기반 챗봇 프로젝트입니다.

## 1. 설치가 필요한 기술 스택

프로그램을 실행하려면 아래 항목들이 컴퓨터에 준비되어 있어야 합니다.

- Java 21
  - Spring Boot 서버 실행에 필요합니다.
- MySQL
  - 공정 로그 데이터를 저장하고 조회하는 DB입니다.
- Ollama
  - 로컬에서 LLM을 실행하기 위해 필요합니다.
- Ollama 모델 `exaone3.5:latest`
  - 챗봇 답변 생성에 사용합니다.
- 웹 브라우저
  - 채팅 UI 접속에 사용합니다.

참고:
- Maven은 별도 설치하지 않아도 됩니다.
- 프로젝트에 포함된 `mvnw.cmd`를 사용하면 됩니다.

## 2. 실행 전 설정

프로그램 실행 전, **MySQL DB 안에 `schema.yml` 파일에 정의된 구조의 테이블들이 미리 생성되어 있어야 합니다.**

- DB에는 최소한 아래 테이블들이 존재해야 합니다.
  - `Operator_Master`
  - `Process_Master`
  - `Process_Log`
- 테이블명, 컬럼 구성, 관계는 `schema.yml`을 기준으로 맞춰야 합니다.
- DB 서버만 실행하는 것으로는 충분하지 않고, 실제 데이터베이스와 테이블 구조가 준비되어 있어야 합니다.

프로젝트 루트의 `application-secrets.yml` 파일에 실제 실행 환경 값을 입력합니다.

예시:

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/project_DB?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: change_me
    password: change_me
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: exaone3.5:latest

server:
  port: 8080
```

필요하면 `application-secrets.example.yml`을 참고해서 값을 수정하면 됩니다.

## 3. 프로그램 동작 방법

### 1) Ollama 모델 준비

```powershell
ollama pull exaone3.5:latest
```

### 2) MySQL 실행

MySQL 서버가 켜져 있어야 하며, `application-secrets.yml`에 적은 DB로 접속 가능해야 합니다.

### 3) Ollama 실행

Ollama 서버가 실행 중이어야 합니다.

기본 주소:

```text
http://localhost:11434
```

### 4) Spring Boot 서버 실행

프로젝트 루트에서 아래 명령어를 실행합니다.

```powershell
cd '본인의 파일 경로'
.\mvnw.cmd spring-boot:run
```

### 5) 브라우저에서 접속

서버가 실행되면 아래 주소로 접속합니다.

- 채팅 UI: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 서버 상태 확인: `http://localhost:8080/api/health`

## 4. 빌드 후 실행 방법

원하면 jar 파일로도 실행할 수 있습니다.

```powershell
cd '본인의 파일 경로'
.\mvnw.cmd -DskipTests package
java -jar .\target\process-log-chatbot-0.0.1-SNAPSHOT.jar
```

## 5. 사용 방법

1. 브라우저에서 `http://localhost:8080/` 접속
2. 화면 하단 입력창에 질문 입력
3. Enter로 전송
4. 오른쪽에 사용자 질문, 왼쪽에 챗봇 답변 확인

예시 질문:

- `최근 작업 기록 5건 보여줘`
- `OOO 작업자의 오늘 작업 내역 알려줘`
- `식각 공정의 오늘 작업 건수 알려줘`
