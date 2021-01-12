package io.hotmoka.nodes.views;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.internal.InitializedNodeImpl;

/**
 * A node where the jar with the basic Takamaka classes have been installed,
 * along with a gamete and a manifest.
 */
@ThreadSafe
public interface InitializedNode extends Node {

	/**
	 * The keys that have been generated for the gamete.
	 * They can be used for signing requests on behalf of the gamete.
	 * 
	 * @return the keys
	 */
	KeyPair keysOfGamete();

	/**
	 * Yields the gamete that has been created.
	 * 
	 * @return the gamete
	 */
	StorageReference gamete();

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Generates new keys to control the gamete. It uses a generic empty set of
	 * validators and a generic gas station.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
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
	static InitializedNode of(Node parent, ConsensusParams consensus, Path takamakaCode, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return of(parent, consensus, parent.getSignatureAlgorithmForRequests().getKeyPair(), takamakaCode, greenAmount, redAmount, null, null);
	}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. It uses a generic empty set of
	 * validators and a generic gas station.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param keysOfGamete the keys that must be used to control the gamete
	 * @param takamakaCode the jar containing the basic Takamaka classes
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
	static InitializedNode of(Node parent, ConsensusParams consensus, KeyPair keysOfGamete, Path takamakaCode, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return of(parent, consensus, keysOfGamete, takamakaCode, greenAmount, redAmount, null, null);
	}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete. It allows one to specify how
	 * the validators and the gas station of the node are being created.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param keysOfGamete the key pair that will be used to control the gamete
	 * @param producerOfValidatorsBuilder an algorithm that creates the builder of the validators to be installed in the manifest of the node;
	 *                                    if this is {@code null}, a generic empty validators set is created
	 * @param producerOfGasStation an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *                             if this is {@code null}, a generic gas station is created
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	static InitializedNode of(Node parent, ConsensusParams consensus, KeyPair keysOfGamete,
			Path takamakaCode, BigInteger greenAmount, BigInteger redAmount, ProducerOfStorageObject producerOfValidatorsBuilder, ProducerOfStorageObject producerOfGasStation) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new InitializedNodeImpl(parent, consensus, keysOfGamete, takamakaCode, greenAmount, redAmount, producerOfValidatorsBuilder, producerOfGasStation);
	}

	/**
	 * An algorithm that yields an object in the store of a node, given
	 * the node and the reference to the basic classes in its store.
	 */
	public interface ProducerOfStorageObject {

		/**
		 * Runs some transactions in the node, that yield the object.
		 * 
		 * @param node the node in whose store the object is being created
		 * @param consensus the consensus parameters of the node
		 * @param takamakaCodeReference the reference to the transaction that installed the Takamaka base classes in the node
		 * @return the reference of the object
		 */
		StorageReference apply(InitializedNode node, ConsensusParams consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException;
	}
}