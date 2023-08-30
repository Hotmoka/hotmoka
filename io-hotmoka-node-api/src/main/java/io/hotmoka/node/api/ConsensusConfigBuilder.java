package io.hotmoka.node.api;

import java.math.BigInteger;
import java.time.LocalDateTime;

import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * The builder of a configuration object.
 * 
 * @param <T> the concrete type of the builder
 */
public interface ConsensusConfigBuilder<T extends ConsensusConfigBuilder<T>> {

	/**
	 * Sets the genesis time, UTC.
	 * 
	 * @param genesisTime the genesis time, UTC
	 * @return this builder
	 */
	T setGenesisTime(LocalDateTime genesisTime);

	/**
	 * Sets the chain identifier of the node.
	 * 
	 * @param chainId the chain identifier
	 * @return this builder
	 */
	T setChainId(String chainId);

	/**
	 * Sets the maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 * 
	 * @param maxErrorLength the maximal length of the error message kept in the store of the node
	 * @return this builder
	 */
	T setMaxErrorLength(long maxErrorLength);

	/**
	 * Sets the maximal number of dependencies in the classpath of a transaction.
	 * 
	 * @param maxDependencies the maximal number of dependencies in the classpath of a transaction
	 * @return this builder
	 */
	T setMaxDependencies(long maxDependencies);

	/**
	 * Sets the maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * of a transaction.
	 * 
	 * @param maxCumulativeSizeOfDependencies the maximal cumulative size (in bytes) of the instrumented
	 *                                        jars of the dependencies of a transaction
	 * @return this builder
	 */
	T setMaxCumulativeSizeOfDependencies(long maxCumulativeSizeOfDependencies);

	/**
	 * Specifies to allow the {@code @@SelfCharged} annotation in the Takamaka
	 * code that runs in the node.
	 * 
	 * @param allowsSelfCharged true if and only if the annotation is allowed
	 * @return this builder
	 */
	T allowSelfCharged(boolean allowsSelfCharged);

	/**
	 * Specifies to allow the {@code faucet()} methods of the gametes without a valid signature.
	 * This is only useful for testing networks, where users can freely fill their accounts at the faucet.
	 * 
	 * @param allowsUnsignedFaucet true if and only if the faucet of the gametes can be used without a valid signature
	 * @return this builder
	 */
	T allowUnsignedFaucet(boolean allowsUnsignedFaucet);

	/**
	 * Specifies to allow the gamete of the node to call, for free, the add method of the accounts ledger
	 * and the mint/burn methods of the accounts, without paying gas and without paying for the minted coins.
	 * 
	 * @param allowsMintBurnFromGamete true if and only if the gamete is allowed
	 * @return this builder
	 */
	T allowMintBurnFromGamete(boolean allowsMintBurnFromGamete);

	/**
	 * Specifies to signature algorithm to use to sign the requests sent to the node.
	 * It defaults to ed25519;
	 * 
	 * @param signature the signature algorithm
	 * @return this builder
	 */
	T signRequestsWith(SignatureAlgorithm<SignedTransactionRequest> signature);

	/**
	 * Sets the initial gas price. It defaults to 100.
	 * 
	 * @param initialGasPrice the initial gas price to set.
	 * @return this builder
	 */
	T setInitialGasPrice(BigInteger initialGasPrice);

	/**
	 * Sets the maximal amount of gas that a non-view transaction can consume.
	 * It defaults to 1_000_000_000.
	 * 
	 * @param maxGasPerTransaction the maximal amount of gas to set
	 * @return this builder
	 */
	T setMaxGasPerTransaction(BigInteger maxGasPerTransaction);

	/**
	 * Sets the units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 * It defaults to 1_000_000.
	 * 
	 * @param targetGasAtReward the units of gas to set
	 * @return this builder
	 */
	T setTargetGasAtReward(BigInteger targetGasAtReward);

	/**
	 * Sets how quick the gas consumed at previous rewards is forgotten:
	 * 0 means never, 1_000_000 means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 * Use 0 to keep the gas price constant.
	 * It defaults to 250_000L.
	 * 
	 * @param oblivion the value to set
	 * @return this builder
	 */
	T setOblivion(long oblivion);

	/**
	 * Sets the initial inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 1,000,000 means 1%.
	 * Inflation can be negative. For instance, -300,000 means -0.3%.
	 * It defaults to 100,000 (that is, inflation is 0.1% by default).
	 * 
	 * @param initialInflation the initial inflation to set
	 * @return this builder
	 */
	T setInitialInflation(long initialInflation);

	/**
	 * Specifies that the minimum gas price for transactions is 0, so that the current
	 * gas price is not relevant for the execution of the transactions. It defaults to false.
	 * 
	 * @param ignoresGasPrice true if and only if the minimum gas price must be ignored
	 * @return this builder
	 */
	T ignoreGasPrice(boolean ignoresGasPrice);

	/**
	 * Requires to skip the verification of the classes of the jars installed in the node.
	 * It defaults to false.
	 * 
	 * @param skipsVerification true if and only if the verification must be disabled
	 * @return this builder
	 */
	T skipVerification(boolean skipsVerification);

	/**
	 * Sets the version of the verification module to use.
	 * It defaults to 0.
	 * 
	 * @param verificationVersion the version of the verification module
	 * @return this builder
	 */
	T setVerificationVersion(long verificationVersion);

	/**
	 * Sets the initial supply of coins of the node.
	 * It defaults to 0.
	 * 
	 * @param initialSupply the initial supply of coins of the node
	 * @return this builder
	 */
	T setInitialSupply(BigInteger initialSupply);

	/**
	 * Sets the initial supply of red coins of the node.
	 * It defaults to 0.
	 * 
	 * @param initialRedSupply the initial supply of red coins of the node
	 * @return this builder
	 */
	T setInitialRedSupply(BigInteger initialRedSupply);

	/**
	 * Sets the public key for the gamete account.
	 * It defaults to "" (hence a non-existent key).
	 * 
	 * @param publicKeyOfGamete the Base64-encoded public key of the gamete account
	 * @return this builder
	 */
	T setPublicKeyOfGamete(String publicKeyOfGamete);

	/**
	 * Sets the final supply of coins of the node.
	 * It defaults to 0.
	 * 
	 * @param finalSupply the final supply of coins of the node
	 * @return this builder
	 */
	T setFinalSupply(BigInteger finalSupply);

	/**
	 * Sets the amount of coins that must be payed to start a new poll amount
	 * to validators, for instance to change a consensus parameter.
	 * It defaults to 100.
	 * 
	 * @param ticketForNewPoll the amount of coins to set
	 * @return this builder
	 */
	T setTicketForNewPoll(BigInteger ticketForNewPoll);

	/**
	 * Builds the configuration.
	 * 
	 * @return the configuration
	 */
	ConsensusConfig build();
}