package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An entry is payable if the caller transfers funds to the contract
 * of the entry at call time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ METHOD, CONSTRUCTOR })
@Inherited
@Documented
public @interface Payable {
}