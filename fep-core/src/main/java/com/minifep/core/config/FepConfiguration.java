package com.minifep.core.config;

import com.minifep.core.mapper.MessageMapper;
import com.minifep.core.mapper.MessageMapperRegistry;
import com.minifep.core.service.FepService;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * FEP 공통 모듈의 Spring Boot 자동 설정.
 * 업무 앱은 TCP 설정 및 Mapper 객체를 직접 생성하지 않는다.
 * 외부 YAML -> FepProperties -> 두 등록소 -> FepService 순서로 Bean을 구성한다.
 */
@AutoConfiguration
@EnableConfigurationProperties(FepProperties.class)
public class FepConfiguration {
    /** 기관 코드별 IP, 포트, 타임아웃을 읽어 접속정보 등록소를 만든다. */
    @Bean
    public InstitutionRegistry institutionRegistry(FepProperties properties) {
        if (properties.institutions() == null) {
            throw new IllegalArgumentException("fep.institutions 설정이 없습니다.");
        }
        Map<String, InstitutionRegistry.ConnectionInfo> configs = properties.institutions().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    FepProperties.Institution info = entry.getValue();
                    return new InstitutionRegistry.ConnectionInfo(info.host(), info.port(),
                            info.connectTimeout(), info.readTimeout());
                }));
        return new InstitutionRegistry(configs);
    }

    /** 일반 Mapper는 YAML로 생성하고 특수 Mapper는 필요 시 등록된 Bean을 사용한다. */
    @Bean
    public MessageMapperRegistry messageMapperRegistry(FepProperties properties,
            @Autowired(required = false) Map<String, MessageMapper> customMappers) {
        // Spring의 Map Bean 주입은 기본적으로 Bean 이름을 키로 사용한다.
        // 기관 코드를 Bean 이름으로 등록하면 custom 유형에서 해당 구현을 찾을 수 있다.
        return new MessageMapperRegistry(properties, customMappers);
    }

    /** 업무 앱이 호출하는 공통 FEP 서비스에 접속/전문 등록소를 주입한다. */
    @Bean
    public FepService fepService(InstitutionRegistry registry, MessageMapperRegistry mapperRegistry) {
        return new FepService(registry, mapperRegistry);
    }
}
