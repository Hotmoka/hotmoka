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

package io.hotmoka.network.errors;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;

/**
 * The model of an exception thrown by a REST method.
 */
public class ErrorModel {

	/**
	 * The message of the exception.
	 */
	public String message;

	/**
	 * The fully-qualified name of the class of the exception.
	 */
	public String exceptionClassName;

	/**
	 * Builds the model of an exception thrown by a REST method.
	 * 
	 * @param message the message of the exception
	 * @param exceptionClass the class of the exception
	 */
    public ErrorModel(String message, Class<? extends Exception> exceptionClass) {
        this.message = message;
        this.exceptionClassName = abstractName(exceptionClass);
    }

    public ErrorModel() {}

    /**
     * Abstracts an exception class to the name of its superclass as declared in the methods
     * of the nodes. This avoids different results for nodes that throw subclasses
     * of the declared exceptions.
     * 
     * @param exceptionClass the class
     * @return the abstracted name of the exception class
     */
    private static String abstractName(Class<? extends Exception> exceptionClass) {
    	if (TransactionException.class.isAssignableFrom(exceptionClass))
    		return TransactionException.class.getName();
    	else if (TransactionRejectedException.class.isAssignableFrom(exceptionClass))
    		return TransactionRejectedException.class.getName();
    	else if (CodeExecutionException.class.isAssignableFrom(exceptionClass))
    		return CodeExecutionException.class.getName();
    	else if (NoSuchElementException.class.isAssignableFrom(exceptionClass))
    		return NoSuchElementException.class.getName();
    	else if (NoSuchAlgorithmException.class.isAssignableFrom(exceptionClass))
    		return NoSuchAlgorithmException.class.getName();
    	else if (InterruptedException.class.isAssignableFrom(exceptionClass))
    		return InterruptedException.class.getName();
    	else if (TimeoutException.class.isAssignableFrom(exceptionClass))
    		return TimeoutException.class.getName();
    	else
    		return exceptionClass.getName();
	}

	/**
     * Builds the model of an exception thrown by a REST method.
     *
     * @param e the exception
     */
    public ErrorModel(Exception e) {
        this(e.getMessage() != null ? e.getMessage() : "", e.getClass());
    }
}