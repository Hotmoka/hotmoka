package io.hotmoka.network.models.network;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Error model to hold the message of the exception and the exception type
 */
public class ErrorModel {
    public final String message;
    public final String exceptionType;

    public ErrorModel(String message, String exceptionType) {
        this.message = message;
        this.exceptionType = exceptionType;
    }


    public static ErrorModel from(InputStream inputStream) {
        try {
            Gson gson = new Gson();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String body = br.lines().collect(Collectors.joining("\n"));
                return gson.fromJson(body, ErrorModel.class);
            }
        } catch (Exception e) {
            return new ErrorModel("Cannot create error model", InternalError.class.getSimpleName());
        }
    }
}