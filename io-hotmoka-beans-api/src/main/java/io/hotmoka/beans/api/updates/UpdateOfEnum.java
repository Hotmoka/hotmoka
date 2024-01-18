/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans.api.updates;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.values.EnumValue;

/**
 * An update of a field of enumeration type.
 */
@Immutable
public interface UpdateOfEnum extends UpdateOfField {

	@Override
	EnumValue getValue();

	/**
	 * Yields the name of the enumeration class.
	 * 
	 * @return the name of the enumeration class
	 */
	String getEnumClassName();

	/**
	 * Yields the name of the enumeration element.
	 * 
	 * @return the name of the enumeration element
	 */
	String getName();
}