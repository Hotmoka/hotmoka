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

package io.hotmoka.verification;

import io.hotmoka.verification.api.BcelToClass;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.verification.internal.BcelToClassImpl;

/**
 * A provider of transformers of BCEL types into classes.
 */
public final class BcelToClasses {

	private BcelToClasses() {}

	/**
	 * Yields a utility that transforms a BCEL type into its corresponding class tag.
	 *
	 * @param jar the jar for which the transformation is performed
	 */
	public static BcelToClass of(VerifiedJar jar) {
		return new BcelToClassImpl(jar);
	}
}