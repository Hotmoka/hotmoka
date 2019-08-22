package takamaka.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that an argument of a method or constructor of a white-listed
 * method must be false, for the method to be white-listed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER })
@Inherited
@Documented
@WhiteListingProofObligation
public @interface MustBeFalse {
}