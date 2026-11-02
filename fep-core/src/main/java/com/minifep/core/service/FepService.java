
package com.minifep.core.service;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.config.InstitutionRegistry.ConnectionInfo;
import com.minifep.core.tcp.TcpClient;

import java.io.IOException;

/**
 * FEP 공통 서비스
 *
 * 업무 애플리케이션은 기관 코드와 요청 전문만 전달한다.
 * 기관별 접속 정보 조회 및 TCP 송수신은 FEP가 담당한다.
 */
public class FepService {

    private final InstitutionRegistry registry;
    private final TcpClient tcpClient;

    public FepService(InstitutionRegistry registry) {
        this.registry = registry;
        this.tcpClient = new TcpClient();
    }

    /**
     * 기관 코드 기반 전문 송수신
     *
     * @param institutionCode 기관 코드
     * @param request 요청 전문
     * @param responseLength 응답 전문 길이
     * @return 응답 전문
     */
    public byte[] send(
            String institutionCode,
            byte[] request,
            int responseLength) throws IOException {

        // 1. 기관 코드로 접속 정보 조회
        ConnectionInfo info = registry.get(institutionCode);

        // 2. 공통 TCP Client를 이용해 전문 송수신
        return tcpClient.send(
            info.host(),
            info.port(),
            request,
            responseLength,
            info.connectTimeout(),
            info.readTimeout()
        );
    }
}
