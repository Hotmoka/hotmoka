package takamaka.lang;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An entry is a method or constructor that can only be called from
 * a distinct contract object. The latter will be available inside the
 * method or constructor as its caller.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ METHOD, CONSTRUCTOR })
@Inherited
@Documented
public @interface Entry {
	// the following definition would be better, but cannot be used since
	// the Contract class will be replaced, at run time, with its instrumented
	// version and we would run into an illegal annotation default
	//Class<? extends Contract> value() default Contract.class;
	Class<?> value() default Object.class;
}