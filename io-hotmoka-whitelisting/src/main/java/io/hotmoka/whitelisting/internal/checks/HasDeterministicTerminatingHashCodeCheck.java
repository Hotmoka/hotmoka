/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.whitelisting.internal.checks;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.whitelisting.api.WhiteListingPredicate;
import io.hotmoka.whitelisting.api.WhiteListingProofObligation;
import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * A check that a value has a deterministic and terminating implementation of {@code hashCode()}.
 */
public class HasDeterministicTerminatingHashCodeCheck implements WhiteListingPredicate {

	@Override
	public boolean test(Object value, WhiteListingWizard wizard) {
		return value == null || hashCodelsIsDeterministicAndTerminating(value.getClass(), wizard);
	}

	private static boolean hashCodelsIsDeterministicAndTerminating(Class<?> clazz, WhiteListingWizard wizard) {
		Optional<Method> hashCode = getHashCodeFor(clazz);
		return hashCode.isPresent() && isInWhiteListingDatabaseWithoutProofObligations(hashCode.get(), wizard);
	}

	private static boolean isInWhiteListingDatabaseWithoutProofObligations(Method method, WhiteListingWizard wizard) {
		Optional<Method> model = wizard.whiteListingModelOf(method);
		return model.isPresent() && hasNoProofObligations(model.get());
	}

	private static boolean hasNoProofObligations(Method model) {
		return Stream.concat(Stream.of(model.getAnnotations()), Stream.of(model.getParameterAnnotations()).flatMap(Stream::of))
				.map(Annotation::annotationType)
				.map(Class::getAnnotations)
				.flatMap(Stream::of)
				.noneMatch(annotation -> annotation instanceof WhiteListingProofObligation);
	}

	private static Optional<Method> getHashCodeFor(Class<?> clazz) {
		return Stream.of(clazz.getMethods())
				.filter(method -> !Modifier.isAbstract(method.getModifiers())
						&& Modifier.isPublic(method.getModifiers())
						&& !Modifier.isStatic(method.getModifiers())
						&& method.getParameters().length == 0
						&& "hashCode".equals(method.getName())
						&& method.getReturnType() == int.class)
				.findFirst();
	}

	@Override
	public String messageIfFailed(String methodName) {
		return "cannot prove that hashCode() on this object is deterministic and terminating";
	}
}