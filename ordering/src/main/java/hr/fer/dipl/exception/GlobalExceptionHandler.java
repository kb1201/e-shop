package hr.fer.dipl.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.error("Resource not found exception: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Resource Not Found");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientInventoryException(InsufficientInventoryException ex) {
        logger.error("Insufficient inventory exception: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Insufficient Inventory");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InventoryReservationException.class)
    public ResponseEntity<Map<String, Object>> handleInventoryReservationException(InventoryReservationException ex) {
        logger.error("Insufficient inventory exception: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Insufficient Inventory");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        logger.error("Security exception: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Access Denied");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        logger.error("Illegal state exception: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
//        logger.error("Unexpected error: {}", ex.getMessage(), ex);
//
//        Map<String, Object> body = new HashMap<>();
//        body.put("timestamp", LocalDateTime.now());
//        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//        body.put("error", "Internal Server Error");
//        body.put("message", "An unexpected error occurred");
//
//        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
}