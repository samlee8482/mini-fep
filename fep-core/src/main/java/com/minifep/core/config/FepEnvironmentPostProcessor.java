
package com.minifep.core.config;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * FEP 공통 프레임워크의 외부 YAML 설정 로딩 클래스.
 *
 * [역할]
 * 1. Spring Boot 애플리케이션 시작 시 외부 fep.yml을 읽는다.
 * 2. YAML 설정값을 Spring Environment에 등록한다.
 * 3. 업무 애플리케이션이 FEP 설정 파일을 직접 로딩하지 않도록 한다.
 *
 * [설정 파일 위치]
 * ./config/fep.yml
 *
 * 실행 프로세스의 현재 작업 디렉터리를 기준으로 파일을 찾는다.
 *
 * [동작 흐름]
 * business-app 실행
 *     ↓
 * FepEnvironmentPostProcessor
 *     ↓
 * 외부 fep.yml 로딩
 *     ↓
 * Spring Environment에 fep.* 설정 등록
 *     ↓
 * FepProperties에 설정값 바인딩
 *
 * 주의:
 * 이 클래스는 기관별 접속정보를 직접 사용하지 않는다.
 * 설정 파일을 읽어 Spring 환경에 등록하는 역할만 담당한다.
 */
public class FepEnvironmentPostProcessor
        implements EnvironmentPostProcessor {

    /**
     * Spring Boot가 Bean을 생성하기 전에 호출하는 메서드.
     *
     * @param environment Spring 설정값 관리 객체
     * @param application 실행 중인 Spring Boot 애플리케이션
     */
    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {

        // 외부 FEP 설정 파일의 경로
        Resource resource =
                new FileSystemResource("./config/fep.yml");

        // 기관 접속정보가 없으면 FEP 통신을 수행할 수 없으므로
        // 애플리케이션 시작 단계에서 오류를 발생시킨다.
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "FEP 설정 파일을 찾을 수 없습니다: ./config/fep.yml");
        }

        try {
            // YAML 파일을 Spring PropertySource 형식으로 변환한다.
            YamlPropertySourceLoader loader =
                    new YamlPropertySourceLoader();

            List<PropertySource<?>> propertySources =
                    loader.load("fepExternalConfig", resource);

            // 읽어온 설정값을 Spring Environment에 등록한다.
            // addLast는 기존 환경변수 등의 설정을 우선 적용하도록 한다.
            for (PropertySource<?> propertySource : propertySources) {
                environment.getPropertySources().addLast(propertySource);
            }

        } catch (IOException e) {
            // 설정 파일을 읽을 수 없다면 애플리케이션 실행을 중단한다.
            throw new IllegalStateException(
                    "FEP 설정 파일 로딩 실패: ./config/fep.yml", e);
        }
    }
}
