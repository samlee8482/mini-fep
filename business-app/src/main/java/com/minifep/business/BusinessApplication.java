
package com.minifep.business;

import com.minifep.core.service.FepService;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Mini FEP 업무 애플리케이션
 *
 * 기관 IP/Port를 직접 관리하지 않는다.
 * FEP 공통 서비스에 기관 코드와 전문만 전달한다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BusinessApplication implements CommandLineRunner {

    private final FepService fepService;

    // Spring Boot가 FepService를 자동 주입
    public BusinessApplication(FepService fepService) {
        this.fepService = fepService;
    }

    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        // 기관 A/B로 업무 요청
        sendToInstitution("A", "HELLO00001");
        sendToInstitution("B", "HELLO00002");
    }

    /**
     * 기관 코드와 요청 전문을 FEP에 전달한다.
     */
    private void sendToInstitution(
            String institutionCode,
            String message) throws Exception {

        // 요청 전문 생성
        byte[] request = message.getBytes(StandardCharsets.UTF_8);

        // FEP 공통 서비스를 통해 TCP 송수신
        byte[] response = fepService.send(
            institutionCode,
            request,
            10
        );

        // 응답 결과 출력
        System.out.println(
            "[기관 " + institutionCode + "] 요청: " + message
        );

        System.out.println(
            "[기관 " + institutionCode + "] 응답: " +
            new String(response, StandardCharsets.UTF_8)
        );
    }
}
