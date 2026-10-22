package com.HieuPahm.AniHoyo.utils.error;

import java.time.Instant;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.HieuPahm.AniHoyo.domain.RestResponse;
import com.turkraft.springfilter.parser.InvalidSyntaxException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Single place that turns every exception into the {@link RestResponse} envelope.
 *
 * <p>
 * Two invariants matter here and were broken before:
 * <ol>
 * <li>the HTTP status must equal {@code body.statusCode} — a response that says
 * 403 in the body while the wire says 400 makes clients (axios, browsers,
 * proxies) report the wrong thing;</li>
 * <li>an unexpected server-side failure (SQL error, NPE, ...) must be reported
 * as 5xx with its real cause, never disguised as a permission problem.</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalException {

    private static final Pattern COLUMN_NAME = Pattern.compile("(?:column|field) '([^']+)'",
            Pattern.CASE_INSENSITIVE);

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<RestResponse<Object>> error(HttpStatus status, String message, String detail,
            HttpServletRequest request) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(status.value());
        res.setMessage(message);
        res.setError(detail);
        if (request != null) {
            res.setPath(request.getRequestURI());
        }
        res.setTimestamp(Instant.now());
        return ResponseEntity.status(status).body(res);
    }

    /** Deepest cause message — MySQL/JDBC put the useful text there. */
    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        String message = ex.getMessage();
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getMessage();
            }
        }
        return message;
    }

    private static String columnOf(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = COLUMN_NAME.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String violationsOf(BindingResult result) {
        if (result == null) {
            return null;
        }
        return result.getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    // ------------------------------------------------------ business exceptions

    @ExceptionHandler(value = {
            UsernameNotFoundException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<RestResponse<Object>> handleBadCredentials(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu", ex.getMessage(), request);
    }

    @ExceptionHandler(value = {
            BadActionException.class
    })
    public ResponseEntity<RestResponse<Object>> handleBadActionException(Exception ex, HttpServletRequest request) {
        log.warn("Bad request on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", ex.getMessage(), request);
    }

    @ExceptionHandler(value = {
            ForbidenException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<RestResponse<Object>> handlePermissionException(Exception ex, HttpServletRequest request) {
        log.warn("Forbidden on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này",
                ex.getMessage(), request);
    }

    // Handle file
    @ExceptionHandler(value = {
            StorageException.class
    })
    public ResponseEntity<RestResponse<Object>> handleUploadFileException(Exception ex, HttpServletRequest request) {
        log.error("Storage error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.BAD_REQUEST, "Lỗi lưu trữ tệp tin", ex.getMessage(), request);
    }

    // ------------------------------------------------------------ 404 / 405 / 415

    @ExceptionHandler(value = {
            NoResourceFoundException.class,
            NoHandlerFoundException.class,
            NoSuchElementException.class,
    })
    public ResponseEntity<RestResponse<Object>> handleNotFoundException(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên được yêu cầu",
                ex.getMessage() != null ? ex.getMessage() : request.getRequestURI(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED,
                "Phương thức " + ex.getMethod() + " không được hỗ trợ cho " + request.getRequestURI(),
                ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RestResponse<Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type không được hỗ trợ", ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestResponse<Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp tải lên vượt quá dung lượng cho phép", ex.getMessage(),
                request);
    }

    // ------------------------------------------------------------ invalid input

    /**
     * Bean-validation failures ({@code @Valid} on a body or a form) and the
     * filter/sort parameters that point at a field which does not exist.
     */
    @ExceptionHandler(value = {
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ResponseEntity<RestResponse<Object>> handleValidation(Exception ex, HttpServletRequest request) {
        BindingResult result = ex instanceof BindException bindException ? bindException.getBindingResult() : null;
        String detail = violationsOf(result);
        if (detail == null) {
            detail = ex.getMessage();
        }
        log.warn("Validation failed on {} {}: {}", request.getMethod(), request.getRequestURI(), detail);
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ", detail, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RestResponse<Object>> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ", detail, request);
    }

    /** Body that could not be parsed at all (bad JSON, unknown enum value, wrong type...). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Unreadable body on {} {}: {}", request.getMethod(), request.getRequestURI(), rootMessage(ex));
        return error(HttpStatus.BAD_REQUEST, "Nội dung JSON không hợp lệ hoặc sai kiểu dữ liệu",
                rootMessage(ex), request);
    }

    @ExceptionHandler(value = {
            MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<RestResponse<Object>> handleBadParameter(Exception ex, HttpServletRequest request) {
        log.warn("Bad parameter on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Tham số yêu cầu không hợp lệ", rootMessage(ex), request);
    }

    @ExceptionHandler(InvalidSyntaxException.class)
    public ResponseEntity<RestResponse<Object>> handleInvalidFilterSyntax(InvalidSyntaxException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Cú pháp tham số 'filter' không hợp lệ", ex.getMessage(), request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<RestResponse<Object>> handleUnknownProperty(PropertyReferenceException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
                "Trường '" + ex.getPropertyName() + "' không tồn tại để lọc/sắp xếp", ex.getMessage(), request);
    }

    // ------------------------------------------------------------------ database

    /**
     * Database rejections caused by the payload itself (value too long for a column,
     * duplicate key, missing required value, ...). Previously these surfaced as an
     * opaque 500 — and thanks to the /error dispatch, as a bogus 403.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String root = rootMessage(ex);
        String lower = root == null ? "" : root.toLowerCase(Locale.ROOT);
        String column = columnOf(root);

        log.error("Data integrity violation on {} {}: {}", request.getMethod(), request.getRequestURI(), root);

        if (lower.contains("data too long") || lower.contains("data truncation")
                || lower.contains("out of range value")) {
            String message = column != null
                    ? "Dữ liệu gửi lên quá dài/dài hơn giới hạn của cột '" + column + "' trong cơ sở dữ liệu"
                    : "Dữ liệu gửi lên vượt quá giới hạn cho phép của cơ sở dữ liệu";
            return error(HttpStatus.BAD_REQUEST, message,
                    root + " (kiểm tra lại độ dài dữ liệu gửi lên, hoặc mở rộng cột tương ứng)", request);
        }
        if (lower.contains("duplicate entry") || lower.contains("unique constraint")
                || lower.contains("duplicate key")) {
            return error(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại (vi phạm ràng buộc duy nhất)", root, request);
        }
        if (lower.contains("cannot be null") || lower.contains("doesn't have a default value")
                || lower.contains("column cannot be null")) {
            return error(HttpStatus.BAD_REQUEST,
                    "Thiếu dữ liệu bắt buộc" + (column != null ? " cho cột '" + column + "'" : ""), root, request);
        }
        if (lower.contains("foreign key constraint")) {
            return error(HttpStatus.CONFLICT, "Dữ liệu tham chiếu không tồn tại hoặc đang được sử dụng", root, request);
        }
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu không thoả mãn ràng buộc của cơ sở dữ liệu", root, request);
    }

    /** Any other database problem is a server-side failure, never a 4xx. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<RestResponse<Object>> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("Database error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi truy cập cơ sở dữ liệu", rootMessage(ex), request);
    }

    /** Flush-time bean validation arrives wrapped in a transaction exception. */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<RestResponse<Object>> handleTransactionSystem(TransactionSystemException ex,
            HttpServletRequest request) {
        Throwable cause = ex.getRootCause() != null ? ex.getRootCause() : ex;
        if (cause instanceof ConstraintViolationException violations) {
            String detail = violations.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ", detail, request);
        }
        log.error("Transaction error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể hoàn tất giao dịch", rootMessage(ex), request);
    }

    // ----------------------------------------------------------- MVC / fallbacks

    /** Keep the status Spring itself computed (405, 415, 404, ...). */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<RestResponse<Object>> handleErrorResponse(ErrorResponseException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return error(status, status.getReasonPhrase(), ex.getBody().getDetail(), request);
    }

    /**
     * Last resort: an unexpected server error keeps its 5xx status and its real
     * cause instead of being rewritten into something misleading.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Object>> handleUncategorized(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Lỗi hệ thống không mong đợi (" + ex.getClass().getSimpleName() + ")",
                rootMessage(ex), request);
    }
}
