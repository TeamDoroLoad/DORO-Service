package com.doroload.api.common.error;

import com.doroload.api.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 모든 Controller의 예외를 REST 명세서 4.4 형식의 단일 오류 Body로 변환
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Bean Validation(@Valid) 실패를 400으로 변환하고, 좌표 Field 오류면 INVALID_COORDINATE를 사용
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorCode code = containsCoordinateField(details) ? ErrorCode.INVALID_COORDINATE : ErrorCode.INVALID_ARGUMENT;
        return build(code, "요청 값을 확인해주세요.", request, details);
    }

    // Query Parameter 등 Method 인자 제약 위반을 400으로 변환
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.INVALID_ARGUMENT, "요청 값을 확인해주세요.", request, details);
    }

    // JSON 본문 파싱 실패를 400으로 변환
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(ErrorCode.INVALID_ARGUMENT, "요청 본문을 해석할 수 없습니다.", request, null);
    }

    // 지원하지 않는 HTTP Method 호출을 400으로 변환
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(ErrorCode.INVALID_ARGUMENT, "지원하지 않는 요청 방식입니다.", request, null);
    }

    // 도메인 예외(NotFound·RateLimit·Dependency·Validation)를 각자의 ErrorCode로 변환
    @ExceptionHandler(DoroLoadException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DoroLoadException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> details =
                ex instanceof ValidationException ve ? ve.details() : null;
        return build(ex.errorCode(), ex.getMessage(), request, details);
    }

    // MySQL 연결 실패 등 DB 접근 예외를 503으로 변환하고 내부 상세는 Log에만 남김
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("[{}] DB 접근 실패: {}", RequestContext.currentRequestId(), ex.getMessage(), ex);
        return build(ErrorCode.DATABASE_UNAVAILABLE, "일시적으로 서비스를 이용할 수 없습니다.", request, null);
    }

    // 분류되지 않은 모든 예외는 500으로 변환하고 Stack Trace는 Client에 노출하지 않음
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("[{}] 예상하지 못한 오류", RequestContext.currentRequestId(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "알 수 없는 오류가 발생했습니다.", request, null);
    }

    private boolean containsCoordinateField(List<ErrorResponse.FieldViolation> details) {
        return details.stream()
                .anyMatch(d -> d.field().contains("latitude") || d.field().contains("longitude"));
    }

    private ResponseEntity<ErrorResponse> build(
            ErrorCode code, String message, HttpServletRequest request, List<ErrorResponse.FieldViolation> details) {
        String requestId = RequestContext.currentRequestId();
        ErrorResponse body = ErrorResponse.of(
                code.httpStatus().value(), code.name(), message, request.getRequestURI(), requestId, details);
        return ResponseEntity.status(code.httpStatus()).body(body);
    }
}
