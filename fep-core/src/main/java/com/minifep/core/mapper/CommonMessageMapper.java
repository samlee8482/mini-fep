package com.minifep.core.mapper;

import com.minifep.core.message.FepMessage;
import java.nio.charset.StandardCharsets;

/**
 * [Chapter 3-1] 일반 기관이 공통으로 사용하는 전문 변환기.
 * 기관 A/B마다 Java 클래스를 만들지 않고 YAML의 prefix만 바꿔 재사용한다.
 *
 * 전문 본문: [기관 식별자 1바이트][업무 데이터 10바이트] = 11바이트.
 * TCP 4바이트 길이 헤더는 기존 TcpClient가 처리하므로 여기서 붙이지 않는다.
 * FepMessage는 영문/숫자 10자리 검증을 기존대로 수행한다.
 */
public final class CommonMessageMapper implements MessageMapper {
    private final byte prefix;

    /** 설정 오류를 통신 전에 발견하도록 접두문자를 정확히 1자리 ASCII로 검증한다. */
    public CommonMessageMapper(String prefix) {
        if (prefix == null || prefix.length() != 1 || prefix.charAt(0) > 0x7f
                || prefix.charAt(0) < 0x21) {
            throw new IllegalArgumentException("Mapper prefix는 출력 가능한 ASCII 1자리여야 합니다: " + prefix);
        }
        this.prefix = (byte) prefix.charAt(0);
    }

    /** 공통 업무 데이터 앞에 기관 구분값을 붙여 기관용 본문을 생성한다. */
    @Override
    public byte[] toInstitution(FepMessage message) {
        if (message == null) throw new IllegalArgumentException("공통 요청 전문이 없습니다.");
        byte[] data = message.data().getBytes(StandardCharsets.US_ASCII);
        byte[] result = new byte[11];
        result[0] = prefix;
        System.arraycopy(data, 0, result, 1, 10);
        return result;
    }

    /** 기관 응답의 길이 및 구분자를 검사하고 공통 전문으로 되돌린다. */
    @Override
    public FepMessage fromInstitution(byte[] response) {
        if (response == null || response.length != 11 || response[0] != prefix) {
            throw new IllegalArgumentException("기관 응답 전문 길이 또는 구분값 오류: " + (char) prefix);
        }
        return new FepMessage(new String(response, 1, 10, StandardCharsets.US_ASCII));
    }
}
