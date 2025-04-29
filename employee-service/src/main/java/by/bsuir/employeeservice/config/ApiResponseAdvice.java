package by.bsuir.employeeservice.config;

import by.bsuir.employeeservice.DTO.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType contentType,
        Class selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse) {
            return body;
        }
        return ApiResponse.<Object>builder()
                .data(body)
                .status(true)
                .message("OK")
                .build();
    }
}
