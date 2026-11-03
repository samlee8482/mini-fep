package com.minifep.business;

import com.minifep.core.message.FepMessage;
import com.minifep.core.service.FepService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 업무 애플리케이션은 FepMessage에 업무 데이터만 담아 FepService로 전달한다.
 * Spring Boot가 공통 모듈의 FepService Bean을 주입하므로 직접 생성하지 않는다.
 *
 * 업무 애플리케이션은 기관별 TCP 헤더나 기관 구분값을 만들지 않는다.
 * 기관 코드와 공통 업무 데이터만 FEP 공통 서비스로 전달한다.
 */
@SpringBootApplication
public class BusinessApplication implements CommandLineRunner {
    private final FepService fepService;

    // fep-core 자동 설정에서 등록한 FepService를 주입받는다.
    public BusinessApplication(FepService fepService) {
        this.fepService = fepService;
    }

    /**
     * 프로그램의 시작점이다. 실행 인자에 따라 기관 서버 또는 업무 앱을 시작한다.
     */
    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }

    /**
     * Spring Boot 시작 직후 테스트용 기관 A/B 요청을 순서대로 수행한다.
     */
    @Override
    public void run(String... args) throws Exception {
        sendToInstitution("A", "HELLO00001");
        sendToInstitution("B", "HELLO00002");
    }

    private void sendToInstitution(String institutionCode, String data) throws Exception {
        // 공통 전문 객체에는 기관별 prefix나 TCP 길이 헤더가 없다.
        FepMessage request = new FepMessage(data);
        FepMessage response = fepService.send(institutionCode, request);
        System.out.println("[기관 " + institutionCode + "] 요청: " + request.data());
        System.out.println("[기관 " + institutionCode + "] 응답: " + response.data());
    }
}
