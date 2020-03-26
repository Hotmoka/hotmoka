package io.hotmoka.tendermint.internal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.CodeExecutionException;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.tendermint.TendermintBlockchain;

/**
 * An implementation of a blockchain integrated over the Tendermint generic
 * blockchain engine. It provides support for the creation of a given number of initial accounts.
 */
public class TendermintBlockchainImpl implements TendermintBlockchain {

	/**
	 * The reference, in the blockchain, where the base Takamaka classes have been installed.
	 */
	private final Classpath takamakaCode;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts with the given initial funds.
	 * 
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public TendermintBlockchainImpl(Path takamakaCodePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		TransactionReference support = addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCodePath)));
		this.takamakaCode = new Classpath(support, false);

		// we compute the total amount of funds needed to create the accounts
		BigInteger sum = Stream.of(funds).reduce(BigInteger.ZERO, BigInteger::add);

		StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode(), sum));

		// let us create the accounts
		this.accounts = new StorageReference[funds.length];
		ConstructorSignature constructor = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);
		for (int i = 0; i < accounts.length; i++) {}
		/*this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, gas, BigInteger.ZERO, takamakaCode(), constructor, new BigIntegerValue(funds[i])));*/
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<Classpath> getDependenciesOfJarStoreTransactionAt(TransactionReference transactionReference)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionResponseWithInstrumentedJar getJarStoreResponseAt(TransactionReference transactionReference)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public GasCostModel getGasCostModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
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
}