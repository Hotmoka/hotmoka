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

import java.util.stream.Stream;

import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintInitOutput;
import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintInitOutput.ValidatorDescription;
import io.hotmoka.moka.internal.nodes.tendermint.Init;
import io.hotmoka.moka.internal.nodes.tendermint.Init.ValidatorDescriptionImpl;
import io.hotmoka.node.StorageValues;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka nodes tendermint init} command.
 */
public abstract class NodesTendermintInitOutputJson extends NodeInitOutputJson implements JsonRepresentation<NodesTendermintInitOutput> {
	private final ValidatorDescriptionJson[] validators;

	protected NodesTendermintInitOutputJson(NodesTendermintInitOutput output) {
		super(output);

		this.validators = output.getValidators().map(ValidatorDescriptionJson::new).toArray(ValidatorDescriptionJson[]::new);
	}

	public Stream<ValidatorDescriptionJson> getValidators() {
		return Stream.of(validators);
	}

	@Override
	public NodesTendermintInitOutput unmap() throws InconsistentJsonException {
		return new Init.Output(this);
	}

	/**
	 * The JSON representation of a validator description.
	 */
	public static class ValidatorDescriptionJson implements JsonRepresentation<ValidatorDescription> {
		private final StorageValues.Json reference;
		private final String publicKeyBase58;

		private ValidatorDescriptionJson(ValidatorDescription validator) {
			this.reference = new StorageValues.Json(validator.getReference());
			this.publicKeyBase58 = validator.getPublicKeyBase58();
		}

		@Override
		public ValidatorDescription unmap() throws InconsistentJsonException {
			return new ValidatorDescriptionImpl(this);
		}

		public StorageValues.Json getReference() {
			return reference;
		}

		public String getPublicKeyBase58() {
			return publicKeyBase58;
		}
	}
}