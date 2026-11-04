package com.minifep.server;

import com.minifep.core.message.FepMessage;
import com.minifep.core.service.FepService;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업무 앱의 REST 요청을 받는 독립 FEP 서버 진입점.
 * X-Trace-Id가 있으면 이어받고, 없으면 새로 생성한다.
 * MDC는 현재 요청 스레드의 로그에 traceId를 자동으로 추가한다.
 * 반드시 finally에서 MDC를 제거해 다른 요청으로 추적 ID가 섞이지 않도록 한다.
 */
@RestController
@RequestMapping("/api/fep")
public class FepController {
    private static final Logger log = LoggerFactory.getLogger(FepController.class);
    private final FepService fepService;

    public FepController(FepService fepService) {
        this.fepService = fepService;
    }

    @PostMapping("/send")
    public FepRestResponse send(@RequestBody FepRestRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String incomingTraceId) {
        // 외부에서 전달된 임의의 로그 문자열을 허용하지 않도록 UUID 형식을 검증한다.
        String traceId = validTraceId(incomingTraceId);
        MDC.put("traceId", traceId);
        long started = System.nanoTime();
        String institution = request == null ? "UNKNOWN" : String.valueOf(request.institutionCode());
        log.info("stage=FEP_RECEIVED status=START institution={}", institution);
        try {
            if (request == null || request.institutionCode() == null
                    || request.institutionCode().isBlank()) {
                throw new IllegalArgumentException("기관 코드가 없습니다.");
            }
            FepMessage commonRequest = new FepMessage(request.data());
            FepMessage commonResponse = fepService.send(request.institutionCode(), commonRequest);
            log.info("stage=FEP_FINISHED status=SUCCESS institution={} elapsedMs={}",
                    institution, elapsedMs(started));
            // 기존 JSON 응답 형식(data 필드)을 변경하지 않는다.
            return new FepRestResponse(commonResponse.data());
        } catch (IllegalArgumentException ex) {
            log.warn("stage=FEP_FINISHED status=ERROR institution={} errorType={} elapsedMs={}",
                    institution, ex.getClass().getSimpleName(), elapsedMs(started));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error("stage=FEP_FINISHED status=ERROR institution={} errorType={} elapsedMs={}",
                    institution, ex.getClass().getSimpleName(), elapsedMs(started), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "기관 TCP 통신 실패", ex);
        } finally {
            MDC.remove("traceId");
        }
    }

    private static String validTraceId(String value) {
        if (value != null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return value.toLowerCase(java.util.Locale.ROOT);
        }
        return UUID.randomUUID().toString();
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    public record FepRestRequest(String institutionCode, String data) { }
    public record FepRestResponse(String data) { }
}
