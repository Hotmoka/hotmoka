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
	 * Generates new keys to control the gamete.
	 * 
	 * @param parent the node to decorate
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param chainId the initial chainId set for the node, inside its manifest
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
	static InitializedNode of(Node parent, Path takamakaCode, String chainId, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new InitializedNodeImpl(parent, takamakaCode, chainId, greenAmount, redAmount);
	}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfGamete the keys that must be used to control the gamete
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param chainId the initial chainId set for the node, inside its manifest
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
	static InitializedNode of(Node parent, KeyPair keysOfGamete, Path takamakaCode, String chainId, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new InitializedNodeImpl(parent, keysOfGamete, takamakaCode, chainId, greenAmount, redAmount);
	}

	/**
	 * Yields a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete. It allows one to specify how
	 * the validators of the node are being created.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfGamete the key pair that will be used to control the gamete
	 * @param producerOfValidatorsBuilder an algorithm that creates the builder of the validators to be installed in the manifest of the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param chainId the initial chainId set for the node, inside its manifest
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
	static InitializedNode of(Node parent, KeyPair keysOfGamete, ProducerOfValidatorsBuilder producerOfValidatorsBuilder, Path takamakaCode, String chainId, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		return new InitializedNodeImpl(parent, keysOfGamete, producerOfValidatorsBuilder, takamakaCode, chainId, greenAmount, redAmount);
	}

	/**
	 * An algorithm that produces the builder of the validators of the node.
	 */
	public interface ProducerOfValidatorsBuilder {

		/**
		 * Runs some transactions in the node, in order to create the builder,
		 * and yields the storage reference of the latter.
		 * 
		 * @param node the node whose validators are being created
		 * @param takamakaCodeReference the reference to the transaction that installed the Takamaka base classes in the node
		 * @return the reference of the validators builder that has been created
		 */
		StorageReference apply(InitializedNode node, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException;
	}
}