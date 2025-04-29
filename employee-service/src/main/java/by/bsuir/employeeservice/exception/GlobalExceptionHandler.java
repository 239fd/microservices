// src/main/java/by/bsuir/employeeservice/exception/GlobalExceptionHandler.java
package by.bsuir.employeeservice.exception;

import by.bsuir.employeeservice.DTO.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ApiResponse<Object> resp = ApiResponse.builder()
                .data(null)
                .status(false)
                .message(ex.getMessage())
                .build();
        return ResponseEntity
                .status(ex.getCode())
                .body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex) {
        ApiResponse<Object> resp = ApiResponse.builder()
                .data(null)
                .status(false)
                .message("An error occurred: " + ex.getMessage())
                .build();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(resp);
    }
}
