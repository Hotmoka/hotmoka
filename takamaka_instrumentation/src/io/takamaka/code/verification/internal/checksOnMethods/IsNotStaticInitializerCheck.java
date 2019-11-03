package io.takamaka.code.verification.internal.checksOnMethods;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.internal.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.issues.IllegalStaticInitializationError;

/**
 * A check the method is not the static class initializer.
 */
public class IsNotStaticInitializerCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public IsNotStaticInitializerCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.getInstructionList() != null && Const.STATIC_INITIALIZER_NAME.equals(methodName))
			if (clazz.isEnum() || clazz.isSynthetic()) {
				// checks that the static fields of enum's or synthetic classes with a static initializer
				// are either synthetic or enum elements or final static fields with
				// an explicit constant initializer. This check is necessary since we cannot forbid static initializers
				// in such classes, hence we do at least avoid the existence of extra static fields
				ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					Stream.of(classLoader.loadClass(className).getDeclaredFields())
						.filter(field -> Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && !field.isEnumConstant()
							&& !(Modifier.isFinal(field.getModifiers()) && hasExplicitConstantValue(field)))
						.findAny()
						.ifPresent(field -> issue(new IllegalStaticInitializationError(inferSourceFile(), methodName, lineOf(instructions().findFirst().get()))));
				});
			}
			else
				issue(new IllegalStaticInitializationError(inferSourceFile(), methodName, lineOf(instructions().findFirst().get())));
	}

	private boolean hasExplicitConstantValue(Field field) {
		return Stream.of(clazz.getFields())
			.filter(f -> f.isStatic() && f.getName().equals(field.getName()) && clazz.bcelToClass.of(f.getType()) == field.getType())
			.allMatch(f -> f.getConstantValue() != null);
	}
}