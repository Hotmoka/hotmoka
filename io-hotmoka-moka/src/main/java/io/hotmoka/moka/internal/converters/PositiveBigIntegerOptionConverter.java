/*
Copyright 2025 Fausto Spoto

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

import java.math.BigInteger;

import picocli.CommandLine.ITypeConverter;

/**
 * A converter of an option into a (strictly) positive BigInteger.
 */
public class PositiveBigIntegerOptionConverter implements ITypeConverter<BigInteger> {

	@Override
	public BigInteger convert(String value) throws IllegalArgumentException, NumberFormatException {
		var bi = new BigInteger(value);
		if (bi.signum() <= 0)
			throw new IllegalArgumentException("A strictly positive BigInteger was required");

		return bi;
	}
}