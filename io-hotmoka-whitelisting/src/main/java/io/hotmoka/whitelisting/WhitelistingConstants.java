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

package io.hotmoka.whitelisting;

/**
 * Constants used in the white-listing algorithm.
 */
public abstract class WhitelistingConstants {

	/**
	 * The name of the class {@link #io.hotmoka.whitelisting.Dummy}.
	 */
	public final static String DUMMY_NAME = Dummy.class.getName();

	/**
	 * The name of the class {@code io.hotmoka.local.internal.runtime.Runtime}.
	 * This is treated specially in terms of white-listing, since its methods
	 * are allowed in instrumented code.
	 */
	public final static String RUNTIME_NAME = "io.hotmoka.local.internal.runtime.Runtime";
}