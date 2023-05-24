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

/**
 * An exception thrown when the verification version of the node
 * is not supported by its verification module.
 */
public class UnsupportedVerificationVersionException extends Exception {
	private static final long serialVersionUID = -1232455923178336022L;

	/**
	 * The unsupported verification version.
	 */
	public final int verificationVerification;

	/**
	 * Creates the exception.
	 * 
	 * @param verificationVerification the unsupported verification version
	 */
	public UnsupportedVerificationVersionException(int verificationVerification) {
		super("the verification module does not support version " + verificationVerification);

		this.verificationVerification = verificationVerification;
	}
}