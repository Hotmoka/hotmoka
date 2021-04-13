package io.takamaka.code.whitelisting;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
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

	class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value, WhiteListingWizard wizard) {
			return value == null || hashCodelsIsDeterministicAndTerminating(value.getClass(), wizard);
		}

		private static boolean hashCodelsIsDeterministicAndTerminating(Class<?> clazz, WhiteListingWizard wizard) {
			Optional<Method> hashCode = getHashCodeFor(clazz);
			return hashCode.isPresent() && isInWhiteListingDatabaseWithoutProofObligations(hashCode.get(), wizard);
		}

		private static boolean isInWhiteListingDatabaseWithoutProofObligations(Method method, WhiteListingWizard wizard) {
			try {
				Optional<Method> model = wizard.whiteListingModelOf(method);
				return model.isPresent() && hasNoProofObligations(model.get());
			}
			catch (ClassNotFoundException e) {
				return false;
			}
		}

		private static boolean hasNoProofObligations(Method model) {
			return Stream.concat(Stream.of(model.getAnnotations()), Stream.of(model.getParameterAnnotations()).flatMap(Stream::of))
					.map(Annotation::annotationType)
					.map(Class::getAnnotations)
					.flatMap(Stream::of)
					.noneMatch(annotation -> annotation instanceof WhiteListingProofObligation);
		}

		private static Optional<Method> getHashCodeFor(Class<?> clazz) {
			return Stream.of(clazz.getMethods())
				.filter(method -> !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())
					&& !Modifier.isStatic(method.getModifiers())
					&& method.getParameters().length == 0
					&& "hashCode".equals(method.getName())
					&& method.getReturnType() == int.class)
				.findFirst();
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "cannot prove that hashCode() on this object is deterministic and terminating";
		}
	}
}