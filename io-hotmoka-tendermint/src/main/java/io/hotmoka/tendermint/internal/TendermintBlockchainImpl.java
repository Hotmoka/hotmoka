package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.gson.Gson;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
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
	 * This is copy of the information in the state, for efficiency.
	 */
	private final Classpath takamakaCode;

	/**
	 * The classpath of a user jar that has been installed, if any.
	 * This jar is typically referred to at construction time of the node.
	 */
	private final Classpath jar;

	/**
	 * The accounts created during initialization.
	 * This is copy of the information in the state, for efficiency.
	 */
	private final StorageReference[] accounts;

	/**
	 * True if and only if this node doesn't accept initial transactions anymore.
	 * This is copy of the information in the state, for efficiency.
	 */
	private boolean initialized;

    private final Server server;

	private final ABCI abci;

	/**
	 * An object for JSON manipulation.
	 */
	private final Gson gson = new Gson();

	/**
	 * The state where blockchain data is persisted.
	 */
	final State state;

	/**
	 * Builds a Tendermint blockchain and initializes user accounts with the given initial funds.
	 * This constructor spawns the Tendermint process on localhost and connects it to an ABCI application
	 * for handling its transactions. The blockchain gets deleted if it existed already at the given directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param jar the path of a jar that must be installed after the creation of the gamete. This is optional and mainly
	 *            useful to simplify the implementation of tests
	 * @param funds the initial funds of the accounts that are created
	 * @throws Exception if the blockchain could not be created
	 */
	public TendermintBlockchainImpl(Config config, Path takamakaCodePath, Optional<Path> jar, BigInteger... funds) throws Exception {
		try {
			this.abci = new ABCI(this);
			deleteDir(config.dir);
			this.state = new State(config.dir + "/state");
			this.server = ServerBuilder.forPort(config.abciPort).addService(abci).build();
			this.server.start();
			this.tendermint = new Tendermint(config, true);

			addShutdownHook();

			this.takamakaCode = new Classpath(addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath))), false);
			state.putTakamakaCode(takamakaCode);

			// we compute the total amount of funds needed to create the accounts
			BigInteger sum = Stream.of(funds).reduce(BigInteger.ZERO, BigInteger::add);

			StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode(), sum));

			BigInteger nonce = BigInteger.ZERO;
			JarSupplier jarSupplier;

			if (jar.isPresent()) {
				jarSupplier = postJarStoreTransaction(new JarStoreTransactionRequest(gamete, nonce, BigInteger.valueOf(1_000_000), BigInteger.ZERO, takamakaCode(), Files.readAllBytes(jar.get()), new Classpath(takamakaCode().transaction, true)));
				nonce = nonce.add(BigInteger.ONE);
			}
			else
				jarSupplier = null;

			// let us create the accounts
			this.accounts = new StorageReference[funds.length];
			ConstructorSignature constructor = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);
			BigInteger gas = BigInteger.valueOf(10_000); // enough for creating an account
			List<CodeSupplier<StorageReference>> accounts = new ArrayList<>();

			for (BigInteger fund: funds) {
				accounts.add(postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, nonce, gas, BigInteger.ZERO, takamakaCode(), constructor, new BigIntegerValue(fund))));

				nonce = nonce.add(BigInteger.ONE);
			}

			int i = 0;
			for (CodeSupplier<StorageReference> account: accounts)
				state.addAccount(this.accounts[i++] = account.get());

			if (jar.isPresent()) {
				this.jar = new Classpath(jarSupplier.get(), true);
				state.putJar(this.jar);
			}
			else
				this.jar = null;

			tendermint.pollingTime = 0L;
			tendermint.writingTime = 0L;
		}
		catch (Throwable t) {
			try {
				deleteDir(config.dir); // do not leave zombies behind
				close();
			}
			catch (Exception e2) {}

			throw t;
		}
	}

	/**
	 * Builds a Tendermint blockchain and initializes red/green user accounts with the given initial funds.
	 * 
	 * @param config the configuration of the blockchain
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param jar the path of a jar that must be installed after the creation of the gamete. This is optional and mainly
	 *            useful to simplify the implementation of tests
	 * @param redGreen unused; only meant to distinguish the signature of this constructor from that of the previous one
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws Exception if the blockchain could not be created
	 */
	public TendermintBlockchainImpl(Config config, Path takamakaCodePath, Optional<Path> jar, boolean redGreen, BigInteger... funds) throws Exception {
		try {
			this.abci = new ABCI(this);
			deleteDir(config.dir);
			this.state = new State(config.dir + "/state");
			this.server = ServerBuilder.forPort(config.abciPort).addService(abci).build();
			this.server.start();
			this.tendermint = new Tendermint(config, true);

			addShutdownHook();

			this.takamakaCode = new Classpath(addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath))), false);
			state.putTakamakaCode(takamakaCode);

			// we compute the total amount of funds needed to create the accounts
			BigInteger green = BigInteger.ZERO;
			for (int pos = 0; pos < funds.length; pos += 2)
				green = green.add(funds[pos]);

			BigInteger red = BigInteger.ZERO;
			for (int pos = 1; pos < funds.length; pos += 2)
				red = red.add(funds[pos]);

			StorageReference gamete = addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCode(), green, red));

			BigInteger nonce = BigInteger.ZERO;
			JarSupplier jarSupplier;

			if (jar.isPresent()) {
				jarSupplier = postJarStoreTransaction(new JarStoreTransactionRequest(gamete, nonce, BigInteger.valueOf(1_000_000), BigInteger.ZERO, takamakaCode(), Files.readAllBytes(jar.get()), new Classpath(takamakaCode().transaction, true)));
				nonce = nonce.add(BigInteger.ONE);
			}
			else
				jarSupplier = null;

			// let us create the accounts
			this.accounts = new StorageReference[funds.length / 2];
			BigInteger gas = BigInteger.valueOf(10000); // enough for creating an account
			ConstructorSignature constructor = new ConstructorSignature(ClassType.TRGEOA, ClassType.BIG_INTEGER);
			VoidMethodSignature receiveRed = new VoidMethodSignature(ClassType.RGPAYABLE_CONTRACT, "receiveRed", ClassType.BIG_INTEGER);

			List<CodeSupplier<StorageReference>> accounts = new ArrayList<>();

			for (int i = 0; i < this.accounts.length; i++) {
				// the constructor provides the green coins
				accounts.add(postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(gamete, nonce, gas, BigInteger.ZERO, takamakaCode(), constructor, new BigIntegerValue(funds[i * 2]))));

				nonce = nonce.add(BigInteger.ONE);
			}

			int i = 0;
			for (CodeSupplier<StorageReference> account: accounts) {
				// then we add the red coins
				state.addAccount(this.accounts[i] = account.get());
				postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, nonce, gas, BigInteger.ZERO, takamakaCode(),
					receiveRed, this.accounts[i], new BigIntegerValue(funds[1 + i * 2])));

				nonce = nonce.add(BigInteger.ONE);
				i++;
			}

			if (jar.isPresent()) {
				this.jar = new Classpath(jarSupplier.get(), true);
				state.putJar(this.jar);
			}
			else
				this.jar = null;
		}
		catch (Throwable t) {
			try {
				deleteDir(config.dir); // do not leave zombies behind
				close();
			}
			catch (Exception e2) {}

			throw t;
		}
	}

	/**
	 * Builds a Tendermint blockchain and initializes it with the information already
	 * existing at its configuration directory. This constructor can be used to
	 * recover a blockchain already created in the past, with all its information.
	 * A Tendermint blockchain must have been already successfully created at
	 * its configuration directory.
	 * 
	 * @param config the configuration of the blockchain
	 * @throws IOException if a disk error occurs
	 * @throws InterruptedException if the Java process has been interrupted while starting Tendermint
	 */
	public TendermintBlockchainImpl(Config config) throws IOException, InterruptedException {
		this.abci = new ABCI(this);

		try {
			this.state = new State(config.dir + "/state");
			this.server = ServerBuilder.forPort(config.abciPort).addService(abci).build();
			this.server.start();
			this.tendermint = new Tendermint(config, false);

			addShutdownHook();

			this.takamakaCode = state.getTakamakaCode().get();
			this.jar = state.getJar().orElse(null);
			this.initialized = state.isInitialized();
			this.accounts = state.getAccounts().toArray(StorageReference[]::new);
			setNext(state.getNext().orElse(LocalTransactionReference.FIRST));
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
	public void close() throws Exception {
		super.close();

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
	public Optional<Classpath> jar() {
		return Optional.ofNullable(jar);
	}

	@Override
	protected void setNext(LocalTransactionReference next) {
		super.setNext(next);
		state.putNext(next);
	}

	@Override
	protected TransactionResponse getResponseAtInternal(TransactionReference transactionReference) throws Exception {
		return state.getResponseOf(transactionReference)
			.orElseThrow(() -> new IllegalStateException("cannot find no response for transaction " + transactionReference));
	}

	@Override
	protected TransactionReference nextAndIncrement() {
		return super.nextAndIncrement();
	}

	@Override
	protected Supplier<String> postTransaction(TransactionRequest<?> request) throws Exception {
		String response = tendermint.broadcastTxAsync(request);

		return () -> extractHashFromBroadcastTxResponse(response);
	}

	@Override
	public TransactionReference getTransactionReferenceFor(String id) throws Exception {
		TendermintTopLevelResult tendermintResult = tendermint.poll(id);

		TendermintTxResult tx_result = tendermintResult.tx_result;
		if (tx_result == null)
			throw new IllegalStateException("no result for transaction " + id);

		String data = tx_result.data;
		if (data == null)
			throw new IllegalStateException(tx_result.info);

		Object dataAsObject = base64DeserializationOf(data);
		if (!(dataAsObject instanceof String))
			throw new IllegalStateException("no Hotmoka transaction reference found in data field of Tendermint transaction");

		return new LocalTransactionReference((String) dataAsObject);
	}

	@Override
	public long getNow() throws Exception {
		return abci.getNow();
	}

	@Override
	protected Stream<TransactionReference> getHistoryOf(StorageReference object) {
		return state.getHistoryOf(object).orElseGet(Stream::empty);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		state.setHistory(object, history);
	}

	@Override
	protected void expandStoreWith(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws Exception {
		state.putResponseOf(reference, response);
		super.expandStoreWith(reference, request, response);
	}

	@Override
	protected boolean isInitialized() {
		return initialized;
	}

	@Override
	protected void markAsInitialized() {
		if (!initialized) {
			state.markAsInitialized();
			initialized = true;
		}
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
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
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