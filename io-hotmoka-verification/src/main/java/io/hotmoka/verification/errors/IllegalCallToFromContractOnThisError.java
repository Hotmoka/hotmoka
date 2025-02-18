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

package io.hotmoka.verification.errors;

import io.hotmoka.verification.internal.AbstractErrorImpl;

/**
 * An error issued when a {@code @@FromContract} method or constructor is called
 * on {@code this} from something that is not {@code @@FromContract} itself.
 */
public class IllegalCallToFromContractOnThisError extends AbstractErrorImpl {

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param methodName the name of the method where the error occurs
	 * @param fromContractMethodName the name of the {@code @@FromContract} method
	 * @param line the program line where the error occurs
	 */
	public IllegalCallToFromContractOnThisError(String where, String methodName, String fromContractMethodName, int line) {
		super(where, methodName, line, "\"" + fromContractMethodName + "\" is @FromContract and called on \"this\", hence it can only be called from a @FromContract method or constructor");
	}
}