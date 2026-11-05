package com.minifep.business;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

/** 업무 앱은 요청 시점에만 FEP 호출. 오류 JSON은 FEP가 만든 원문을 그대로 전달한다. */
@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private static final Logger log = LoggerFactory.getLogger(BusinessController.class);
    private final RestClient fepClient;
    public BusinessController(RestClient.Builder builder, @Value("${fep.server.url}") String url) {
        this.fepClient = builder.baseUrl(url).build();
    }
    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> send(@RequestBody(required = false) FepRestRequest request) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        long started = System.nanoTime();
        String institution = request == null ? "UNKNOWN" : String.valueOf(request.institutionCode());
        log.info("stage=BUSINESS_RECEIVED status=START institution={}", institution);
        try {
            if (request == null || request.institutionCode() == null || request.institutionCode().isBlank()
                    || request.data() == null) {
                return error(400, "FEP-1001", "요청 데이터 오류", traceId, institution, started);
            }
            log.info("stage=FEP_HTTP_REQUEST status=START institution={}", institution);
            // retrieve()는 HTTP 4xx/5xx를 예외로 던지므로 아래에서 본문을 보존한다.
            FepRestResponse response = fepClient.post().uri("/api/fep/send")
                    .header("X-Trace-Id", traceId).contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(FepRestResponse.class);
            if (response == null) return error(502, "FEP-9000", "FEP 서버 응답이 비어 있습니다", traceId, institution, started);
            log.info("stage=BUSINESS_FINISHED status=SUCCESS institution={} elapsedMs={}", institution, elapsedMs(started));
            return ResponseEntity.ok().header("X-Trace-Id", traceId).body(response);
        } catch (RestClientResponseException ex) {
            // HTTP 오류 상태와 JSON 본문을 손실 없이 업무 호출자에게 전달한다.
            log.warn("stage=BUSINESS_FINISHED status=ERROR institution={} httpStatus={} elapsedMs={}",
                    institution, ex.getStatusCode().value(), elapsedMs(started));
            String body = ex.getResponseBodyAsString();
            if (body != null && body.contains("\"code\"") && body.contains("\"traceId\"")) {
                return ResponseEntity.status(ex.getStatusCode()).contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", traceId).body(body);
            }
            return error(502, "FEP-9000", "FEP 서버 응답 오류", traceId, institution, started);
        } catch (RestClientException ex) {
            log.error("stage=BUSINESS_FINISHED status=ERROR institution={} errorType={} elapsedMs={}",
                    institution, ex.getClass().getSimpleName(), elapsedMs(started), ex);
            return error(502, "FEP-9000", "FEP 서버 호출 실패", traceId, institution, started);
        } finally { MDC.remove("traceId"); }
    }
    private ResponseEntity<FepErrorResponse> error(int status, String code, String message, String traceId,
            String institution, long started) {
        log.warn("stage=BUSINESS_FINISHED status=ERROR institution={} errorCode={} elapsedMs={}",
                institution, code, elapsedMs(started));
        return ResponseEntity.status(status).header("X-Trace-Id", traceId)
                .body(new FepErrorResponse(code, message, traceId));
    }
    private static long elapsedMs(long started) { return (System.nanoTime() - started) / 1_000_000; }
    public record FepRestRequest(String institutionCode, String data) { }
    public record FepRestResponse(String data) { }
    public record FepErrorResponse(String code, String message, String traceId) { }
}
