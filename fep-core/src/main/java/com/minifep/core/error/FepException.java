package com.minifep.core.error;

/** 원본 예외를 cause로 보존하고, 외부에는 정해진 오류코드만 노출한다. */
public class FepException extends RuntimeException {
    private final FepError error;
    public FepException(FepError error, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
    }
    public FepException(FepError error) { this(error, null); }
    public FepError error() { return error; }
}
