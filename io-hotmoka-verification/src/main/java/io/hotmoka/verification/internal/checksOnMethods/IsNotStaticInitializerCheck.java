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

package io.hotmoka.verification.internal.checksOnMethods;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.ThrowIncompleteClasspathError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;
import io.hotmoka.verification.issues.IllegalStaticInitializationError;

/**
 * A check the method is not the static class initializer.
 */
public class IsNotStaticInitializerCheck extends CheckOnMethods {

	public IsNotStaticInitializerCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		if (method.getInstructionList() != null && Const.STATIC_INITIALIZER_NAME.equals(methodName))
			if (isEnum() || isSynthetic()) {
				// checks that the static fields of enum's or synthetic classes with a static initializer
				// are either synthetic or enum elements or final static fields with
				// an explicit constant initializer. This check is necessary since we cannot forbid static initializers
				// in such classes, hence we do at least avoid the existence of extra static fields
				ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> Stream.of(classLoader.loadClass(className).getDeclaredFields())
					.filter(field -> Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && !field.isEnumConstant()
						&& !(Modifier.isFinal(field.getModifiers()) && hasExplicitConstantValue(field)))
					.findAny()
					.ifPresent(field -> issue(new IllegalStaticInitializationError(inferSourceFile(), methodName, lineOf(instructions().findFirst().get())))));
			}
			else
				issue(new IllegalStaticInitializationError(inferSourceFile(), methodName, lineOf(instructions().findFirst().get())));
	}

	private boolean hasExplicitConstantValue(Field field) {
		return getFields()
			.filter(f -> f.isStatic() && f.getName().equals(field.getName()) && bcelToClass.of(f.getType()) == field.getType())
			.allMatch(f -> f.getConstantValue() != null);
	}
}