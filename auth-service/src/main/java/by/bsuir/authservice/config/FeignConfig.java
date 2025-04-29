package by.bsuir.authservice.config;

import by.bsuir.authservice.DTO.ApiResponse;
import by.bsuir.authservice.exception.ApiResponseErrorDecoder;
import by.bsuir.authservice.exception.AppException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new ApiResponseErrorDecoder();
    }

    private static class ApiResponseErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultDecoder = new Default();
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public Exception decode(String methodKey, Response response) {
            String body;
            try {
                body = Util.toString(response.body().asReader());
            } catch (Exception e) {
                return defaultDecoder.decode(methodKey, response);
            }

            try {
                ApiResponse<?> apiResp = mapper.readValue(
                        body,
                        new TypeReference<ApiResponse<?>>() {}
                );
                return new AppException(
                        apiResp.getMessage(),
                        HttpStatus.valueOf(response.status())
                );
            } catch (Exception e) {
                return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}
