/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.verification.internal.json;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.verification.api.VerificationError;
import io.hotmoka.verification.internal.AbstractVerificationError;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link VerificationError}.
 */
@Immutable
public class VerificationErrorJson implements JsonRepresentation<VerificationError> {
	private final String type;
	private final String where;
	private final String message;

	protected VerificationErrorJson(VerificationError error) {
		this.type = error.getClass().getSimpleName();
		this.where = error.getWhere();
		this.message = error.getMessage();
	}

	/**
	 * Yields the type of the error.
	 * 
	 * @return the type of the error
	 */
	public String getType() {
		return type;
	}

	/**
	 * Yields a description of where the error occurred.
	 * 
	 * @return a description of where the error occurred
	 */
	public String getWhere() {
		return where;
	}

	/**
	 * Yields the message of the error.
	 * 
	 * @return the message of the error
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public VerificationError unmap() throws InconsistentJsonException {
		return AbstractVerificationError.from(this);
	}
}