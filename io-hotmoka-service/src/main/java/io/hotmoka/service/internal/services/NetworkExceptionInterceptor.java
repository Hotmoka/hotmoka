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

package io.hotmoka.service.internal.services;

import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Network exception interceptor to return a friendly error response to the client
 */
@RestControllerAdvice
public class NetworkExceptionInterceptor {
    private final static Logger LOGGER = Logger.getLogger(NetworkExceptionInterceptor.class.getName());

    @ExceptionHandler(value = NetworkExceptionResponse.class)
    public ResponseEntity<ErrorModel> handleNetworkException(NetworkExceptionResponse e) {
        return new ResponseEntity<>(e.errorModel, HttpStatus.valueOf(e.getStatus()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorModel> handleGenericException(Exception e) {
        LOGGER.log(Level.WARNING, "a generic error occurred", e);
       	return new ResponseEntity<>(new ErrorModel("Failed to process the request",  e.getClass()), HttpStatus.BAD_REQUEST);
    }
}
