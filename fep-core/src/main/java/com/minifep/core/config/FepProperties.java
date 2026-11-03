package com.minifep.core.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [Chapter 3 설계 및 동작 설명]
 * @ConfigurationProperties는 fep.yml의 fep.institutions 설정을 Java record로 변환한다.
 * connect-timeout과 read-timeout은 각각 connectTimeout, readTimeout에 대응한다.
 * 외부 fep.yml의 fep.* 값을 Java 객체로 바인딩한다. */
@ConfigurationProperties(prefix = "fep")
public record FepProperties(Map<String, Institution> institutions) {
    /** 기관별 TCP 주소와 연결/응답 제한 시간(밀리초)이다. */
    public record Institution(String host, int port, int connectTimeout, int readTimeout) { }
}
