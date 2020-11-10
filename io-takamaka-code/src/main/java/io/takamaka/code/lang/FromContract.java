package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation states that a method or constructor can only be called from
 * a contract. The latter will be available inside the method or constructor as its caller.
 * Hence, this annotation limits the possible contexts from where the method or constructor
 * is called. Note that a call from outside the node, for instance from a wallet,
 * comes from a contract, namely, from the externally owned account that pays for
 * the transaction and becomes the caller inside the annotated method or constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ METHOD, CONSTRUCTOR })
@Inherited
@Documented
public @interface FromContract {
	Class<? extends Contract> value() default Contract.class;
}