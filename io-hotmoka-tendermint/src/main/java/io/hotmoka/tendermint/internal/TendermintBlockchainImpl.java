package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Optional;
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
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.internal.beans.TendermintBroadcastTxResponse;
import io.hotmoka.tendermint.internal.beans.TendermintTopLevelResult;
import io.hotmoka.tendermint.internal.beans.TendermintTxResult;
import io.hotmoka.tendermint.internal.beans.TxError;
import io.takamaka.code.engine.AbstractNode;

/**
 * An implementation of a blockchain integrated over the Tendermint generic
 * blockchain engine. It provides support for the creation of a given number of initial accounts.
 */
public class TendermintBlockchainImpl extends AbstractNode implements TendermintBlockchain {

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

	final State state;

	/**
	 * Builds a Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This constructor spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 * @throws InterruptedException if the Java process has been interrupted while starting the Tendermint process
	 */
	public TendermintBlockchainImpl(Config config, Path takamakaCodePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException, InterruptedException {
		this.abci = new ABCI(this);
		deleteDir(config.dir);

		try {
			this.state = new State(config.dir + "/state");
			this.server = ServerBuilder.forPort(config.abciPort).addService(abci).build();
			this.server.start();
			this.tendermint = new Tendermint(config, true);

			addShutdownHook();

			this.takamakaCode = new Classpath(addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath))), false);
			System.out.println("takamakaCode = " + takamakaCode);

			// we compute the total amount of funds needed to create the accounts
			BigInteger sum = Stream.of(funds).reduce(BigInteger.ZERO, BigInteger::add);

			StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode(), sum));
			System.out.println("gamete = " + gamete);

			// let us create the accounts
			this.accounts = new StorageReference[funds.length];
			ConstructorSignature constructor = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);
			BigInteger gas = BigInteger.valueOf(10000); // enough for creating an account
			for (int i = 0; i < accounts.length; i++) {
				this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
						(gamete, gas, BigInteger.ZERO, takamakaCode(), constructor, new BigIntegerValue(funds[i])));

				System.out.println("account #" + i + ": " + accounts[i]);
			}
		}
		catch (Exception e) {
			try {
				close();
			}
			catch (Exception e2) {}

			throw e;
		}
	}

	@Override
	public void close() throws InterruptedException {
		if (tendermint != null)
			tendermint.close();

		if (server != null && !server.isShutdown()) {
			server.shutdown();
			server.awaitTermination();
		}

		if (state != null)
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
	public long getNow() throws Exception {
		return abci.getNow();
	}

	@Override
	protected TransactionReference addJarStoreInitialTransactionInternal(JarStoreInitialTransactionRequest request) throws Exception {
		String response = tendermint.broadcastTxAsync(request);
		String hash = extractHashFromBroadcastTxResponse(response);
		TransactionReference transactionReference = extractTransactionReferenceFromTendermintResult(hash);
		return ((JarStoreInitialTransactionResponse) state.getResponseOf(transactionReference).get()).getOutcomeAt(transactionReference);
	}

	@Override
	protected StorageReference addGameteCreationTransactionInternal(GameteCreationTransactionRequest request) throws Exception {
		String response = tendermint.broadcastTxAsync(request);
		String hash = extractHashFromBroadcastTxResponse(response);
		TransactionReference transactionReference = extractTransactionReferenceFromTendermintResult(hash);
		return ((GameteCreationTransactionResponse) state.getResponseOf(transactionReference).get()).getOutcome();
	}

	@Override
	protected StorageReference addRedGreenGameteCreationTransactionInternal(RedGreenGameteCreationTransactionRequest request) throws Exception {
		String response = tendermint.broadcastTxAsync(request);
		String hash = extractHashFromBroadcastTxResponse(response);
		TransactionReference transactionReference = extractTransactionReferenceFromTendermintResult(hash);
		return ((GameteCreationTransactionResponse) state.getResponseOf(transactionReference).get()).getOutcome();
	}

	@Override
	protected TransactionReference addJarStoreTransactionInternal(JarStoreTransactionRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StorageReference addConstructorCallTransactionInternal(ConstructorCallTransactionRequest request) throws Exception {
		String response = tendermint.broadcastTxAsync(request);
		String hash = extractHashFromBroadcastTxResponse(response);
		TransactionReference transactionReference = extractTransactionReferenceFromTendermintResult(hash);
		return ((ConstructorCallTransactionResponse) state.getResponseOf(transactionReference).get()).getOutcome();
	}

	@Override
	protected StorageValue addInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StorageValue addStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StorageValue runViewInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception {
		//TODO
		return null;
	}

	@Override
	protected StorageValue runViewStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Stream<TransactionReference> getHistoryOf(StorageReference object) {
		return state.getHistoryOf(object).get();
	}

	/**
	 * Deletes the given directory, recursively.
	 * 
	 * @param dir the directory to delete
	 * @throws IOException if the directory or some of its subdirectories cannot be deleted
	 */
	private static void deleteDir(Path dir) throws IOException {
		if (Files.exists(dir))
		Files.walk(dir)
			.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
	}

	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				close();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}));
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
			throw new TransactionException("no Hotmoka transaction reference found in data field of Tendermint transaction " + hash
				+ "(" + tx_result.info + ")");
	
		Object dataAsObject = base64DeserializationOf(data);
		if (!(dataAsObject instanceof String))
			throw new TransactionException("no Hotmoka transaction reference found in data field of Tendermint transaction " + hash
				+ "(" + tx_result.info + ")");
	
		return new TendermintTransactionReference((String) dataAsObject);
	}

	private String extractHashFromBroadcastTxResponse(String response) {
		TendermintBroadcastTxResponse parsedResponse = gson.fromJson(response, TendermintBroadcastTxResponse.class);
	
		TxError error = parsedResponse.error;
		if (error != null)
			throw new IllegalStateException("Tendermint transaction failed: " + error.message + ": " + error.data);
	
		TendermintTopLevelResult result = parsedResponse.result;
	
		if (result == null)
			throw new IllegalStateException("missing result in Tendermint response");
	
		String hash = result.hash;
		if (hash == null)
			throw new IllegalStateException("missing hash in Tendermint response");
	
		return hash;
	}

	private static Object base64DeserializationOf(String s) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(s)))) {
			return ois.readObject();
		}
	}
}