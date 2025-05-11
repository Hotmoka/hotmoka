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

package io.hotmoka.moka.internal.json;

/**
 * The JSON description of a Tendermint validator key.
 */
@SuppressWarnings("unused")
public class TendermintPrivValidatorJson {
	private final TypedKey pub_key;
	private final String address;
	private final TypedKey priv_key;

	/**
	 * Builds the JSON description of a Tendermint validator key.
	 * 
	 * @param publicKeyBase64 the Base64-encoded public key of the validator
	 * @param tendermintLikeAddress the Tendermint-like address of the validator
	 * @param concatenatedPrivatePublicKeyBase64 the Base64-encoded concatenation of the private and of the public key of the validator
	 */
	public TendermintPrivValidatorJson(String publicKeyBase64, String tendermintLikeAddress, String concatenatedPrivatePublicKeyBase64) {
		this.pub_key = new TypedKey("tendermint/PubKeyEd25519", publicKeyBase64);
		this.address = tendermintLikeAddress;
		this.priv_key = new TypedKey("tendermint/PrivKeyEd25519", concatenatedPrivatePublicKeyBase64);
	}

	private static class TypedKey {
		private final String type;
		private final String value;

		private TypedKey(String type, String value) {
			this.type = type;
			this.value = value;
		}
	}
}