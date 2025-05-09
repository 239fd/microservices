package by.bsuir.authservice.exception;

import by.bsuir.authservice.DTO.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ApiResponse<Object> resp = ApiResponse.builder()
                .data(null)
                .status(false)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(resp, ex.getCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex) {
        ApiResponse<Object> resp = ApiResponse.builder()
                .data(null)
                .status(false)
                .message("An error occurred: " + ex.getMessage())
                .build();
        return ResponseEntity
                .status(500)
                .body(resp);
    }
}
