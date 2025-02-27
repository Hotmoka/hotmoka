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

package io.hotmoka.verification;

import java.util.stream.Stream;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.internal.TakamakaClassLoaderImpl;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * A provider of class loaders used to access the definition of the classes of a Takamaka program.
 */
public final class TakamakaClassLoaders {

	private TakamakaClassLoaders() {}

	/**
	 * Yields a class loader with the given jars, given as byte arrays.
	 * 
	 * @param jars the jars
	 * @param verificationVersion the version of the verification module that must be used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 * @return the class loader
	 * @throws UnsupportedVerificationVersionException if the annotations for the required verification
	 *                                                 version cannot be found in the white-listing database
	 * @throws IllegalJarException if some jar is illegal, for instance, their class path does not include the Takamaka runtime
	 */
	public static TakamakaClassLoader of(Stream<byte[]> jars, long verificationVersion) throws UnsupportedVerificationVersionException, IllegalJarException {
		return new TakamakaClassLoaderImpl(jars, verificationVersion);
	}
}