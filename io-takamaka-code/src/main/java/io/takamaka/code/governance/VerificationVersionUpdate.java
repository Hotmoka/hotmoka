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

package io.takamaka.code.governance;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * An event issued when the version of the verification module has been updated.
 */
public class VerificationVersionUpdate extends ConsensusUpdate {

	/**
	 * The new verification version.
	 */
	public final int newVerificationVersion;

	/**
	 * Creates the event.
	 * 
	 * @param newVerificationVersion the new verification version
	 */
	@FromContract VerificationVersionUpdate(int newVerificationVersion) {
		super("the version of the verification module has been set to " + newVerificationVersion);

		this.newVerificationVersion = newVerificationVersion;
	}

	/**
	 * Yields the new verification version.
	 * 
	 * @return the new verification version
	 */
	public @View int getVerificationVersion() {
		return newVerificationVersion;
	}
}