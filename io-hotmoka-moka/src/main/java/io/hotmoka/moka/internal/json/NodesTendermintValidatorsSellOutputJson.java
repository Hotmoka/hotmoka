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

import java.util.Optional;

import io.hotmoka.moka.api.nodes.tendermint.validators.NodesTendermintValidatorsSellOutput;
import io.hotmoka.moka.internal.nodes.tendermint.validators.Sell;
import io.hotmoka.node.StorageValues;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka nodes tendermint validators sell} command.
 */
public abstract class NodesTendermintValidatorsSellOutputJson extends GasCostOutputJson implements JsonRepresentation<NodesTendermintValidatorsSellOutput> {

	private final StorageValues.Json offer;

	protected NodesTendermintValidatorsSellOutputJson(NodesTendermintValidatorsSellOutput output) {
		super(output);

		this.offer = output.getOffer().map(StorageValues.Json::new).orElse(null);
	}

	public Optional<StorageValues.Json> getOffer() {
		return Optional.ofNullable(offer);
	}

	@Override
	public NodesTendermintValidatorsSellOutput unmap() throws InconsistentJsonException {
		return new Sell.Output(this);
	}
}