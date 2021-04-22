package io.hotmoka.tendermint.internal;

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermint.views.TendermintInitializedNode;
import io.hotmoka.views.InitializedNode;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * Compared to the {@link io.hotmoka.nodes.views.InitializedNode} interface, this
 * class feeds the initialized node with the chain identifier and the
 * validators set of the underlying Tendermint network.
 */
public class TendermintInitializedNodeImpl implements TendermintInitializedNode {

	private final static Logger logger = LoggerFactory.getLogger(TendermintInitializedNodeImpl.class);

	/**
	 * The view that gets extended.
	 */
	private final InitializedNode parent;

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id and the validators
	 * of the underlying Tendermint network. It allows to specify the gas station to use.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param keysOfGamete the keys that must be used to control the gamete
	 * @param producerOfGasStationBuilder
	 * 		an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *      if this is {@code null}, a generic gas station is created
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
	public TendermintInitializedNodeImpl(TendermintBlockchain parent, ConsensusParams consensus, KeyPair keysOfGamete, ProducerOfStorageObject producerOfGasStationBuilder, Path takamakaCode, BigInteger greenAmount, BigInteger redAmount) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		TendermintPoster poster = new TendermintPoster(parent.getConfig());

		// we modify the consensus parameters, by setting the chain identifier of the underlying Tendermint network
		consensus = consensus.toBuilder()
			.setChainId(poster.getTendermintChainId())
			.build();

		this.parent = InitializedNode.of(parent, consensus, keysOfGamete,
			takamakaCode, greenAmount, redAmount, (node, _consensus, takamakaCodeReference) -> createTendermintValidatorsBuilder(poster, node, _consensus, takamakaCodeReference), producerOfGasStationBuilder);
	}

	private static StorageReference createTendermintValidatorsBuilder(TendermintPoster poster, InitializedNode node, ConsensusParams consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException {
		SignatureAlgorithm<SignedTransactionRequest> signature = io.hotmoka.crypto.SignatureAlgorithm.mk(node.getSignatureAlgorithmForRequests(), SignedTransactionRequest::toByteArrayWithoutSignature);
		Signer signer = Signer.with(signature, node.keysOfGamete());
		StorageReference gamete = node.gamete();

		InstanceMethodCallTransactionRequest getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, BigInteger.valueOf(50_000), takamakaCodeReference, CodeSignature.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).value;

		// we create validators corresponding to those declared in the configuration file of the Tendermint node
		TendermintValidator[] tendermintValidators = poster.getTendermintValidators().toArray(TendermintValidator[]::new);

		Encoder encoder = Base64.getEncoder();
		String publicKeys = Stream.of(tendermintValidators)
			.map(TendermintInitializedNodeImpl::publicKeyFromTendermintValidator)
			.map(PublicKey::getEncoded)
			.map(encoder::encodeToString)
			.collect(Collectors.joining(" "));

		String powers = Stream.of(tendermintValidators)
			.map(TendermintInitializedNodeImpl::powerFromTendermintValidator)
			.map(String::valueOf)
			.collect(Collectors.joining(" "));

		// we create the builder of the validators, passing the public keys of the validators and their powers
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(signer, gamete, nonceOfGamete, "", BigInteger.valueOf(100_000), ZERO, takamakaCodeReference,
			new ConstructorSignature(ClassType.TENDERMINT_VALIDATORS + "$Builder", ClassType.STRING, ClassType.STRING, ClassType.BIG_INTEGER),
			new StringValue(publicKeys), new StringValue(powers), new BigIntegerValue(consensus.ticketForNewPoll));

		StorageReference builder = node.addConstructorCallTransaction(request);

		Stream.of(tendermintValidators)
			.forEachOrdered(tv -> logger.info("added Tendermint validator with address " + tv.address + " and power " + tv.power));

		return builder;
	}

	private static PublicKey publicKeyFromTendermintValidator(TendermintValidator validator) {
		if (!"tendermint/PubKeyEd25519".equals(validator.publicKeyType))
			throw new IllegalArgumentException("It is currently possible to create Tendermint validators only if they use Ed25519 keys");

        try {
        	byte[] raw = Base64.getDecoder().decode(validator.publicKey);
        	SubjectPublicKeyInfo info = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(new Ed25519PublicKeyParameters(raw, 0));
        	SignatureAlgorithm<SignedTransactionRequest> ed25519 = io.hotmoka.crypto.SignatureAlgorithm.ed25519(SignedTransactionRequest::toByteArrayWithoutSignature);
			return ed25519.publicKeyFromEncoded(info.getEncoded());
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw InternalFailureException.of(e);
		}
        catch (InvalidKeySpecException e) {
        	throw new IllegalArgumentException(e);
		}
	}

	private static long powerFromTendermintValidator(TendermintValidator validator) {
		if (!"tendermint/PubKeyEd25519".equals(validator.publicKeyType))
			throw new IllegalArgumentException("It is currently possible to create Tendermint validators only if they use Ed25519 keys");

		return validator.power;
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
	public String getSignatureAlgorithmForRequests() {
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