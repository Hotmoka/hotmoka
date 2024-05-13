/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.node.tendermint.internal;

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.tendermint.api.TendermintNode;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * Compared to the {@link io.hotmoka.helpers.api.views.InitializedNode} interface, this
 * class feeds the initialized node with the chain identifier and the
 * validators set of the underlying Tendermint network.
 */
public class TendermintInitializedNodeImpl implements InitializedNode {

	private final static Logger logger = Logger.getLogger(TendermintInitializedNodeImpl.class.getName());

	/**
	 * The view that gets extended.
	 */
	private final InitializedNode parent;

	/**
	 * True if and only if this node has been closed already.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id, the genesis time and the validators
	 * of the underlying Tendermint network. It allows to specify the gas station to use.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param producerOfGasStationBuilder
	 * 		an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *      if this is {@code null}, a generic gas station is created
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public TendermintInitializedNodeImpl(TendermintNode parent, ValidatorsConsensusConfig<?,?> consensus,
			ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStationBuilder, Path takamakaCode) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException, NodeException, TimeoutException, InterruptedException {

		var tendermintConfigFile = new TendermintConfigFile(parent.getLocalConfig());
		var poster = new TendermintPoster(parent.getLocalConfig(), tendermintConfigFile.tendermintPort);

		// we modify the consensus parameters, by setting the chain identifier and the genesis time to that of the underlying Tendermint network
		consensus = consensus.toBuilder()
			.setChainId(poster.getTendermintChainId())
			.setGenesisTime(LocalDateTime.parse(poster.getGenesisTime(), DateTimeFormatter.ISO_DATE_TIME))
			.build();

		this.parent = InitializedNodes.of(parent, consensus, takamakaCode,
			(node, _consensus, takamakaCodeReference) -> createTendermintValidatorsBuilder(poster, node, _consensus, takamakaCodeReference),
			producerOfGasStationBuilder);

		// when the parent is closed, this decorator will be closed as well
		parent.addOnCloseHandler(this_close);
	}

	private static StorageReference createTendermintValidatorsBuilder(TendermintPoster poster, InitializedNode node, ValidatorsConsensusConfig<?,?> consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {
		StorageReference gamete = node.gamete();
		var getNonceRequest = TransactionRequests.instanceViewMethodCall
			(gamete, BigInteger.valueOf(50_000), takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonceOfGamete = node.runInstanceMethodCallTransaction(getNonceRequest)
			.orElseThrow(() -> new NodeException(MethodSignatures.NONCE + " should not return void"))
			.asBigInteger(value -> new NodeException(MethodSignatures.NONCE + " should return a BigInteger, not a " + value.getClass().getName()));

		// we create validators corresponding to those declared in the configuration file of the Tendermint node
		var tendermintValidators = poster.getTendermintValidators().toArray(TendermintValidator[]::new);

		var ed25519 = SignatureAlgorithms.ed25519();

		// we create the builder of the validators
		var _200_000 = BigInteger.valueOf(200_000);
		String builderClassName = StorageTypes.TENDERMINT_VALIDATORS + "$Builder";

		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCodeReference,
					ConstructorSignatures.of(builderClassName, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG,
					StorageTypes.INT, StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
			StorageValues.bigIntegerOf(consensus.getTicketForNewPoll()), StorageValues.bigIntegerOf(consensus.getFinalSupply()),
			StorageValues.longOf(consensus.getInitialInflation()), StorageValues.intOf(consensus.getPercentStaked()), StorageValues.intOf(consensus.getBuyerSurcharge()),
			StorageValues.intOf(consensus.getSlashingForMisbehaving()), StorageValues.intOf(consensus.getSlashingForNotBehaving()));

		nonceOfGamete = nonceOfGamete.add(BigInteger.ONE);

		StorageReference builder = node.addConstructorCallTransaction(request);

		// we populate the builder with a Tendermint validator at a time; this guarantees that they are created with 0 as progressive identifier 
		var addValidatorMethod = MethodSignatures.ofVoid(builderClassName, "addValidator", StorageTypes.STRING, StorageTypes.LONG);
		for (TendermintValidator tv: tendermintValidators) {
			String publicKeyBase64 = Base64.toBase64String(ed25519.encodingOf(publicKeyFromTendermintValidator(tv)));
			long power = powerFromTendermintValidator(tv);
			var addValidator = TransactionRequests.instanceMethodCall
				(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCodeReference,
				addValidatorMethod, builder, StorageValues.stringOf(publicKeyBase64), StorageValues.longOf(power));
			node.addInstanceMethodCallTransaction(addValidator);
			nonceOfGamete = nonceOfGamete.add(BigInteger.ONE);
		}

		Stream.of(tendermintValidators)
			.forEachOrdered(tv -> logger.info("added Tendermint validator with address " + tv.address + " and power " + tv.power));

		return builder;
	}

	private static PublicKey publicKeyFromTendermintValidator(TendermintValidator validator) {
		if (!"tendermint/PubKeyEd25519".equals(validator.publicKeyType))
			throw new IllegalArgumentException("It is currently possible to create Tendermint validators only if they use Ed25519 keys");

        try {
        	byte[] encoded = Base64.fromBase64String(validator.publicKey);
        	return SignatureAlgorithms.ed25519().publicKeyFromEncoding(encoded);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
        catch (InvalidKeySpecException | Base64ConversionException e) {
        	throw new IllegalArgumentException(e);
		}
	}

	private static long powerFromTendermintValidator(TendermintValidator validator) {
		if (!"tendermint/PubKeyEd25519".equals(validator.publicKeyType))
			throw new IllegalArgumentException("It is currently possible to create Tendermint validators only if they use Ed25519 keys");

		return validator.power;
	}

	@Override
	public StorageReference gamete() throws NodeException, TimeoutException, InterruptedException {
		return parent.gamete();
	}

	@Override
	public void close() throws InterruptedException, NodeException {
		if (!isClosed.getAndSet(true))
			parent.close();
	}

	@Override
	public StorageReference getManifest() throws NodeException, TimeoutException, InterruptedException {
		return parent.getManifest();
	}

	@Override
	public TransactionReference getTakamakaCode() throws NodeException, TimeoutException, InterruptedException {
		return parent.getTakamakaCode();
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, TimeoutException, InterruptedException {
		return parent.getNodeInfo();
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getClassTag(reference);
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getState(reference);
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return parent.addJarStoreInitialTransaction(request);
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		return parent.addGameteCreationTransaction(request);
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, NodeException, TimeoutException, InterruptedException {
		return parent.addJarStoreTransaction(request);
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.addConstructorCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.addInstanceMethodCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.addStaticMethodCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.runInstanceMethodCallTransaction(request);
	}

	@Override
	public Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.runStaticMethodCallTransaction(request);
	}

	@Override
	public JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postJarStoreTransaction(request);
	}

	@Override
	public ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postConstructorCallTransaction(request);
	}

	@Override
	public MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postInstanceMethodCallTransaction(request);
	}

	@Override
	public MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		return parent.postStaticMethodCallTransaction(request);
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		parent.addInitializationTransaction(request);
	}

	@Override
	public ConsensusConfig<?,?> getConfig() throws NodeException, TimeoutException, InterruptedException {
		return parent.getConfig();
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getRequest(reference);
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, UnknownReferenceException, NodeException, TimeoutException, InterruptedException {
		return parent.getResponse(reference);
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		return parent.getPolledResponse(reference);
	}

	@Override
	public Subscription subscribeToEvents(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) throws UnsupportedOperationException, NodeException {
		return parent.subscribeToEvents(key, handler);
	}

	@Override
	public void addOnCloseHandler(OnCloseHandler handler) {
		parent.addOnCloseHandler(handler);
	}

	@Override
	public void removeOnCloseHandler(OnCloseHandler handler) {
		parent.removeOnCloseHandler(handler);
	}
}