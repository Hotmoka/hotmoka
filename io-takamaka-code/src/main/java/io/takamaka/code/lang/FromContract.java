/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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