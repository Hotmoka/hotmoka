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

package io.hotmoka.verification.internal.checksOnClass;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.errors.IllegalFieldNameError;
import io.hotmoka.verification.errors.IllegalMethodNameError;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that field and method names do not start with the
 * forbidden prefix that will later be used for synthetic instrumented members
 */
public class NamesDontStartWithForbiddenPrefix extends CheckOnClasses {

	public NamesDontStartWithForbiddenPrefix(VerifiedClassImpl.Verification builder) throws IllegalJarException {
		super(builder);

		String forbiddenPrefixAsString = VerifiedClass.FORBIDDEN_PREFIX;

		getFields().map(Field::getName)
			.filter(name -> name.startsWith(forbiddenPrefixAsString))
			.forEachOrdered(name -> issue(new IllegalFieldNameError(inferSourceFile(), name)));

		getMethods().map(MethodGen::getName)
			.filter(name -> name.startsWith(forbiddenPrefixAsString))
			.forEachOrdered(name -> issue(new IllegalMethodNameError(inferSourceFile(), name)));
	}
}