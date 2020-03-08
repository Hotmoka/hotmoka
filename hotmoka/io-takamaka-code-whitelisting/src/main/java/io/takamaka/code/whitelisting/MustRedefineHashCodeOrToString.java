package io.takamaka.code.whitelisting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * States that an argument of a method or constructor of a white-listed
 * method must be an object that redefines either {@code Object.hashCode()}
 * or {@code Object.toString()} (or both),
 * for the method to be white-listed. That is, a call to {@code Object.hashCode()}
 * on that object will not be resolved into {@code Object.hashCode()} itself
 * or the same must hold for {@code Object.toString()}.
 * This annotation can also be applied
 * to a method, in which case it refers to the receiver of the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER, ElementType.METHOD })
@Inherited
@Documented
@WhiteListingProofObligation(check = MustRedefineHashCodeOrToString.Check.class)
public @interface MustRedefineHashCodeOrToString {

	public class Check implements WhiteListingPredicate {

		@Override
		public boolean test(Object value) {
			return value == null ||
				Stream.of(value.getClass().getMethods())
					.filter(method -> !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class)
					.map(Method::getName)
					.anyMatch(name -> "hashCode".equals(name) || "toString".equals(name));
		}

		@Override
		public String messageIfFailed(String methodName) {
			return "the actual parameter of " + methodName + " must redefine Object.hashCode() or Object.toString()";
		}
	}
}