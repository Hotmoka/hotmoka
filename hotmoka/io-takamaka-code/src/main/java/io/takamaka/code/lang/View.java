package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that an entry does not modify the state of any storage object
 * reachable before its execution, nor yields a new storage object.
 * Hence the entry can be executed off-chain, without generating
 * a transaction.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ METHOD })
@Inherited
@Documented
public @interface View {
}