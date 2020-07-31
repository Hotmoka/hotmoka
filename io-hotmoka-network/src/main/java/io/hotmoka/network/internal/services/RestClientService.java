package io.hotmoka.network.internal.services;

import io.hotmoka.network.models.network.ErrorModel;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * A Rest client class with a custom error handler
 */
public class RestClientService {

    private static RestTemplate getRestTemplate() {
       return new RestTemplateBuilder().errorHandler(new ErrorHandler()).build();
    }

    /**
     * It returns an entity T as response by doing a GET request
     * @param url the url
     * @param response the response class type
     * @param <T> the entity response type
     * @return the response
     */
    public static <T> T get(String url, Class<T> response) {
        return getRestTemplate().getForEntity(url, response).getBody();
    }

    /**
     * It returns an entity T as response by doing a POST request
     * @param url the url
     * @param requestBody the request body
     * @param response the response class type
     * @param <T> the entity response type
     * @param <R> the entity request type
     * @return the response
     */
    public static <T, R> T post(String url, R requestBody, Class<T> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<R> request = new HttpEntity<>(requestBody, headers);
        return getRestTemplate().postForEntity(url, request, response).getBody();
    }

    /**
     * Custom error handler for the rest client class
     */
    static class ErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
            return clientHttpResponse.getStatusCode().is4xxClientError() || clientHttpResponse.getStatusCode().is5xxServerError();
        }

        @Override
        public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {

            if (clientHttpResponse.getStatusCode().is5xxServerError()) {
                // TODO

            } else if (clientHttpResponse.getStatusCode().is4xxClientError()) {
                ErrorModel error = ErrorModel.from(clientHttpResponse.getBody());
                throw new NetworkExceptionResponse(clientHttpResponse.getStatusCode(), error.message, error.exceptionType);
            }
        }
    }
}
