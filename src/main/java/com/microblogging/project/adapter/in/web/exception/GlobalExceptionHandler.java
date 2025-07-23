package com.microblogging.project.adapter.in.web.exception;

import com.microblogging.project.domain.exception.AlreadyFollowingException;
import com.microblogging.project.domain.exception.CannotFollowSelfException;
import com.microblogging.project.domain.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Handler for MissingRequestHeaderException ---
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException ex, WebRequest request) {
        logger.warn("MissingRequestHeaderException: {}", ex.getMessage());
        String errorCode = "MISSING_REQUIRED_HEADER";
        String errorDescription = "Required header '" + ex.getHeaderName() + "' is not present.";
        ErrorResponse errorResponse = new ErrorResponse(errorCode, errorDescription);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // --- Handlers for your custom domain exceptions ---
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        logger.warn("UserNotFoundException: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("USER_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CannotFollowSelfException.class)
    public ResponseEntity<ErrorResponse> handleCannotFollowSelfException(CannotFollowSelfException ex, WebRequest request) {
        logger.warn("CannotFollowSelfException: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("CANNOT_FOLLOW_SELF", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AlreadyFollowingException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyFollowingException(AlreadyFollowingException ex, WebRequest request) {
        logger.warn("AlreadyFollowingException: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("ALREADY_FOLLOWING", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // --- Handler for @Valid / @Validated validation errors on request body ---
    //@Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.warn("MethodArgumentNotValidException: {}", ex.getMessage());

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "Request validation failed: " + errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // --- Handler for malformed JSON or invalid request body content ---
    //@Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        logger.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        String detailMessage = "Request body is malformed or not readable.";
        if (ex.getCause() != null) {
            detailMessage += " Reason: " + ex.getCause().getMessage();
        }

        ErrorResponse errorResponse = new ErrorResponse("INVALID_REQUEST_BODY", detailMessage);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // --- Generic Exception Handler ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtExceptions(Exception ex, WebRequest request) {
        logger.error("An unexpected internal server error occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}