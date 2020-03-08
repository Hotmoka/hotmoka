package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class is immutable if its fields are final and have immutable or primitive type.
 * An immutable class can only have immutable subclasses.
 */
@Retention(RetentionPolicy.CLASS)
@Target(value={ TYPE })
@Inherited
@Documented
public @interface Immutable {
}