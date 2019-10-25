package io.takamaka.whitelisting;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that a method or constructor can be called from user code.
 * This is significant for code that is not in the jars used as
 * class path for the transaction that runs the method or constructor
 * and for the {@code takamaka.*} hierarchy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ FIELD, METHOD, CONSTRUCTOR })
@Inherited
@Documented
public @interface WhiteListed {
}