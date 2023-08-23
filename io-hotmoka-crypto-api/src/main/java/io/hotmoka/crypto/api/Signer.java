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
 * An object that computes the signature of a value with a private key.
 *
 * @param <T> the type of values that get signed
 */
public interface Signer<T> {

	/**
	 * Computes the signature of the given value with the given key.
	 * 
	 * @param what the value to sign
	 * @return the signature of the value
	 * @throws InvalidKeyException if the private key used for signing is invalid
	 * @throws SignatureException if the value cannot be signed
	 */
	byte[] sign(T what) throws InvalidKeyException, SignatureException;
}