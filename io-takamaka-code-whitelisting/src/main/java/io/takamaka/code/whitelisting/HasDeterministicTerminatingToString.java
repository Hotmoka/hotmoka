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
 * method has a {@code toString()} implementation that is deterministic and
 * terminating. It checks that the value of the argument
 * can be held in storage, hence is an extension
 * of {@link io.takamaka.code.lang.Storage} or {@link java.lang.String}
 * or {@link java.math.BigInteger} or an enumeration,
 * or is an object that redefines {@code toString)}
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

	class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value, WhiteListingWizard wizard) {
			return value == null || toStringlsIsDeterministicAndTerminating(value.getClass(), wizard);
		}

		private static boolean toStringlsIsDeterministicAndTerminating(Class<?> clazz, WhiteListingWizard wizard) {
			Optional<Method> toString = getToStringFor(clazz);
			return toString.isPresent() &&
					(isInWhiteListingDatabaseWithoutProofObligations(toString.get(), wizard)
					|| toStringIsInObjectAndHashCodeIsDeterministicAndTerminating(toString.get(), clazz, wizard));
		}

		private static boolean isInWhiteListingDatabaseWithoutProofObligations(Method method, WhiteListingWizard wizard) {
            Optional<Method> model = wizard.whiteListingModelOf(method);
            return model.isPresent() && hasNoProofObligations(model.get());
        }

		private static boolean hasNoProofObligations(Method model) {
			return Stream.concat(Stream.of(model.getAnnotations()), Stream.of(model.getParameterAnnotations()).flatMap(Stream::of))
					.map(Annotation::annotationType)
					.map(Class::getAnnotations)
					.flatMap(Stream::of)
					.noneMatch(annotation -> annotation instanceof WhiteListingProofObligation);
		}

		private static Optional<Method> getToStringFor(Class<?> clazz) {
			return Stream.of(clazz.getMethods())
				.filter(method -> !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())
					&& !Modifier.isStatic(method.getModifiers())
					&& method.getParameters().length == 0
					&& "toString".equals(method.getName())
					&& method.getReturnType() == String.class)
				.findFirst();
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

		private static boolean toStringIsInObjectAndHashCodeIsDeterministicAndTerminating(Method toString, Class<?> clazz, WhiteListingWizard wizard) {
			if (toString.getDeclaringClass() == Object.class) {
				Optional<Method> hashCode = getHashCodeFor(clazz);
				return hashCode.isPresent() && isInWhiteListingDatabaseWithoutProofObligations(hashCode.get(), wizard);
			}
			else
				return false;
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "cannot prove that toString() on this object is deterministic and terminating";
		}
	}
}