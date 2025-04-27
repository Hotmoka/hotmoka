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

package io.hotmoka.node.internal.nodes;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.AbstractConsensusConfigBuilder;
import io.hotmoka.node.internal.json.ConsensusConfigJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;


/**
 * A builder of a basic consensus configurations, according to the builder pattern.
 */
public class BasicConsensusConfigBuilder extends AbstractConsensusConfigBuilder<BasicConsensusConfig, BasicConsensusConfigBuilder> {

	public BasicConsensusConfigBuilder() throws NoSuchAlgorithmException {
	}

	/**
	 * Creates a consensus configuration builder from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public BasicConsensusConfigBuilder(ConsensusConfigJson json) throws NoSuchAlgorithmException, InconsistentJsonException {
		try {
			var signature = SignatureAlgorithms.of(json.getSignatureForRequests());

			setGenesisTime(LocalDateTime.parse(json.getGenesisTime(), ISO_LOCAL_DATE_TIME))
			.setChainId(json.getChainId())
			.setMaxErrorLength(json.getMaxErrorLength())
			.setMaxDependencies(json.getMaxDependencies())
			.setMaxCumulativeSizeOfDependencies(json.getMaxCumulativeSizeOfDependencies())
			.allowUnsignedFaucet(json.isAllowsUnsignedFaucet())
			.skipVerification(json.isSkipsVerification())
			.setPublicKeyOfGamete(signature.publicKeyFromEncoding(Base64.fromBase64String(json.getPublicKeyOfGameteBase64(), InconsistentJsonException::new)))
			.setInitialGasPrice(json.getInitialGasPrice())
			.setMaxGasPerTransaction(json.getMaxGasPerTransaction())
			.ignoreGasPrice(json.isIgnoresGasPrice())
			.setTargetGasAtReward(json.getTargetGasAtReward())
			.setOblivion(json.getOblivion())
			.setInitialInflation(json.getInitialInflation())
			.setVerificationVersion(json.getVerificationVersion())
			.setInitialSupply(json.getInitialSupply())
			.setFinalSupply(json.getFinalSupply())
			.setTicketForNewPoll(json.getTicketForNewPoll())
			.setSignatureForRequests(signature);
		}
		catch (NullPointerException | IllegalArgumentException | InvalidKeyException | InvalidKeySpecException | DateTimeParseException e) {
			throw new InconsistentJsonException(e);
		}
	}

	public BasicConsensusConfigBuilder(SignatureAlgorithm signatureForRequests) {
		super(signatureForRequests);
	}

	public BasicConsensusConfigBuilder(Path path) throws NoSuchAlgorithmException, FileNotFoundException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		super(readToml(path));
	}

	public BasicConsensusConfigBuilder(BasicConsensusConfig config) {
		super(config);
	}

	@Override
	public BasicConsensusConfig build() {
		return new BasicConsensusConfig(this);
	}

	@Override
	protected BasicConsensusConfigBuilder getThis() {
		return this;
	}
}