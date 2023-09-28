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

package io.hotmoka.crypto.api;

import java.security.InvalidKeyException;
import java.security.SignatureException;

/**
 * An object that verifies the signature of a value.
 *
 * @param <T> the type of values that get verified
 */
public interface Verifier<T> {

	/**
	 * Verifies that the given signature corresponds to the given value.
	 * 
	 * @param what the value whose signature gets verified
	 * @param signature the signature to verify
	 * @return true if and only if the signature matches
	 * @throws InvalidKeyException if the public key used for verification is invalid
	 * @throws SignatureException if the value cannot be verified
	 */
	boolean verify(T what, byte[] signature) throws InvalidKeyException, SignatureException;
}