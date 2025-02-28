/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.verification;

import io.hotmoka.verification.api.AnnotationUtility;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.verification.internal.AnnotationUtilityImpl;

/**
 * A provider of utilities for dealing with annotations.
 */
public final class AnnotationUtilities {

	private AnnotationUtilities() {}

	/**
	 * Yields a utility that deals with annotations.
	 *
	 * @param jar the jar for which the utility is used
	 * @return the utility
	 */
	public static AnnotationUtility of(VerifiedJar jar) {
		return new AnnotationUtilityImpl(jar);
	}
}