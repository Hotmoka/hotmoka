package io.takamaka.code.verification.internal.checksOnClass;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.internal.CheckOnClasses;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalFieldNameError;
import io.takamaka.code.verification.issues.IllegalMethodNameError;

/**
 * A check that field and method names do not start with the
 * forbidden prefix that will later be used for synthetic instrumented members
 */
public class NamesDontStartWithForbiddenPrefix extends CheckOnClasses {

	public NamesDontStartWithForbiddenPrefix(VerifiedClassImpl.Verification builder) {
		super(builder);

		String forbiddedPrefixAsString = String.valueOf(VerifiedClass.FORBIDDEN_PREFIX);

		getFields().map(Field::getName)
			.filter(name -> name.startsWith(forbiddedPrefixAsString))
			.forEachOrdered(name -> issue(new IllegalFieldNameError(inferSourceFile(), name)));

		getMethods().map(MethodGen::getName)
			.filter(name -> name.startsWith(forbiddedPrefixAsString))
			.forEachOrdered(name -> issue(new IllegalMethodNameError(inferSourceFile(), name)));
	}
}