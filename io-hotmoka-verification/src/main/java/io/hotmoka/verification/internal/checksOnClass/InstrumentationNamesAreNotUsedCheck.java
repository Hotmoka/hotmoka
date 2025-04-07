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

import java.util.stream.Stream;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerifiedClass;
import io.hotmoka.verification.errors.IllegalFieldNameError;
import io.hotmoka.verification.errors.IllegalMethodNameError;
import io.hotmoka.verification.errors.IllegalUseOfDummyInFieldSignatureError;
import io.hotmoka.verification.errors.IllegalUseOfDummyInMethodSignatureError;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;
import io.hotmoka.whitelisting.WhitelistingConstants;

/**
 * A check that the names of the fields or methods added by instrumentation are
 * not used before the same instrumentation.
 */
public class InstrumentationNamesAreNotUsedCheck extends CheckOnClasses {

	public InstrumentationNamesAreNotUsedCheck(VerifiedClassImpl.Verification builder) throws IllegalJarException, UnknownTypeException {
		super(builder);

		var DUMMY_OT = new ObjectType(WhitelistingConstants.DUMMY_NAME);

		// most instrumented code starts with a forbidden prefix: we check that it is not
		// used to define non-instrumented methods, constructors or fields
		String forbiddenPrefixAsString = VerifiedClass.FORBIDDEN_PREFIX;

		getFields().map(Field::getName)
			.filter(name -> name.startsWith(forbiddenPrefixAsString))
			.forEachOrdered(name -> issue(new IllegalFieldNameError(inferSourceFile(), name)));

		getMethods().map(MethodGen::getName)
			.filter(name -> name.startsWith(forbiddenPrefixAsString))
			.forEachOrdered(name -> issue(new IllegalMethodNameError(inferSourceFile(), name)));

		// the deserialization constructor used a special Dummy type at the end of its signature:
		// we guarantee that that is not used in non-instrumented methods, constructors or fields
		getFields()
			.filter(field -> DUMMY_OT.equals(field.getType()))
			.forEachOrdered(field -> issue(new IllegalUseOfDummyInFieldSignatureError(inferSourceFile(), field.getName())));

		getMethods()
			.filter(this::signatureUsesDummy)
			.forEachOrdered(field -> issue(new IllegalUseOfDummyInMethodSignatureError(inferSourceFile(), field.getName())));

		// there is no need to check that the code does not use instrumentation fields, constructors or methods,
		// since the verification that such operations are white-listed is enough to conclude that they do not use
		// Dummy nor the forbidden prefix, because of the checks above
	}

	private boolean signatureUsesDummy(MethodGen method) {
		var DUMMY_OT = new ObjectType(WhitelistingConstants.DUMMY_NAME);

		return DUMMY_OT.equals(method.getReturnType()) || Stream.of(method.getArgumentTypes()).anyMatch(DUMMY_OT::equals);
	}
}