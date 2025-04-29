package by.bsuir.authservice.exception;

import by.bsuir.authservice.DTO.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import feign.Util;
import org.springframework.http.HttpStatus;

import java.io.IOException;

public class ApiResponseErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String body;
        try {
            body = Util.toString(response.body().asReader());
        } catch (IOException ioe) {
            return defaultDecoder.decode(methodKey, response);
        }

        try {
            ApiResponse<?> apiResp = mapper.readValue(body, new TypeReference<ApiResponse<?>>() {});

            return new AppException(
                apiResp.getMessage(),
                HttpStatus.valueOf(response.status())
            );
        } catch (Exception e) {
            return defaultDecoder.decode(methodKey, response);
        }
    }
}
