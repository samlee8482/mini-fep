package com.minifep.core.service;

import com.minifep.core.config.InstitutionRegistry;
import com.minifep.core.config.InstitutionRegistry.ConnectionInfo;
import com.minifep.core.mapper.MessageMapper;
import com.minifep.core.mapper.MessageMapperRegistry;
import com.minifep.core.message.FepMessage;
import com.minifep.core.tcp.TcpClient;
import com.minifep.core.error.FepError;
import com.minifep.core.error.FepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 기존 YAML Mapper와 TCP 송수신 호출 순서를 변경하지 않는 공통 FEP 서비스.
 * MDC의 traceId는 동일 요청 스레드에서 호출되는 TcpClient 로그에도 유지된다.
 * 전문 원문은 개인정보 노출 위험이 있어 로그에 남기지 않고 길이만 기록한다.
 */
public class FepService {
    private static final Logger log = LoggerFactory.getLogger(FepService.class);
    private final InstitutionRegistry registry;
    private final MessageMapperRegistry mapperRegistry;
    private final TcpClient tcpClient;

    public FepService(InstitutionRegistry registry, MessageMapperRegistry mapperRegistry) {
        this.registry = registry;
        this.mapperRegistry = mapperRegistry;
        this.tcpClient = new TcpClient();
    }

    public FepMessage send(String institutionCode, FepMessage request) {
        // 기존 예외 전파 방식을 유지한다. 최종 성공/실패는 REST Controller에서 기록한다.
        log.info("stage=INSTITUTION_LOOKUP status=START institution={}", institutionCode);
        ConnectionInfo info;
        MessageMapper mapper;
        try {
            info = registry.get(institutionCode);
            mapper = mapperRegistry.get(institutionCode);
        } catch (IllegalArgumentException ex) {
            throw new FepException(FepError.INVALID_INSTITUTION, ex);
        }
        log.info("stage=INSTITUTION_LOOKUP status=SUCCESS institution={}", institutionCode);

        byte[] institutionRequest;
        try { institutionRequest = mapper.toInstitution(request); }
        catch (IllegalArgumentException ex) { throw new FepException(FepError.INVALID_REQUEST, ex); }
        log.info("stage=REQUEST_MAPPING status=SUCCESS institution={} bytes={}",
                institutionCode, institutionRequest.length);

        byte[] institutionResponse = tcpClient.send(info.host(), info.port(), institutionRequest,
                info.connectTimeout(), info.readTimeout());
        log.info("stage=TCP_RESPONSE status=SUCCESS institution={} bytes={}",
                institutionCode, institutionResponse.length);

        FepMessage response;
        try { response = mapper.fromInstitution(institutionResponse); }
        catch (IllegalArgumentException ex) { throw new FepException(FepError.INVALID_RESPONSE, ex); }
        log.info("stage=RESPONSE_MAPPING status=SUCCESS institution={}", institutionCode);
        return response;
    }
}
