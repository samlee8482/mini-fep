package com.minifep.core.mapper;

import com.minifep.core.message.FepMessage;
import java.nio.charset.StandardCharsets;

/**
 * [Chapter 3 설계 및 동작 설명]
 * 기관 A와 B는 접두 문자만 다르므로 중복 코드를 추상 부모 클래스에 모았다.
 * toInstitution은 11바이트 본문만 만든다. 4바이트 길이 헤더는 TcpClient가 담당한다.
 * fromInstitution은 응답 본문 길이와 기관 구분자를 확인하고 공통 전문으로 되돌린다.
 *
 * 기관 A/B에 공통인 전문 규칙을 재사용한다.
 * [기관 구분 1바이트][업무 데이터 10바이트] 형식이다.
 * 실제 금융기관마다 전문 형식이 달라지면 각 기관 Mapper에서 변환한다.
 */
abstract class AbstractPrefixMessageMapper implements MessageMapper {
    private final char prefix;

    protected AbstractPrefixMessageMapper(char prefix) {
        this.prefix = prefix;
    }

    /**
     * 공통 요청을 해당 기관의 TCP 본문 바이트로 변환한다.
     */
    @Override
    public byte[] toInstitution(FepMessage message) {
        // TCP 길이 헤더는 TcpClient가 붙이므로 여기서는 본문만 생성한다.
        return (prefix + message.data()).getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * 기관 응답 본문을 검증한 후 공통 전문으로 변환한다.
     */
    @Override
    public FepMessage fromInstitution(byte[] response) {
        // 응답 기관이 요청한 기관과 다르면 전문 매핑 오류로 처리한다.
        if (response == null || response.length != 11 || response[0] != (byte) prefix) {
            throw new IllegalArgumentException("기관 응답 전문 형식 또는 기관 구분값 오류: " + prefix);
        }
        String data = new String(response, 1, 10, StandardCharsets.US_ASCII);
        return new FepMessage(data);
    }
}
