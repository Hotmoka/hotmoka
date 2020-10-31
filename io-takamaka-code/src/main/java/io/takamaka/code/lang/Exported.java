package io.takamaka.code.lang;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A storage class is exported if its instances can be passed as parameters of methods
 * or constructors from outside the node. If a class is exported, it follows that everybody
 * can run any public method on its instances, even through the instance is apparently
 * encapsulated, since its reference is actually public in the store. This can be problematic
 * if the instances are modifiable throuogh their public API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ TYPE })
@Inherited
@Documented
public @interface Exported {
}