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

package io.takamaka.code.whitelisting;

/**
 * An exception thrown when the white-listing annotations are missing
 * for the version of the verification module of the node.
 */
public class MissingWhiteListingAnnotationsError extends java.lang.Error {
	private static final long serialVersionUID = 563905276281900393L;

	public final int verificationVerification;

	public MissingWhiteListingAnnotationsError(int verificationVerification) {
		super("the white-listing annotations are missing for verification version " + verificationVerification);

		this.verificationVerification = verificationVerification;
	}
}