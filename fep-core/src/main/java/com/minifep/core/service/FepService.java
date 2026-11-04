package com.minifep.core.service;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.config.InstitutionRegistry.ConnectionInfo;
import com.minifep.core.mapper.MessageMapper;
import com.minifep.core.mapper.MessageMapperRegistry;
import com.minifep.core.message.FepMessage;
import com.minifep.core.tcp.TcpClient;
import java.io.IOException;

/**
 * FEP 서버가 사용하는 공통 거래 처리 서비스(Chapter 3-1 기능 유지).
 * 이 클래스는 HTTP를 알지 못한다. 호출자는 FEP 서버의 REST Controller이다.
 * 1) 기관 코드로 접속정보/Mapper 조회
 * 2) 공통 10자리 전문을 기관별 11바이트 전문으로 변환
 * 3) TcpClient로 4바이트 길이 헤더를 포함해 송수신
 * 4) 기관 응답을 공통 FepMessage로 변환
 * Chapter 4 Trace ID, MDC, 거래 단계별 로그는 포함하지 않는다.
 */
public class FepService {
    private final InstitutionRegistry registry;
    private final MessageMapperRegistry mapperRegistry;
    private final TcpClient tcpClient;

    public FepService(InstitutionRegistry registry, MessageMapperRegistry mapperRegistry) {
        this.registry = registry;
        this.mapperRegistry = mapperRegistry;
        this.tcpClient = new TcpClient();
    }

    /** 기존 send(String, FepMessage) 시그니처 및 예외 전달 방식을 유지한다. */
    public FepMessage send(String institutionCode, FepMessage request) throws IOException {
        ConnectionInfo info = registry.get(institutionCode);
        MessageMapper mapper = mapperRegistry.get(institutionCode);
        byte[] institutionRequest = mapper.toInstitution(request);
        byte[] institutionResponse = tcpClient.send(info.host(), info.port(), institutionRequest,
                info.connectTimeout(), info.readTimeout());
        return mapper.fromInstitution(institutionResponse);
    }
}
