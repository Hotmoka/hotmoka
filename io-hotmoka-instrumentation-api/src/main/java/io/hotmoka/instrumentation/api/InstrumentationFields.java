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

package io.hotmoka.instrumentation.api;

import static io.hotmoka.verification.api.VerifiedClass.FORBIDDEN_PREFIX;

/**
 * Constants about fields used for instrumentation of Takamaka classes.
 */
public abstract class InstrumentationFields {

	private InstrumentationFields() {}

	/**
	 * The name of the instrumented field of storage objects that holds their storage reference.
	 * Since it is private, it does not need any forbidden character at its beginning.
	 */
	public final static String STORAGE_REFERENCE_FIELD_NAME = "storageReference";

	/**
	 * The name of the field used in instrumented storage classes
	 * to remember if the object is new or already serialized in blockchain.
	 * Since it is private, it does not need any forbidden character at its beginning.
	 */
	public final static String IN_STORAGE = "inStorage";

	/**
	 * The prefix of the name of the field used in instrumented storage classes
	 * to take note of the old value of the fields.
	 */
	public final static String OLD_PREFIX = FORBIDDEN_PREFIX + "old_";
}