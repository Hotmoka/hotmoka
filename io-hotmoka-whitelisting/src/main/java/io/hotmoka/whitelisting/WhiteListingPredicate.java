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

package io.hotmoka.whitelisting;

import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * A predicate for a value, that must satisfy some property if used
 * in a white-listed method or constructor.
 */
public interface WhiteListingPredicate {

	/**
	 * Checks if the given value satisfies the condition expressed by this predicate.
	 * 
	 * @param value the value to check
	 * @param wizard the object that can be used to access white-listing annotations about the library
	 * @return true if and only if {@code value} satisfies the conditions expressed by this predicate
	 */
	boolean test(Object value, WhiteListingWizard wizard);

	/**
	 * Yields the message to report if the predicate fails.
	 * 
	 * @param methodName the name of the method or constructor whose parameter is checked
	 * @return the message
	 */
	String messageIfFailed(String methodName);
}