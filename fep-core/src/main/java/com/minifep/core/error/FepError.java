package com.minifep.core.error;

/** FEP 전체에서 공유하는 오류 분류표. HTTP 상태는 REST 진입점에서만 사용한다. */
public enum FepError {
    INVALID_REQUEST("FEP-1001", "요청 데이터 오류", 400),
    INVALID_INSTITUTION("FEP-1002", "기관 설정 오류", 400),
    CONNECT_TIMEOUT("FEP-2001", "기관 TCP 연결 시간 초과", 504),
    CONNECT_FAILURE("FEP-2002", "기관 TCP 연결 실패", 502),
    READ_TIMEOUT("FEP-2003", "기관 TCP 응답 대기시간 초과", 504),
    INVALID_RESPONSE("FEP-3001", "기관 응답 전문 오류", 502),
    INTERNAL_ERROR("FEP-9000", "FEP 내부 처리 오류", 500);

    private final String code;
    private final String message;
    private final int httpStatus;
    FepError(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    public String code() { return code; }
    public String message() { return message; }
    public int httpStatus() { return httpStatus; }
}
