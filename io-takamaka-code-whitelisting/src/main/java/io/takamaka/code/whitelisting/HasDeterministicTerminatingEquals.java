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
 * method has an {@code equals()} implementation that is deterministic and
 * terminating. It checks that the value of the argument
 * is a {@link java.lang.String} or a {@link java.math.BigInteger} or an enumeration
 * or an object that redefines {@code equals()} in a Takamaka class in blockchain
 * (hence not in the Java library) or an object that uses {@link java.lang.Object#equals(Object)}.
 * This annotation can also be applied
 * to a method, in which case it refers to the receiver of the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER, ElementType.METHOD })
@Inherited
@Documented
@WhiteListingProofObligation(check = HasDeterministicTerminatingEquals.Check.class)
public @interface HasDeterministicTerminatingEquals {

	public class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value, WhiteListingWizard wizard) {
			return value == null || equalsIsDeterministicAndTerminating(value.getClass(), wizard);
		}

		private static boolean equalsIsDeterministicAndTerminating(Class<?> clazz, WhiteListingWizard wizard) {
			Optional<Method> equals = getEqualsFor(clazz);
			return equals.isPresent() &&
				// Object.equals() is deterministic and terminating, but its subclasses are not always so;
				// hence we cannot rely on the database for that special case, since proof-obligations are inherited
				(equals.get().getDeclaringClass() == Object.class || isInWhiteListingDatabaseWithoutProofObligations(equals.get(), wizard));
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

		private static Optional<Method> getEqualsFor(Class<?> clazz) {
			return Stream.of(clazz.getMethods())
				.filter(method -> !Modifier.isAbstract(method.getModifiers())
					&& Modifier.isPublic(method.getModifiers())
					&& !Modifier.isStatic(method.getModifiers())
					&& method.getParameters().length == 1
					&& method.getParameterTypes()[0] == Object.class
					&& "equals".equals(method.getName())
					&& method.getReturnType() == boolean.class)
				.findFirst();
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "cannot prove that equals() on this object is deterministic and terminating";
		}
	}
}