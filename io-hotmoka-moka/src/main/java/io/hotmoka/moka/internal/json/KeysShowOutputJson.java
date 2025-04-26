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

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.moka.api.keys.KeysShowOutput;
import io.hotmoka.moka.internal.keys.Show;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka keys show} command.
 */
public abstract class KeysShowOutputJson implements JsonRepresentation<KeysShowOutput> {
	private final String signature;
	private final String publicKeyBase58;
	private final String publicKeyBase64;
	private final String tendermintAddress;
	private final String privateKeyBase58;
	private final String privateKeyBase64;
	private final String concatenatedBase64;

	protected KeysShowOutputJson(KeysShowOutput output) {
		this.signature = output.getSignature().getName();
		this.publicKeyBase58 = output.getPublicKeyBase58();
		this.publicKeyBase64 = output.getPublicKeyBase64();
		this.tendermintAddress = output.getTendermintAddress();
		this.privateKeyBase58 = output.getPrivateKeyBase58().orElse(null);
		this.privateKeyBase64 = output.getPrivateKeyBase64().orElse(null);
		this.concatenatedBase64 = output.getConcatenatedBase64().orElse(null);
	}

	public String getSignature() {
		return signature;
	}

	public String getPublicKeyBase58() {
		return publicKeyBase58;
	}

	public String getPublicKeyBase64() {
		return publicKeyBase64;
	}

	public String getTendermintAddress() {
		return tendermintAddress;
	}

	public Optional<String> getPrivateKeyBase58() {
		return Optional.ofNullable(privateKeyBase58);
	}

	public Optional<String> getPrivateKeyBase64() {
		return Optional.ofNullable(privateKeyBase64);
	}

	public Optional<String> getConcatenatedBase64() {
		return Optional.ofNullable(concatenatedBase64);
	}

	@Override
	public KeysShowOutput unmap() throws InconsistentJsonException, NoSuchAlgorithmException {
		return new Show.Output(this);
	}
}