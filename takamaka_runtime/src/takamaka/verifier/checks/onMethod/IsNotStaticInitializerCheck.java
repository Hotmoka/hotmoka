package takamaka.verifier.checks.onMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.apache.bcel.Const;

import takamaka.translator.IncompleteClasspathError;
import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalStaticInitializationError;

/**
 * A check the method is not the static class initializer.
 */
public class IsNotStaticInitializerCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public IsNotStaticInitializerCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		if (method.getCode() != null && method.getName().equals(Const.STATIC_INITIALIZER_NAME))
			if (clazz.isEnum() || clazz.isSynthetic()) {
				// checks that the static fields of enum's or synthetic classes with a static initializer
				// are either synthetic or enum elements or final static fields with
				// an explicit constant initializer. This check is necessary since we cannot forbid static initializers
				// in such classes, hence we do at least avoid the existence of extra static fields
				IncompleteClasspathError.insteadOfClassNotFoundException(() -> {
					Stream.of(classLoader.loadClass(className).getDeclaredFields())
						.filter(field -> Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && !field.isEnumConstant()
							&& !(Modifier.isFinal(field.getModifiers()) && hasExplicitConstantValue(field)))
						.findAny()
						.ifPresent(field -> issue(new IllegalStaticInitializationError(clazz, method.getName(), lineOf(instructions().findFirst().get()))));
				});
			}
			else
				issue(new IllegalStaticInitializationError(clazz, method.getName(), lineOf(instructions().findFirst().get())));
	}

	private boolean hasExplicitConstantValue(Field field) {
		return Stream.of(clazz.getFields())
			.filter(f -> f.isStatic() && f.getName().equals(field.getName()) && classLoader.bcelToClass(f.getType()) == field.getType())
			.allMatch(f -> f.getConstantValue() != null);
	}
}