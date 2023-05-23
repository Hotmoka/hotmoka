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

package io.hotmoka.whitelisting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.hotmoka.whitelisting.api.WhiteListingProofObligation;
import io.hotmoka.whitelisting.internal.checks.HasDeterministicTerminatingToStringCheck;

/**
 * States that an argument of a method or constructor of a white-listed
 * method has a {@code toString()} implementation that is deterministic and
 * terminating. It checks that the value of the argument
 * can be held in storage, hence is an extension
 * of {@link io.takamaka.code.lang.Storage} or {@link java.lang.String}
 * or {@link java.math.BigInteger} or an enumeration,
 * or is an object that redefines {@code toString)}
 * or {@code hashCode()} in a Takamaka class in blockchain (hence not in the Java library).
 * This annotation can also be applied
 * to a method, in which case it refers to the receiver of the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER, ElementType.METHOD })
@Inherited
@Documented
@WhiteListingProofObligation(check = HasDeterministicTerminatingToStringCheck.class)
public @interface HasDeterministicTerminatingToString {
}