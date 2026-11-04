package com.minifep.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 업무 애플리케이션의 독립 실행 진입점.
 * 기존 CommandLineRunner를 제거하여 기동 시 FEP 서버에 요청하지 않는다.
 * 업무 요청은 BusinessController가 HTTP로 수신한 시점에만 처리한다.
 * FEP 서버가 중지되어 있어도 업무 앱 자체는 기동 및 대기할 수 있다.
 */
@SpringBootApplication
public class BusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }
}
