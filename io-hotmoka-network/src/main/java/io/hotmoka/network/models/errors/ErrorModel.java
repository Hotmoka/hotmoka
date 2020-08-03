package io.hotmoka.network.models.errors;

import com.google.gson.Gson;

import io.hotmoka.beans.InternalFailureException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * The model of an exception thrown by a REST method.
 */
public class ErrorModel {

	/**
	 * The message of the exception.
	 */
	public final String message;

	/**
	 * The fully-qualified name of the class of the exception.
	 */
	public final String exceptionType;

	/**
	 * Builds the model of an exception thrown by a REST method.
	 * 
	 * @param message the message of the exception
	 * @param exceptionType the fully-qualified name of the class of the exception
	 */
    public ErrorModel(String message, String exceptionType) {
        this.message = message;
        this.exceptionType = exceptionType;
    }

    /**
     * Builds this model from an input stream
     * 
     * @param inputStream the input stream
     * @return an instance of this model
     */
    public static ErrorModel from(InputStream inputStream) {
        try {
            Gson gson = new Gson();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String body = br.lines().collect(Collectors.joining("\n"));
                return gson.fromJson(body, ErrorModel.class);
            }
        }
        catch (Exception e) {
            return new ErrorModel("Cannot create the error model", InternalFailureException.class.getName());
        }
    }
}