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

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.network.NetworkExceptionResponse;
import io.hotmoka.network.errors.ErrorModel;
import io.hotmoka.service.internal.Application;
import io.hotmoka.nodes.Node;

abstract class AbstractService {
    private final static Logger LOGGER = Logger.getLogger(AbstractService.class.getName());

    private @Autowired Application application;

    /**
     * Yields the Hotmoka node exposed by the Spring application of this service.
     * 
     * @return the Hotmoka node
     */
    protected final Node getNode() {
    	return application.getNode();
    }

    /**
     * Returns the result of a {@link java.util.concurrent.Callable} task,
     * wrapping its exceptions into a {@link NetworkExceptionResponse}.
	 *
     * @param task the task to call
     * @return the result of the task
     */
    protected static <T> T wrapExceptions(Callable<T> task) {
        try {
            return task.call();
        }
        catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
        	throw new NetworkExceptionResponse(HttpStatus.BAD_REQUEST.name(), new ErrorModel(e));
        }
        catch (Exception e) {
        	LOGGER.log(Level.WARNING, "error during network request", e);
        	throw new NetworkExceptionResponse(HttpStatus.BAD_REQUEST.name(), new ErrorModel(e));
        }
    }
}