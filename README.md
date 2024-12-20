# Account (계좌 관리) 시스템

## 프로젝트 개요

Account(계좌) 시스템은 사용자와 계좌의 정보를 저장하고 있고 아래와 같은 기능을 제공합니다.
1. 외부 시스템에서 거래를 요청할 경우 거래 정보를 받아서 계좌에서 잔액을 거래금액만큼 줄이기(결제)
2. 거래금액만큼 늘리는(결제 취소) 거래 관리 기능을 제공

## 기술 스택

*   Spring Boot
*   Java
*   H2 DB (Memory Mode)
*   Spring Data JPA
*   Embedded Redis
*   JSON (API Request/Response)
*   IntelliJ IDE

## 기능 목록 (API 명세)

### 계좌 관련 API

#### 1. 계좌 생성

*   **요청:** 사용자 아이디, 초기 잔액
*   **응답 (성공):** 사용자 아이디, 생성된 계좌 번호 (랜덤 숫자 10자리 정수 구성), 등록 일시
*   **응답 (실패):** 사용자 없음, 계좌 10개 초과

#### 2. 계좌 해지

*   **요청:** 사용자 아이디, 계좌 번호
*   **응답 (성공):** 사용자 아이디, 계좌 번호, 해지 일시
*   **응답 (실패):** 사용자 없음, 소유주 불일치, 이미 해지됨, 잔액 존재

#### 3. 계좌 확인

*   **요청:** 사용자 아이디
*   **응답 (성공):** 계좌 번호, 잔액 -> json list 형식
*   **응답 (실패):** 사용자 없음

### 거래 (Transaction) 관련 API

#### 1. 잔액 사용

*   **요청:** 사용자 아이디, 계좌 번호, 거래 금액
*   **응답 (성공):** 계좌 번호, 거래 결과, 거래 아이디, 거래 금액, 거래 일시
*   **응답 (실패):** 사용자 없음, 소유주 불일치, 계좌 해지, 잔액 부족, 금액 오류

#### 2. 잔액 사용 취소

*   **요청:** 거래 아이디, 계좌 번호, 거래 금액
*   **응답 (성공):** 계좌 번호, 거래 결과, 거래 아이디, 거래 취소 금액, 거래 일시
*   **응답 (실패):** 원거래 금액 불일치, 해당 계좌 거래 아님

#### 3. 거래 확인

*   **요청:** 거래 아이디
*   **응답 (성공):** 계좌 번호, 거래 종류, 거래 결과, 거래 아이디, 거래 금액, 거래 일시
*   **응답 (실패):** 거래 아이디 없음

## 동시성 제어

본 프로젝트에서는 Redisson 라이브러리를 사용하여 Redis 기반의 **분산 락** 을 구현하여 동시성 문제를 해결했습니다.
계좌 잔액 변경과 같은 중요한 작업에 락을 적용하여, 여러 요청이 동시에 처리되는 상황에서도 데이터의 무결성을 보장합니다.

## 느낀점

이번 프로젝트를 진행하면서 Spring Boot, Java, 데이터 베이스, API, 동시성 등 다양한 기술들을 접해볼 수 있었습니다. 특히 동시성 문제에 대해 경험 해볼 수 있었던 것은 좋은거 같습니다. 
처음에는 용어들이 어렵고 낯설었지만, 하나씩 찾아보고 코드를 작성하면서 조금씩 이해할 수 있었습니다. 물론 아직 부족한 점이 많지만, 하나하나 편리한 기능들을 습득 한다는게 살짝 신났던거 같습니다.
신나진 않았지만 성공하면 마음이 편해지는 테스트 코드의 중요성을 깨달았고, 앞으로는 코드를 작성할 때 테스트 코드도 함께 작성하는 습관을 들여야겠습니다.
