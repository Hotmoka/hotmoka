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
import io.hotmoka.whitelisting.internal.checks.HasDeterministicTerminatingEqualsAndHashCodeCheck;

/**
 * States that an argument of a method or constructor of a white-listed
 * method has an {@code equals()} and a {@code hashCode} implementation
 * that is deterministic and terminating. It performs both checks of
 * {@link io.hotmoka.whitelisting.HasDeterministicTerminatingEquals} and
 * {@link io.hotmoka.whitelisting.HasDeterministicTerminatingHashCode}.
 * This annotation can also be applied
 * to a method, in which case it refers to the receiver of the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.PARAMETER, ElementType.METHOD })
@Inherited
@Documented
@WhiteListingProofObligation(check = HasDeterministicTerminatingEqualsAndHashCodeCheck.class)
public @interface HasDeterministicTerminatingEqualsAndHashCode {
}