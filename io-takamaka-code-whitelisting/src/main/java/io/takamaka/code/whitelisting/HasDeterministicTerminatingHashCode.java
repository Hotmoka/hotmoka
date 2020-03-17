package io.takamaka.code.whitelisting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.stream.Stream;

/**
 * States that an argument of a method or constructor of a white-listed
 * method has a {@code hashCode()} implementation that is deterministic and
 * terminating. It checks that the value of the argument
 * is a {@link java.lang.String} or a {@link java.math.BigInteger} or an enumeration
 * or an object
 * that redefines {@code hashCode()} in a Takamaka class in blockchain (hence not in the Java library).
 * This annotation can also be applied
 * to a method, in which case it refers to the receiver of the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER, ElementType.METHOD })
@Inherited
@Documented
@WhiteListingProofObligation(check = HasDeterministicTerminatingHashCode.Check.class)
public @interface HasDeterministicTerminatingHashCode {

	public class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value) {
			return value == null || value instanceof String || value instanceof BigInteger || value instanceof Enum<?>
				|| hashCodeIsInBlockchainCode(value.getClass());
		}

		private boolean hashCodeIsInBlockchainCode(Class<? extends Object> clazz) {
			return Stream.of(clazz.getMethods())
				.anyMatch(method -> !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())
					&& !Modifier.isStatic(method.getModifiers())
					&& method.getParameters().length == 0
					&& "hashCode".equals(method.getName())
					&& method.getReturnType() == int.class
					&& method.getDeclaringClass().getClassLoader() instanceof ResolvingClassLoader);
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "the actual parameter of " + methodName + " must be a String, a BigInteger or must redefine hashCode() in blockchain code";
		}
	}
}