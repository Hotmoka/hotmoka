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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import io.hotmoka.whitelisting.api.WhiteListingPredicate;
import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * A check that the class of a value is exactly one of the allowed collection classes from the library
 * (not a user subclass then).
 */
public class MustBeSafeLibraryCollectionCheck implements WhiteListingPredicate {

	@Override
	public boolean test(Object value, WhiteListingWizard wizard) {
		return value == null || isSafeLibraryCollection(value.getClass());
	}

	private static boolean isSafeLibraryCollection(Class<?> clazz) {
		return clazz == HashSet.class || clazz == LinkedList.class || clazz == ArrayList.class;
	}

	@Override
	public String messageIfFailed(String methodName) {
		return "cannot prove that this object is a safe library collection";
	}
}