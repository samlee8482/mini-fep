package com.minifep.server;

import com.minifep.core.error.FepError;
import com.minifep.core.error.FepException;
import com.minifep.core.message.FepMessage;
import com.minifep.core.service.FepService;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST 경계: 공통 예외를 HTTP 상태와 표준 JSON으로 바꾼다. 성공 시 기존 data JSON 유지. */
@RestController
@RequestMapping("/api/fep")
public class FepController {
    private static final Logger log = LoggerFactory.getLogger(FepController.class);
    private final FepService fepService;
    public FepController(FepService fepService) { this.fepService = fepService; }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody(required = false) FepRestRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String incomingTraceId) {
        String traceId = validTraceId(incomingTraceId);
        MDC.put("traceId", traceId);
        long started = System.nanoTime();
        String institution = request == null ? "UNKNOWN" : String.valueOf(request.institutionCode());
        log.info("stage=FEP_RECEIVED status=START institution={}", institution);
        try {
            if (request == null || request.institutionCode() == null || request.institutionCode().isBlank()
                    || request.data() == null) throw new FepException(FepError.INVALID_REQUEST);
            FepMessage response;
            try { response = fepService.send(request.institutionCode(), new FepMessage(request.data())); }
            catch (IllegalArgumentException ex) { throw new FepException(FepError.INVALID_REQUEST, ex); }
            log.info("stage=FEP_FINISHED status=SUCCESS institution={} elapsedMs={}", institution, elapsedMs(started));
            return ResponseEntity.ok().header("X-Trace-Id", traceId).body(new FepRestResponse(response.data()));
        } catch (FepException ex) {
            return error(ex.error(), traceId, institution, started, ex);
        } catch (Exception ex) {
            return error(FepError.INTERNAL_ERROR, traceId, institution, started, ex);
        } finally {
            MDC.remove("traceId");
        }
    }

    private ResponseEntity<FepErrorResponse> error(FepError error, String traceId, String institution,
            long started, Exception cause) {
        log.error("stage=FEP_FINISHED status=ERROR institution={} errorCode={} errorType={} elapsedMs={}",
                institution, error.code(), cause.getClass().getSimpleName(), elapsedMs(started), cause);
        return ResponseEntity.status(error.httpStatus()).header("X-Trace-Id", traceId)
                .body(new FepErrorResponse(error.code(), error.message(), traceId));
    }
    private static String validTraceId(String value) {
        if (value != null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            return value.toLowerCase(Locale.ROOT);
        return UUID.randomUUID().toString();
    }
    private static long elapsedMs(long started) { return (System.nanoTime() - started) / 1_000_000; }
    public record FepRestRequest(String institutionCode, String data) { }
    public record FepRestResponse(String data) { }
    public record FepErrorResponse(String code, String message, String traceId) { }
}
