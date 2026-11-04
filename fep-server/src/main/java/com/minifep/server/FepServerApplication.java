package com.minifep.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 3-2의 FEP 독립 실행 진입점.
 * 내장 HTTP 서버를 8080 포트로 실행하고 REST Controller를 등록한다.
 * fep-core가 제공하는 Spring Boot 자동 설정을 통해 FepService Bean도 생성된다.
 * 기관별 TCP 접속 설정은 실행 디렉터리의 config/fep.yml에서 읽는다.
 */
@SpringBootApplication
public class FepServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FepServerApplication.class, args);
    }
}
