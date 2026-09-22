package com.resipay.idempotency.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resipay.idempotency.domain.IdempotencyRecord;
import com.resipay.idempotency.exception.IdempotencyConflictException;
import com.resipay.idempotency.exception.IdempotencyInProgressException;
import com.resipay.idempotency.service.IdempotencyService;
import com.resipay.idempotency.service.LockResult;
import com.resipay.idempotency.util.PayloadHasher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        // Only filter mutating payment requests
        return !("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/payments"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RepeatableContentCachingRequestWrapper wrappedRequest = new RepeatableContentCachingRequestWrapper(request);
        String idempotencyKey = resolveIdempotencyKey(wrappedRequest);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.debug("No idempotency key provided for {}; bypassing idempotency filter", request.getRequestURI());
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        String method = wrappedRequest.getMethod();
        String path = wrappedRequest.getRequestURI();
        String body = wrappedRequest.getBodyAsString();
        String payloadHash = PayloadHasher.computeHash(method, path, body);

        LockResult lockResult;
        try {
            lockResult = idempotencyService.acquireLock(
                    idempotencyKey,
                    method,
                    path,
                    payloadHash,
                    Duration.ofHours(24),
                    2000 // wait up to 2 seconds for in-progress concurrent requests
            );
        } catch (IdempotencyConflictException e) {
            writeErrorResponse(response, HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), idempotencyKey);
            return;
        } catch (IdempotencyInProgressException e) {
            response.setHeader("Retry-After", "1");
            writeErrorResponse(response, HttpStatus.CONFLICT, e.getMessage(), idempotencyKey);
            return;
        }

        // 1. Replay cached response (IDEM-01)
        if (lockResult.replayed()) {
            IdempotencyRecord record = lockResult.record();
            log.info("Replaying cached response for idempotency key: {}", idempotencyKey);
            response.setStatus(record.getResponseCode() != null ? record.getResponseCode() : 200);
            response.setHeader(IDEMPOTENT_REPLAYED_HEADER, "true");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (record.getResponseBody() != null) {
                response.getWriter().write(record.getResponseBody());
            }
            response.getWriter().flush();
            return;
        }

        // 2. Newly acquired lock: execute and record response
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            int responseCode = wrappedResponse.getStatus();
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

            if (responseCode < 500) {
                idempotencyService.complete(idempotencyKey, responseCode, responseBody);
            } else {
                idempotencyService.fail(idempotencyKey);
            }

            wrappedResponse.copyBodyToResponse();
        } catch (Exception ex) {
            idempotencyService.fail(idempotencyKey);
            wrappedResponse.copyBodyToResponse();
            throw ex;
        }
    }

    private String resolveIdempotencyKey(RepeatableContentCachingRequestWrapper request) {
        String headerKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey.trim();
        }

        // Fallback to inspecting JSON body if available
        try {
            String body = request.getBodyAsString();
            if (body != null && !body.isBlank()) {
                JsonNode node = objectMapper.readTree(body);
                if (node.hasNonNull("idempotencyKey")) {
                    String jsonKey = node.get("idempotencyKey").asText();
                    if (!jsonKey.isBlank()) {
                        return jsonKey.trim();
                    }
                }
            }
        } catch (Exception ignored) {
            // Not valid JSON or parsing error, fallback to null
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message, String key) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> errorBody = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "idempotencyKey", key
        );

        String json = objectMapper.writeValueAsString(errorBody);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
