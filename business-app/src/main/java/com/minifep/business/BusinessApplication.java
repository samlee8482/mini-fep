
package com.minifep.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mini FEP 업무 애플리케이션 시작 클래스
 *
 * 역할:
 * 1. Spring Boot 애플리케이션 실행
 * 2. 향후 업무 요청을 수신하는 REST API 제공
 * 3. FEP 공통 모듈을 호출하여 대외기관과 통신
 *
 * 실제 TCP 통신과 전문 변환은 fep-core에서 처리한다.
 */
@SpringBootApplication
public class BusinessApplication {

    public static void main(String[] args) {

        // Spring Boot 애플리케이션 시작
        SpringApplication.run(BusinessApplication.class, args);
    }
}
