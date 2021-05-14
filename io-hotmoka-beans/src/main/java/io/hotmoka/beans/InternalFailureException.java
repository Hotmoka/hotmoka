/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.beans;

/**
 * An unexpected error, during the execution of some Hotmoka code.
 */
public class InternalFailureException extends RuntimeException {
	private static final long serialVersionUID = 3975906281624182199L;

	private InternalFailureException(Throwable t) {
		super(t.getMessage(), t);
	}

	private InternalFailureException(String message, Throwable t) {
		super(message + ": " + t.getMessage(), t);
	}

	public InternalFailureException(String message) {
		super(message);
	}

	public static InternalFailureException of(Throwable t) {
		if (t instanceof InternalFailureException)
			return (InternalFailureException) t;
		else
			return new InternalFailureException(t);
	}

	public static InternalFailureException of(String message, Throwable t) {
		if (t instanceof InternalFailureException)
			return (InternalFailureException) t;
		else
			return new InternalFailureException(message, t);
	}
}