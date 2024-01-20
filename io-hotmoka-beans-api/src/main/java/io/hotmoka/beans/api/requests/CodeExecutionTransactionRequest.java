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

package io.hotmoka.beans.api.requests;

import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.api.signatures.CodeSignature;
import io.hotmoka.beans.api.values.StorageValue;

/**
 * A request for executing a constructor or a method.
 * 
 * @param <R> the type of the corresponding response
 */
@Immutable
public interface CodeExecutionTransactionRequest<R extends CodeExecutionTransactionResponse> extends NonInitialTransactionRequest<R> {

	/**
	 * Yields the actual arguments passed to the method.
	 * 
	 * @return the actual arguments
	 */
	Stream<StorageValue> actuals();

	/**
	 * Yields the method or constructor referenced in this request.
	 * 
	 * @return the method or constructor
	 */
	CodeSignature getStaticTarget();

	/**
	 * Marshals this object into a byte array, without taking its signature into account.
	 * 
	 * @return the byte array resulting from marshalling this object
	 */
	byte[] toByteArrayWithoutSignature();
}