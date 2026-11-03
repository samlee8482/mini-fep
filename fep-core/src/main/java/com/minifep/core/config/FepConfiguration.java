
package com.minifep.core.config;

import com.minifep.core.service.FepService;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * FEP 공통 프레임워크의 Spring Bean 생성 클래스.
 *
 * [역할]
 * 1. 기관별 접속 설정을 InstitutionRegistry에 등록한다.
 * 2. TCP 통신을 담당하는 FepService 객체를 생성한다.
 * 3. 업무 애플리케이션이 FEP 내부 객체를 직접 생성하지 않도록 한다.
 *
 * [동작 흐름]
 * 외부 fep.yml
 *     ↓
 * FepProperties
 *     ↓
 * InstitutionRegistry
 *     ↓
 * FepService
 *     ↓
 * business-app에서 호출
 *
 * [Spring Boot 자동 설정]
 * @AutoConfiguration을 통해 FEP 공통 설정을 자동 등록한다.
 * @EnableConfigurationProperties를 통해 FepProperties를
 * Spring Bean으로 등록하고 외부 설정값을 바인딩한다.
 *
 * 외부 fep.yml 파일 로딩은 별도 단계에서 구현한다.
 */
@AutoConfiguration
@EnableConfigurationProperties(FepProperties.class)
public class FepConfiguration {

    /**
     * 기관별 접속정보를 관리하는 Registry Bean 생성.
     *
     * @param properties YAML 설정값이 바인딩된 객체
     * @return 기관별 접속정보를 보관하는 Registry
     */
    @Bean
    public InstitutionRegistry institutionRegistry(
            FepProperties properties) {

        // YAML에서 읽은 기관별 설정을 Registry 형식으로 변환한다.
        Map<String, InstitutionRegistry.ConnectionInfo> configs =
                properties.institutions().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    FepProperties.Institution info =
                                            entry.getValue();

                                    return new InstitutionRegistry.ConnectionInfo(
                                            info.host(),
                                            info.port(),
                                            info.connectTimeout(),
                                            info.readTimeout()
                                    );
                                }
                        ));

        return new InstitutionRegistry(configs);
    }

    /**
     * FEP 공통 서비스 Bean 생성.
     *
     * @param registry 기관별 접속정보 관리 객체
     * @return TCP 통신에 사용하는 FEP 공통 서비스
     */
    @Bean
    public FepService fepService(
            InstitutionRegistry registry) {

        return new FepService(registry);
    }
}
