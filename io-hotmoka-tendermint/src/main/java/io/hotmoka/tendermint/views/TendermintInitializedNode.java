package io.hotmoka.tendermint.views;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.function.IntFunction;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.internal.TendermintInitializedNodeImpl;

/**
 * A node where the jar with the basic Takamaka classes have been installed,
 * along with a gamete and a manifest. It is a view of a Tendermint node, hence it
 * uses the chain identifier and the validator set of the underlying Tendermint network.
 * Namely, compared to the {@link #io.hotmoka.nodes.views.InitializedNode} interface, this
 * interface feeds the initialized node with the chain identifier and the
 * validators set of the underlying Tendermint network.
 */
@ThreadSafe
public interface TendermintInitializedNode extends InitializedNode {

	/**
	 * The keys of the {@code num}th initial validator. These are the (public and private) keys
	 * used for controlling the externally owned accounts associated to each Tendermint
	 * validator declared in the configuration file of Tendermint. These accounts receive
	 * rewards each time a block is generated. Note that
	 * these keys are in general distinct from the keys of the Tendermint validators themselves.
	 * 
	 * @return the keys
	 */
	KeyPair keysOfValidator(int num);

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Generates new keys to control the gamete. Uses the chain id and the validators
	 * of the underlying Tendermint network.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfValidators the keys to use for the Takamaka accounts that will be created for
	 *                         each Tendermint validator and stored in the manifest of the network
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param manifestClassName the name of the class of the manifest set for the node
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static TendermintInitializedNode of(TendermintBlockchain parent, IntFunction<KeyPair> keysOfValidators, Path takamakaCode, String manifestClassName, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new TendermintInitializedNodeImpl(parent, keysOfValidators, takamakaCode, manifestClassName, greenAmount, redAmount);
	}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id and the validators
	 * of the underlying Tendermint network.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfGamete the keys that must be used to control the gamete
	 * @param keysOfValidators the keys to use for the Takamaka accounts that will be created for
	 *                         each Tendermint validator and stored in the manifest of the network
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param manifestClassName the name of the class of the manifest set for the node
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
	 * @return a decorated view of {@code parent}
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static TendermintInitializedNode of(TendermintBlockchain parent, KeyPair keysOfGamete, IntFunction<KeyPair> keysOfValidators, Path takamakaCode, String manifestClassName, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new TendermintInitializedNodeImpl(parent, keysOfGamete, keysOfValidators, takamakaCode, manifestClassName, greenAmount, redAmount);
	}
}