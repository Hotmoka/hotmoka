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

package io.hotmoka.node.internal.json;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.internal.nodes.BasicConsensusConfigBuilder;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@code Config}.
 */
public abstract class ConsensusConfigJson implements JsonRepresentation<ConsensusConfig<?,?>> {
	private final String genesisTime;
	private final String chainId;
	private final int maxDependencies;
	private final long maxCumulativeSizeOfDependencies;
	private final long maxRequestSize;
	private final boolean allowsUnsignedFaucet;
	private final boolean skipsVerification;
	private final String publicKeyOfGameteBase64;
	private final BigInteger initialGasPrice;
	private final BigInteger maxGasPerTransaction;
	private final boolean ignoresGasPrice;
	private final BigInteger targetGasAtReward;
	private final long oblivion;
	private final long verificationVersion;
	private final BigInteger initialSupply;
	private final BigInteger finalSupply;
	private final BigInteger heightAtFinalSupply;
	private final BigInteger ticketForNewPoll;
	private final String signatureForRequests;

	protected ConsensusConfigJson(ConsensusConfig<?,?> config) {
		this.genesisTime = ISO_LOCAL_DATE_TIME.format(config.getGenesisTime());
		this.chainId = config.getChainId();
		this.maxDependencies = config.getMaxDependencies();
		this.maxCumulativeSizeOfDependencies = config.getMaxCumulativeSizeOfDependencies();
		this.maxRequestSize = config.getMaxRequestSize();
		this.allowsUnsignedFaucet = config.allowsUnsignedFaucet();
		this.skipsVerification = config.skipsVerification();
		this.publicKeyOfGameteBase64 = config.getPublicKeyOfGameteBase64();
		this.initialGasPrice = config.getInitialGasPrice();
		this.maxGasPerTransaction = config.getMaxGasPerTransaction();
		this.ignoresGasPrice = config.ignoresGasPrice();
		this.targetGasAtReward = config.getTargetGasAtReward();
		this.oblivion = config.getOblivion();
		this.verificationVersion = config.getVerificationVersion();
		this.initialSupply = config.getInitialSupply();
		this.finalSupply = config.getFinalSupply();
		this.heightAtFinalSupply = config.getHeightAtFinalSupply();
		this.ticketForNewPoll = config.getTicketForNewPoll();
		this.signatureForRequests = config.getSignatureForRequests().getName();
	}

	public String getSignatureForRequests() {
		return signatureForRequests;
	}

	public BigInteger getTicketForNewPoll() {
		return ticketForNewPoll;
	}

	public BigInteger getFinalSupply() {
		return finalSupply;
	}

	public BigInteger getHeightAtFinalSupply() {
		return heightAtFinalSupply;
	}

	public BigInteger getInitialSupply() {
		return initialSupply;
	}

	public long getVerificationVersion() {
		return verificationVersion;
	}

	public long getOblivion() {
		return oblivion;
	}

	public BigInteger getTargetGasAtReward() {
		return targetGasAtReward;
	}

	public boolean isIgnoresGasPrice() {
		return ignoresGasPrice;
	}

	public BigInteger getMaxGasPerTransaction() {
		return maxGasPerTransaction;
	}

	public BigInteger getInitialGasPrice() {
		return initialGasPrice;
	}

	public String getPublicKeyOfGameteBase64() {
		return publicKeyOfGameteBase64;
	}

	public boolean isSkipsVerification() {
		return skipsVerification;
	}

	public boolean isAllowsUnsignedFaucet() {
		return allowsUnsignedFaucet;
	}

	public long getMaxCumulativeSizeOfDependencies() {
		return maxCumulativeSizeOfDependencies;
	}

	public String getGenesisTime() {
		return genesisTime;
	}

	public String getChainId() {
		return chainId;
	}

	public int getMaxDependencies() {
		return maxDependencies;
	}

	public long getMaxRequestSize() {
		return maxRequestSize;
	}

	@Override
	public ConsensusConfig<?,?> unmap() throws NoSuchAlgorithmException, InconsistentJsonException {
		return new BasicConsensusConfigBuilder(this).build();
	}
}