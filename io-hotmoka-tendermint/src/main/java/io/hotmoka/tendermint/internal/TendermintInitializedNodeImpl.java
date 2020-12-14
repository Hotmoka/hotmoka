package io.hotmoka.tendermint.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.Validator;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermint.views.TendermintInitializedNode;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * Compared to the {@link #io.hotmoka.nodes.views.InitializedNode} interface, this
 * class feeds the initialized node with the chain identifier and the
 * validators set of the underlying Tendermint network.
 */
public class TendermintInitializedNodeImpl implements TendermintInitializedNode {

	/**
	 * The view that gets extended.
	 */
	private final InitializedNode parent;

	/**
	 * The keys for controlling the original validators, declared in the Tendermint configuration file.
	 */
	private final KeyPair[] keys;

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Generates new keys to control the gamete. Uses the chain id and the validators
	 * of the underlying Tendermint network.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfValidators the keys to use for the Takamaka accounts created for each
	 *                         Tendermint validator and stored in the manifest of the network
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
	public TendermintInitializedNodeImpl(TendermintBlockchain parent, IntFunction<KeyPair> keysOfValidators, Path takamakaCode, String manifestClassName, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		this(parent, parent.getSignatureAlgorithmForRequests().getKeyPair(), keysOfValidators, takamakaCode, manifestClassName, greenAmount, redAmount);
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id and the validators
	 * of the underlying Tendermint network.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfGamete the keys that must be used to control the gamete
	 * @param keysOfValidators the keys to use for the Takamaka accounts created
	 *                         for each Tendermint validator and stored in the manifest of the network
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
	public TendermintInitializedNodeImpl(TendermintBlockchain parent, KeyPair keysOfGamete, IntFunction<KeyPair> keysOfValidators, Path takamakaCode, String manifestClassName, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		TendermintValidator[] tendermintValidators = parent.getTendermintValidators().toArray(TendermintValidator[]::new);

		List<Validator> validators = new ArrayList<>();
		List<KeyPair> keys = new ArrayList<>();
		for (int i = 0; i < tendermintValidators.length; i++) {
			KeyPair keyPair = keysOfValidators.apply(i);
			keys.add(keyPair);
			validators.add(new Validator(tendermintValidators[i].address, tendermintValidators[i].power, keyPair.getPublic()));
		}

		this.keys = keys.toArray(KeyPair[]::new);
		this.parent = InitializedNode.of(parent, keysOfGamete, validators.stream(), takamakaCode, manifestClassName, parent.getTendermintChainId(), greenAmount, redAmount);
	}

	@Override
	public KeyPair keysOfValidator(int num) {
		return keys[num];
	}

	@Override
	public KeyPair keysOfGamete() {
		return parent.keysOfGamete();
	}

	@Override
	public StorageReference gamete() {
		return parent.gamete();
	}

	@Override
	public void close() throws Exception {
		parent.close();
	}

	@Override
	public StorageReference getManifest() throws NoSuchElementException {
		return parent.getManifest();
	}

	@Override
	public TransactionReference getTakamakaCode() {
		return parent.getTakamakaCode();
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
		return parent.getClassTag(reference);
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
		return parent.getState(reference);
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return parent.addJarStoreInitialTransaction(request);
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return parent.addGameteCreationTransaction(request);
	}

	@Override
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return parent.addRedGreenGameteCreationTransaction(request);
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return parent.addJarStoreTransaction(request);
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addConstructorCallTransaction(request);
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addStaticMethodCallTransaction(request);
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runStaticMethodCallTransaction(request);
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return parent.postJarStoreTransaction(request);
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postConstructorCallTransaction(request);
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postInstanceMethodCallTransaction(request);
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postStaticMethodCallTransaction(request);
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		parent.addInitializationTransaction(request);
	}

	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		return parent.getSignatureAlgorithmForRequests();
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
		return parent.getRequest(reference);
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return parent.getResponse(reference);
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		return parent.getPolledResponse(reference);
	}

	@Override
	public Subscription subscribeToEvents(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) throws UnsupportedOperationException {
		return parent.subscribeToEvents(key, handler);
	}
}