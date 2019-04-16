package takamaka.lang;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that a method or constructor can be called from Takamaka code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ METHOD, CONSTRUCTOR })
@Inherited
@Documented
public @interface WhiteListed {
}