/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.remote.internal.http.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;

/**
 * A Rest client class with a custom error handler
 */
public class RestClientService {

	private final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    /**
     * Performs a GET request and yields an entity T as response.
     * 
     * @param url the url
     * @param type the response class type
     * @param <T> the entity response type
     * @return the response
     * @throws NetworkExceptionResponse if client or server errors occur
     */
    public <T> T get(String url, Class<T> type) throws NetworkExceptionResponse {
    	HttpURLConnection con = null;

    	try {
    		con = (HttpURLConnection) new URL(url).openConnection();
	    	con.setRequestMethod("GET");

	    	if (con.getResponseCode() > 299)
	    		throw new NetworkExceptionResponse
	    			("Internal Server Error", gson.fromJson(readFromStream(con.getErrorStream()), ErrorModel.class));

	    	return gson.fromJson(readFromStream(con.getInputStream()), type);
		}
		catch (IOException e) {
			throw new NetworkExceptionResponse("Internal Server Error", errorModelFrom(con.getErrorStream()));
		}
    	finally {
    		if (con != null)
    			con.disconnect();
    	}
    }

    private static String readFromStream(InputStream stream) throws UnsupportedEncodingException, IOException {
    	try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
    		return br.lines().collect(Collectors.joining());
    	}
    }

    /**
     * Performs a POST request and yields an entity T as response.
     * 
     * @param url the url
     * @param requestBody the request body
     * @param type the response class type
     * @param <T> the entity response type
     * @param <R> the entity request type
     * @return the response
     * @throws NetworkExceptionResponse if client or server errors occur
     */
    public <T, R> T post(String url, R requestBody, Class<T> type) throws NetworkExceptionResponse {
    	HttpURLConnection con = null;

    	try {
    		con = (HttpURLConnection) new URL(url).openConnection();
    		con.setRequestMethod("POST");
	    	con.setRequestProperty("Content-Type", "application/json; utf-8");
	    	con.setRequestProperty("Accept", "application/json");
	    	con.setDoOutput(true);

	    	String body = gson.toJson(requestBody);
	    	try(OutputStream os = con.getOutputStream()) {
	    	    byte[] input = body.getBytes("utf-8");
	    	    os.write(input, 0, input.length);			
	    	}

	    	if (con.getResponseCode() > 299)
	    		throw new NetworkExceptionResponse("Internal Server Error", errorModelFrom(con.getErrorStream()));

	    	return gson.fromJson(readFromStream(con.getInputStream()), type);
		}
		catch (IOException e) {
			throw new NetworkExceptionResponse("Internal Server Error", new ErrorModel(e));
		}
    	finally {
    		if (con != null)
    			con.disconnect();
    	}
    }

    /**
     * Builds this model from an input stream
     * 
     * @param inputStream the input stream
     * @return an instance of this model
     */
    private ErrorModel errorModelFrom(InputStream inputStream) {
    	try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
    		String body = br.lines().collect(Collectors.joining("\n"));
    		return gson.fromJson(body, ErrorModel.class);
    	}
        catch (RuntimeException | IOException e) {
            return new ErrorModel("Cannot create the error model", RuntimeException.class);
        }
    }
}