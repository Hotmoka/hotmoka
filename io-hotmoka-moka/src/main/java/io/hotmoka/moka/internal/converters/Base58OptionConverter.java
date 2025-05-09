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

package io.hotmoka.moka.internal.converters;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import picocli.CommandLine.ITypeConverter;

/**
 * A converter of a string option that enforces that it is in Base58 format.
 */
public class Base58OptionConverter implements ITypeConverter<String> {

	@Override
	public String convert(String value) throws Base58ConversionException {
		return Base58.requireBase58(value, Base58ConversionException::new);
	}
}