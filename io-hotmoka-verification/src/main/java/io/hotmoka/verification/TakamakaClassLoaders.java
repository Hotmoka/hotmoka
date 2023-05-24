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

import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.internal.TakamakaClassLoaderImpl;

/**
 * A provider of class loaders used to access the definition of the classes of a Takamaka program.
 */
public interface TakamakaClassLoaders {

	/**
	 * Yields a class loader with the given jars, given as byte arrays.
	 * 
	 * @param jars the jars
	 * @param verificationVersion the version of the verification module that must b e used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 */
	static TakamakaClassLoader of(Stream<byte[]> jars, int verificationVersion) throws ClassNotFoundException {
		return new TakamakaClassLoaderImpl(jars, verificationVersion);
	}
}