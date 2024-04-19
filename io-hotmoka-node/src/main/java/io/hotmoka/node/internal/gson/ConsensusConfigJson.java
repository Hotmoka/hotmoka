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

package io.hotmoka.node.internal.gson;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;

import io.hotmoka.beans.api.nodes.ConsensusConfig;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@code Config}.
 */
public abstract class ConsensusConfigJson implements JsonRepresentation<ConsensusConfig<?,?>> {
	private final String genesisTime;
	private final String chainId;
	private final long maxErrorLength;
	private final long maxDependencies;
	private final long maxCumulativeSizeOfDependencies;
	private final boolean allowsUnsignedFaucet;
	private final boolean skipsVerification;
	private final String publicKeyOfGameteBase64;
	private final BigInteger initialGasPrice;
	private final BigInteger maxGasPerTransaction;
	private final boolean ignoresGasPrice;
	private final BigInteger targetGasAtReward;
	private final long oblivion;
	private final long initialInflation;
	private final long verificationVersion;
	private final BigInteger initialSupply;
	private final BigInteger finalSupply;
	private final BigInteger initialRedSupply;
	private final BigInteger ticketForNewPoll;
	private final String signatureForRequests;

	protected ConsensusConfigJson(ConsensusConfig<?,?> config) {
		this.genesisTime = ISO_LOCAL_DATE_TIME.format(config.getGenesisTime());
		this.chainId = config.getChainId();
		this.maxErrorLength = config.getMaxErrorLength();
		this.maxDependencies = config.getMaxDependencies();
		this.maxCumulativeSizeOfDependencies = config.getMaxCumulativeSizeOfDependencies();
		this.allowsUnsignedFaucet = config.allowsUnsignedFaucet();
		this.skipsVerification = config.skipsVerification();
		this.publicKeyOfGameteBase64 = config.getPublicKeyOfGameteBase64();
		this.initialGasPrice = config.getInitialGasPrice();
		this.maxGasPerTransaction = config.getMaxGasPerTransaction();
		this.ignoresGasPrice = config.ignoresGasPrice();
		this.targetGasAtReward = config.getTargetGasAtReward();
		this.oblivion = config.getOblivion();
		this.initialInflation = config.getInitialInflation();
		this.verificationVersion = config.getVerificationVersion();
		this.initialSupply = config.getInitialSupply();
		this.finalSupply = config.getFinalSupply();
		this.initialRedSupply = config.getInitialRedSupply();
		this.ticketForNewPoll = config.getTicketForNewPoll();
		this.signatureForRequests = config.getSignatureForRequests().getName();
	}

	@Override
	public ConsensusConfig<?,?> unmap() throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		var signature = SignatureAlgorithms.of(signatureForRequests);

		return ConsensusConfigBuilders.defaults()
			.setGenesisTime(LocalDateTime.parse(genesisTime, ISO_LOCAL_DATE_TIME))
			.setChainId(chainId)
			.setMaxErrorLength(maxErrorLength)
			.setMaxDependencies(maxDependencies)
			.setMaxCumulativeSizeOfDependencies(maxCumulativeSizeOfDependencies)
			.allowUnsignedFaucet(allowsUnsignedFaucet)
			.skipVerification(skipsVerification)
			.setPublicKeyOfGamete(signature.publicKeyFromEncoding(Base64.fromBase64String(publicKeyOfGameteBase64)))
			.setInitialGasPrice(initialGasPrice)
			.setMaxGasPerTransaction(maxGasPerTransaction)
			.ignoreGasPrice(ignoresGasPrice)
			.setTargetGasAtReward(targetGasAtReward)
			.setOblivion(oblivion)
			.setInitialInflation(initialInflation)
			.setVerificationVersion(verificationVersion)
			.setInitialSupply(initialSupply)
			.setFinalSupply(finalSupply)
			.setInitialRedSupply(initialRedSupply)
			.setTicketForNewPoll(ticketForNewPoll)
			.setSignatureForRequests(signature)
			.build();
	}
}