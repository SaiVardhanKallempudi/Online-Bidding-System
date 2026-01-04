package com.application.example.online_bidding_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework. validation.FieldError;
import org.springframework.web.bind. MethodArgumentNotValidException;
import org.springframework.web. bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation. RestControllerAdvice;
import org. springframework.web.context.request.WebRequest;

import java. time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle ResourceNotFoundException
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
          ResourceNotFoundException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("error", "Not Found");
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
  }

  /**
   * Handle BadRequestException
   */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequestException(
          BadRequestException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Bad Request");
    body.put("message", ex. getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle UnauthorizedException
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, Object>> handleUnauthorizedException(
          UnauthorizedException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime. now().toString());
    body.put("status", HttpStatus.UNAUTHORIZED.value());
    body.put("error", "Unauthorized");
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handle BadCredentialsException (Login failed)
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
          BadCredentialsException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", HttpStatus.UNAUTHORIZED.value());
    body.put("error", "Unauthorized");
    body.put("message", "Invalid email or password");
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Handle AccessDeniedException (Forbidden)
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
          AccessDeniedException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime. now().toString());
    body.put("status", HttpStatus.FORBIDDEN.value());
    body.put("error", "Forbidden");
    body.put("message", "You don't have permission to access this resource");
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
  }

  /**
   * Handle Validation Errors (@Valid)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
          MethodArgumentNotValidException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation Error");
    body.put("path", request.getDescription(false).replace("uri=", ""));

    // Collect all validation errors
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors. put(fieldName, errorMessage);
    });

    body.put("message", "Validation failed");
    body.put("errors", errors);

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle IllegalArgumentException
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
          IllegalArgumentException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime. now().toString());
    body.put("status", HttpStatus.BAD_REQUEST. value());
    body.put("error", "Bad Request");
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle RuntimeException
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(
          RuntimeException ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime. now().toString());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    // Log the error
    System.err. println("RuntimeException: " + ex.getMessage());
    ex.printStackTrace();

    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handle all other exceptions (Generic)
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGlobalException(
          Exception ex,
          WebRequest request) {

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message", "An unexpected error occurred.  Please try again later.");
    body.put("path", request. getDescription(false).replace("uri=", ""));

    // Log the error
    System.err.println("Exception: " + ex.getMessage());
    ex.printStackTrace();

    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}