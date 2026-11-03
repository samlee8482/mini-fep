package com.minifep.core.service;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.config.InstitutionRegistry.ConnectionInfo;
import com.minifep.core.mapper.MessageMapper;
import com.minifep.core.mapper.MessageMapperRegistry;
import com.minifep.core.message.FepMessage;
import com.minifep.core.tcp.TcpClient;
import java.io.IOException;

/**
 * [Chapter 3 설계 및 동작 설명]
 * FEP 공통 서비스는 기관 접속정보 조회, 요청 매핑, TCP 통신, 응답 역매핑을 순서대로 조정한다.
 * 업무 앱이 IP, 포트, 기관별 전문 규칙을 직접 관리하지 않도록 책임을 분리한다.
 * 업무 앱과 기관별 Mapper/TCP 통신을 연결하는 FEP 공통 서비스이다. */
public class FepService {
    private final InstitutionRegistry registry;
    private final MessageMapperRegistry mapperRegistry;
    private final TcpClient tcpClient;

    public FepService(InstitutionRegistry registry) {
        this.registry = registry;
        this.mapperRegistry = new MessageMapperRegistry();
        this.tcpClient = new TcpClient();
    }

    /**
     * 업무 앱은 기관 코드와 공통 전문만 넘긴다.
     * 1. 기관 설정 조회 → 2. 요청 매핑 → 3. TCP 송수신 → 4. 응답 역매핑.
     */
    /**
     * 기관별 요청을 처리하고 응답을 반환한다. 입력값 및 네트워크 오류는 호출자에게 전달한다.
     */
    public FepMessage send(String institutionCode, FepMessage request) throws IOException {
        ConnectionInfo info = registry.get(institutionCode);
        MessageMapper mapper = mapperRegistry.get(institutionCode);
        byte[] institutionRequest = mapper.toInstitution(request);
        byte[] institutionResponse = tcpClient.send(info.host(), info.port(), institutionRequest,
                info.connectTimeout(), info.readTimeout());
        return mapper.fromInstitution(institutionResponse);
    }
}
