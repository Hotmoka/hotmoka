package io.takamaka.code.whitelisting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.takamaka.code.constants.Constants;

/**
 * States that an argument of a method or constructor of a white-listed
 * method has a {@code toString()} implementation that is deterministic and
 * terminating. It checks that the value of the argument
 * can be held in storage, hence an extension
 * of {@link io.takamaka.code.lang.Storage} or {@link java.lang.String}
 * or {@link java.math.BigInteger}, or is an object that redefines {@code toString)}
 * or {@code hashCode()} in a Takamaka class in blockchain (hence not in the Java library).
 * This annotation can also be applied
 * to a method, in which case it refers to the receiver of the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER, ElementType.METHOD })
@Inherited
@Documented
@WhiteListingProofObligation(check = HasDeterministicTerminatingToString.Check.class)
public @interface HasDeterministicTerminatingToString {

	public class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value) {
			return value == null || value instanceof String || value instanceof BigInteger || value instanceof Enum<?>
				|| isStorage(value.getClass()) || toStringIsInBlockchainCode(value.getClass()) || toStringIsInObjectAndHashCodeIsInBlockchainCode(value.getClass());
		}

		private boolean toStringIsInObjectAndHashCodeIsInBlockchainCode(Class<? extends Object> clazz) {
			try {
				Method toString = clazz.getMethod("toString");
				if (toString.getDeclaringClass() != Object.class)
					return false;
			}
			catch (Exception e) {
				return false;
			}

			return Stream.of(clazz.getMethods())
				.anyMatch(method -> !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())
					&& !Modifier.isStatic(method.getModifiers())
					&& method.getParameters().length == 0
					&& "hashCode".equals(method.getName())
					&& method.getReturnType() == int.class
					&& method.getDeclaringClass().getClassLoader() instanceof ResolvingClassLoader);
		}

		private boolean toStringIsInBlockchainCode(Class<? extends Object> clazz) {
			return Stream.of(clazz.getMethods())
				.anyMatch(method -> !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())
					&& !Modifier.isStatic(method.getModifiers())
					&& method.getParameters().length == 0
					&& "toString".equals(method.getName())
					&& method.getReturnType() == String.class
					&& method.getDeclaringClass().getClassLoader() instanceof ResolvingClassLoader);
		}

		private boolean isStorage(Class<? extends Object> clazz) {
			do {
				if (clazz.getName().equals(Constants.STORAGE_NAME))
					return true;

				clazz = clazz.getSuperclass();
			}
			while (clazz != null);

			return false;
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "the actual parameter of " + methodName + " must be a value that can be held in storage or redefine toString() or hashCode() in blockchain code";
		}
	}
}