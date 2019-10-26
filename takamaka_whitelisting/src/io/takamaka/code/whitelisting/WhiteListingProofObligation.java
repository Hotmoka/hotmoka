package io.takamaka.code.whitelisting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that an annotation is meant to decorate an argument of a method
 * or constructor of a white-listed method and specifies some property that
 * the argument must satisfy, for the method to be white-listed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface WhiteListingProofObligation {
}