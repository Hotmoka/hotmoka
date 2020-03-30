package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.Gson;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTopLevelResult;
import io.hotmoka.tendermint.internal.beans.TendermintTxResult;

/**
 * An implementation of a blockchain integrated over the Tendermint generic
 * blockchain engine. It provides support for the creation of a given number of initial accounts.
 */
public class TendermintBlockchainImpl implements TendermintBlockchain {

	/**
	 * A proxy to the Tendermint process.
	 */
	private final Tendermint tendermint;

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	private final Classpath takamakaCode;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	private final Server server;

	private final ABCI abci;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	/**
	 * True if and only if this blockchain is initialized.
	 */
	private boolean initialized;

	final State state;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts with the given initial funds.
	 * 
	 * @param urlOfTendermint the URL of the Tendermint process. For instance: {@code http://localhost:26657}
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public TendermintBlockchainImpl(URL urlOfTendermint, Path takamakaCodePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		this.tendermint = new Tendermint(urlOfTendermint);
		this.abci = new ABCI(this);
    	this.state = new State();
		this.server = ServerBuilder.forPort(26658).addService(abci).build();
		this.server.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				close();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}));

		tendermint.ping();

		this.takamakaCode = new Classpath(addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath))), false);
		System.out.println("takamakaCode = " + takamakaCode);

		// we compute the total amount of funds needed to create the accounts
		BigInteger sum = Stream.of(funds).reduce(BigInteger.ZERO, BigInteger::add);

		StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode(), sum));

		initialized = true;

		// let us create the accounts
		this.accounts = new StorageReference[funds.length];
		ConstructorSignature constructor = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);
		for (int i = 0; i < accounts.length; i++) {}
		/*this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, gas, BigInteger.ZERO, takamakaCode(), constructor, new BigIntegerValue(funds[i])));*/
	}

	@Override
	public void close() throws InterruptedException {
    	server.shutdown();
    	server.awaitTermination();
    	state.close();
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}

	@Override
	public Classpath takamakaCode() {
		return takamakaCode;
	}

	@Override
	public void postJarStoreTransaction(JarStoreTransactionRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postConstructorCallTransaction(ConstructorCallTransactionRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getClassNameOf(StorageReference storageReference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionReference getTransactionReferenceFor(String toString) {
		return new TendermintTransactionReference(toString);
	}

	@Override
	public TransactionResponse getResponseAt(TransactionReference transactionReference) throws Exception {
		Optional<TransactionResponse> response = state.getResponseOf(transactionReference);
		if (response.isPresent())
			return response.get();
		else
			throw new IllegalStateException("cannot find no response for transaction " + transactionReference);
	}

	@Override
	public Stream<Classpath> getDependenciesOfJarStoreTransactionAt(TransactionReference transactionReference) throws Exception {
		Optional<Stream<Classpath>> dependencies = state.getDependenciesOf(transactionReference);
		if (dependencies.isPresent())
			return dependencies.get();
		else
			throw new IllegalStateException("cannot find no jar store dependencies for transaction " + transactionReference);
	}

	@Override
	public Stream<Update> getLastEagerUpdatesFor(StorageReference storageReference, Consumer<BigInteger> chargeForCPU,
			Function<String, Stream<Field>> eagerFields) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateOfField getLastLazyUpdateToNonFinalFieldOf(StorageReference storageReference, FieldSignature field,
			Consumer<BigInteger> chargeForCPU) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UpdateOfField getLastLazyUpdateToFinalFieldOf(StorageReference storageReference, FieldSignature field,
			Consumer<BigInteger> chargeForCPU) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getNow() throws Exception {
		return abci.getNow();
	}

	@Override
	public GasCostModel getGasCostModel() {
		return GasCostModel.standard();
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireNodeUninitialized();
			String response = tendermint.broadcastTxCommit(request);
			String hash = extractHashFromBroadcastTxResponse(response);
			return extractTransactionReferenceFromTendermintResult(hash);
		});
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request)
			throws TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request)
			throws TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Pools Tendermint for the result of a Tendermint transaction with the given hash.
	 * When it is available, parse its result looking for the {@code data}
	 * field, that should contained the reference of the Hotmoka transaction
	 * executed for that Tendermint transaction.
	 *  
	 * @param hash the hash of the Tendermint transaction
	 * @return the reference to the Hotmoka transaction
	 * @throws Exception if the transaction reference cannot be found
	 */
	private TransactionReference extractTransactionReferenceFromTendermintResult(String hash) throws Exception {
		TendermintTopLevelResult tendermintResult = tendermint.poll(hash);
	
		TendermintTxResult tx_result = tendermintResult.tx_result;
		if (tx_result == null)
			throw new TransactionException("no result for transaction " + hash);
	
		String data = tx_result.data;
		if (data == null)
			throw new TransactionException("no transaction reference found in data field of Tendermint transaction");
	
		Object dataAsObject = base64DeserializationOf(data);
		if (!(dataAsObject instanceof String))
			throw new TransactionException("no transaction reference found in data field of Tendermint transaction");
	
		return new TendermintTransactionReference((String) dataAsObject);
	}

	private String extractHashFromBroadcastTxResponse(String response) {
		TendermintBroadcastTxResponse parsedResponse = gson.fromJson(response, TendermintBroadcastTxResponse.class);
	
		String error = parsedResponse.error;
		if (error != null && !error.isEmpty())
			throw new IllegalStateException("Tendermint transaction failed: " + error);
	
		TendermintTopLevelResult result = parsedResponse.result;
	
		if (result == null)
			throw new IllegalStateException("missing result in Tendermint response");
	
		String hash = result.hash;
		if (hash == null)
			throw new IllegalStateException("missing hash in Tendermint response");
	
		return hash;
	}

	/**
	 * Checks if this node is still not fully initialized, so that further initial transactions can still
	 * be executed. As soon as a non-initial transaction is run with this node, it is considered as initialized.
	 * 
	 * @throws IllegalStateException if this node is already initialized
	 */
	private void requireNodeUninitialized() throws IllegalStateException {
		if (initialized)
			throw new IllegalStateException("this node is already initialized");
	}

	/**
	 * Calls the given callable. If if throws an exception, it wraps it into an {@link io.hotmoka.beans.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
		try {
			return what.call();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
		}
	}

	/**
	 * Calls the given callable. If if throws a {@link io.hotmoka.beans.CodeExecutionException}, if throws it back
	 * unchanged. Otherwise, it wraps the exception into an {@link io.hotmoka.beans.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws CodeExecutionException the unwrapped exception
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapWithCodeInCaseOfException(Callable<T> what) throws TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (CodeExecutionException e) {
			throw e;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
		}
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @return the wrapped or original exception
	 */
	private static TransactionException wrapAsTransactionException(Throwable t) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(t);
	}

	private static Object base64DeserializationOf(String s) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(s)))) {
			return ois.readObject();
		}
	}
}