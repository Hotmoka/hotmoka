package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An instance method of a contract is self charged if the gas for its execution is payed
 * by the contract that defines the method. Nodes might decide to allow self charged methods
 * or forbid them altogether. Currently, only the Takamaka blockchain allows self charged methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ METHOD })
@Inherited
@Documented
public @interface SelfCharged {
}