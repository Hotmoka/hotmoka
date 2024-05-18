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

import java.util.stream.Stream;

import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import io.hotmoka.whitelisting.api.WhiteListingClassLoader;
import io.hotmoka.whitelisting.internal.WhiteListingClassLoaderImpl;

/**
 * A provider of white-listing class loaders.
 */
public final class WhiteListingClassLoaders {

	private WhiteListingClassLoaders() {}

	/**
	 * Yields a resolving class loader that loads classes from the given jars, provided as byte arrays.
	 * 
	 * @param jars the jars, as byte arrays
	 * @param verificationVersion the version of the verification module that must be used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 * @return the class loader
	 * @throws UnsupportedVerificationVersionException if the annotations for the required verification
	 *                                                 version cannot be found in the database
	 */
	public static WhiteListingClassLoader of(Stream<byte[]> jars, long verificationVersion) throws UnsupportedVerificationVersionException {
		return new WhiteListingClassLoaderImpl(jars, verificationVersion);
	}
}