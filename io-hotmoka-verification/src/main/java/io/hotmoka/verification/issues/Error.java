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

package io.hotmoka.verification.issues;

/**
 * A blocking issue: if an error occurs during the processing of a Takamaka jar file,
 * then its instrumentation cannot proceed and will be aborted.
 */
public abstract class Error extends Issue {

	protected Error(String where, String message) {
		super(where, message);
	}

	protected Error(String where, String fieldName, String message) {
		super(where, fieldName, message);
	}

	protected Error(String where, String methodName, int line, String message) {
		super(where, methodName, line, message);
	}
}