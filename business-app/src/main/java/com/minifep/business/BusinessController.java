package com.minifep.business;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업무 앱의 요청 시점에만 FEP 서버를 호출한다(Chapter 3-2 on-demand 유지).
 * 거래별 Trace ID를 생성해 HTTP X-Trace-Id 헤더로 전달한다.
 * 업무 앱과 FEP 서버는 별도 프로세스이므로 MDC 자체는 공유되지 않는다.
 * 따라서 HTTP 헤더로 ID를 전달하고 각 프로세스에서 MDC를 설정해야 한다.
 */
@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private static final Logger log = LoggerFactory.getLogger(BusinessController.class);
    private final RestClient fepClient;

    public BusinessController(RestClient.Builder builder,
            @Value("${fep.server.url}") String fepServerUrl) {
        this.fepClient = builder.baseUrl(fepServerUrl).build();
    }

    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FepRestResponse send(@RequestBody FepRestRequest request) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        long started = System.nanoTime();
        String institution = request == null ? "UNKNOWN" : String.valueOf(request.institutionCode());
        log.info("stage=BUSINESS_RECEIVED status=START institution={}", institution);
        try {
            if (request == null || request.institutionCode() == null
                    || request.institutionCode().isBlank() || request.data() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기관 코드 또는 업무 데이터가 없습니다.");
            }
            log.info("stage=FEP_HTTP_REQUEST status=START institution={}", institution);
            FepRestResponse response = fepClient.post()
                    .uri("/api/fep/send")
                    .header("X-Trace-Id", traceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FepRestResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FEP 서버 응답이 비어 있습니다.");
            }
            log.info("stage=BUSINESS_FINISHED status=SUCCESS institution={} elapsedMs={}",
                    institution, elapsedMs(started));
            return response;
        } catch (RestClientException ex) {
            log.error("stage=BUSINESS_FINISHED status=ERROR institution={} errorType={} elapsedMs={}",
                    institution, ex.getClass().getSimpleName(), elapsedMs(started), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FEP 서버 호출 실패", ex);
        } catch (ResponseStatusException ex) {
            log.warn("stage=BUSINESS_FINISHED status=ERROR institution={} httpStatus={} elapsedMs={}",
                    institution, ex.getStatusCode().value(), elapsedMs(started));
            throw ex;
        } finally {
            MDC.remove("traceId");
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    public record FepRestRequest(String institutionCode, String data) { }
    public record FepRestResponse(String data) { }
}
