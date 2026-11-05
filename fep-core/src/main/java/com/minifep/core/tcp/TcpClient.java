package com.minifep.core.tcp;

import com.minifep.core.error.FepError;
import com.minifep.core.error.FepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * [Chapter 6] 기존 전문 규격 [4바이트 ASCII 본문 길이][본문]을 그대로 사용하는 TCP 클라이언트.
 * 매 거래마다 Socket을 생성하지 않고 기관별 Pool에서 빌려 사용한다.
 * 정상 응답을 완전히 수신한 경우에만 재사용하며, 오류 발생 시 소켓을 폐기한다.
 * 하나의 소켓은 한 시점에 한 거래만 수행한다(요청/응답 뒤섞임 방지).
 */
public final class TcpClient {
    private static final Logger log = LoggerFactory.getLogger(TcpClient.class);
    private static final int HEADER_LENGTH = 4;
    private static final int MAX_BODY_LENGTH = 9999;

    public byte[] send(TcpConnectionPool pool, byte[] request) {
        if (request == null || request.length == 0 || request.length > MAX_BODY_LENGTH) {
            throw new FepException(FepError.INVALID_REQUEST);
        }
        long started = System.nanoTime();
        Socket socket = pool.borrow();
        boolean healthy = false;
        try {
            OutputStream out = socket.getOutputStream();
            out.write(String.format(Locale.ROOT, "%04d", request.length).getBytes(StandardCharsets.US_ASCII));
            out.write(request);
            out.flush();
            log.info("stage=TCP_SEND status=SUCCESS bytes={}", request.length);

            InputStream in = socket.getInputStream();
            byte[] header = in.readNBytes(HEADER_LENGTH);
            if (header.length != HEADER_LENGTH) {
                throw new FepException(FepError.INVALID_RESPONSE, new EOFException("응답 헤더 부족"));
            }
            String lengthText = new String(header, StandardCharsets.US_ASCII);
            if (!lengthText.matches("[0-9]{4}")) {
                throw new FepException(FepError.INVALID_RESPONSE, new IOException("응답 길이 헤더 형식 오류"));
            }
            int length = Integer.parseInt(lengthText);
            if (length <= 0 || length > MAX_BODY_LENGTH) {
                throw new FepException(FepError.INVALID_RESPONSE, new IOException("응답 길이 범위 오류"));
            }
            byte[] response = in.readNBytes(length);
            if (response.length != length) {
                throw new FepException(FepError.INVALID_RESPONSE, new EOFException("응답 본문 부족"));
            }
            healthy = true;
            log.info("stage=TCP_RECEIVE status=SUCCESS bytes={} elapsedMs={}",
                    response.length, (System.nanoTime() - started) / 1_000_000);
            return response;
        } catch (SocketTimeoutException e) {
            throw new FepException(FepError.READ_TIMEOUT, e);
        } catch (IOException e) {
            throw new FepException(FepError.CONNECT_FAILURE, e);
        } finally {
            // 응답 전문이 완전히 수신되었을 때만 재사용할 수 있다.
            // 매핑 오류는 이 단계 이후에 발생하므로 연결 자체는 정상으로 간주한다.
            if (healthy) pool.release(socket);
            else pool.invalidate(socket);
        }
    }
}
