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

	/**
	 * Yields the name of the signature algorithm for signing requests.
	 * 
	 * @return the name of the signature algorithm
	 */
	public String getSignatureForRequests() {
		return signatureForRequests;
	}

	/**
	 * Yields the amount of coins to pay to start a new poll amount the voters,
	 * for instance in order to change a consensus parameter.
	 * 
	 * @return the amount of coins to pay to start a new poll
	 */
	public BigInteger getTicketForNewPoll() {
		return ticketForNewPoll;
	}

	/**
	 * Yields the final supply of coins in the node. Once the current supply reaches
	 * this final amount, it remains constant.
	 * 
	 * @return the final supply of coins in the node
	 */
	public BigInteger getFinalSupply() {
		return finalSupply;
	}

	/**
	 * Yields the height from which coins are not minted anymore.
	 * That is exactly the moment when the final supply gets reached.
	 * From there, validators only earn coins from the gas consumed by the committed transactions.
	 * 
	 * @return the height from which coins are not minted anymore
	 */
	public BigInteger getHeightAtFinalSupply() {
		return heightAtFinalSupply;
	}

	/**
	 * Yields the initial supply of coins in the node.
	 * 
	 * @return the initial supply of coins in the node
	 */
	public BigInteger getInitialSupply() {
		return initialSupply;
	}

	/**
	 * Yields the version of the verification module to use.
	 * 
	 * @return the version of the verification module to use
	 */
	public long getVerificationVersion() {
		return verificationVersion;
	}

	/**
	 * Yields how quickly the gas consumed at previous rewards is forgotten:
	 * 0 means never, 1_000_000 means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 * A value of 0 means that the gas price is constant.
	 * 
	 * @return how quickly the gas consumed at previous rewards is forgotten:
	 *         0 means never, 1_000_000 means immediately
	 */
	public long getOblivion() {
		return oblivion;
	}

	/**
	 * Yields the units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 * 
	 * @return the units of gas that are aimed to be rewarded at each reward
	 */
	public BigInteger getTargetGasAtReward() {
		return targetGasAtReward;
	}

	/**
	 * Yields true if and only if the node ignores the minimum gas price.
	 * Hence requests that specify a lower gas price
	 * than the current gas price of the node are executed anyway.
	 * This is mainly useful for testing.
	 * 
	 * @return true if and only if the gas price must be ignored
	 */
	public boolean ignoresGasPrice() {
		return ignoresGasPrice;
	}

	/**
	 * Yields the maximal amount of gas that a non-view transaction can consume.
	 * 
	 * @return the maximal amount of gas that a non-view transaction can consume
	 */
	public BigInteger getMaxGasPerTransaction() {
		return maxGasPerTransaction;
	}

	/**
	 * Yields the initial gas price.
	 * 
	 * @return the initial gas price
	 */
	public BigInteger getInitialGasPrice() {
		return initialGasPrice;
	}

	/**
	 * Yields the public key of the gamete account.
	 * 
	 * @return the public key, base64-encoded
	 */
	public String getPublicKeyOfGameteBase64() {
		return publicKeyOfGameteBase64;
	}

	/**
	 * Yields true if and only if the static verification of the classes of the jars installed in the node must be skipped.
	 * 
	 * @return true if and only if the condition holds
	 */
	public boolean skipsVerification() {
		return skipsVerification;
	}

	/**
	 * Yields true if and only if the use of the faucet of the gamete is allowed without a valid signature.
	 * 
	 * @return true if and only if the condition holds
	 */
	public boolean allowsUnsignedFaucet() {
		return allowsUnsignedFaucet;
	}

	/**
	 * Yields the maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * of a transaction.
	 * 
	 * @return the maximal cumulative size (in bytes)
	 */
	public long getMaxCumulativeSizeOfDependencies() {
		return maxCumulativeSizeOfDependencies;
	}

	/**
	 * Yields the genesis time, UTC.
	 * 
	 * @return the genesis time
	 */
	public String getGenesisTime() {
		return genesisTime;
	}

	/**
	 * Yields the chain identifier of the node.
	 * 
	 * @return the chain identifier
	 */
	public String getChainId() {
		return chainId;
	}

	/**
	 * Yields the maximal number of dependencies in the classpath of a transaction.
	 * 
	 * @return the maximal number of dependencies
	 */
	public int getMaxDependencies() {
		return maxDependencies;
	}

	/**
	 * Yields the maximum size of a request; larger requests will be rejected.
	 * 
	 * @return the maximum size of a request
	 */
	public long getMaxRequestSize() {
		return maxRequestSize;
	}

	@Override
	public ConsensusConfig<?,?> unmap() throws NoSuchAlgorithmException, InconsistentJsonException {
		return new BasicConsensusConfigBuilder(this).build();
	}
}