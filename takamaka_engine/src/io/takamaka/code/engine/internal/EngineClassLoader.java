package io.takamaka.code.engine.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.takamaka.code.engine.AbstractBlockchain;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.Node;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 */
public class EngineClassLoader implements TakamakaClassLoader {

	/**
	 * The parent of this class loader;
	 */
	private final TakamakaClassLoader parent;

	/**
	 * The temporary files that hold the class path for a transaction.
	 */
	private final List<Path> classpathElements = new ArrayList<>();

	/**
	 * Method {@link io.takamaka.code.lang.Contract#entry(io.takamaka.code.lang.Contract)}.
	 */
	public final Method entry;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, int)}.
	 */
	public final Method payableEntryInt;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, long)}.
	 */
	public final Method payableEntryLong;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, BigInteger)}.
	 */
	public final Method payableEntryBigInteger;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, int)}.
	 */
	public final Method redPayableInt;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, long)}.
	 */
	public final Method redPayableLong;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, BigInteger)}.
	 */
	public final Method redPayableBigInteger;

	/**
	 * The field {@link io.takamaka.code.lang.Storage#storageReference}.
	 */
	public final Field storageReference;

	/**
	 * The field {@link io.takamaka.code.lang.Storage#inStorage}.
	 */
	public final Field inStorage;

	/**
	 * Builds the class loader for the given class path and its dependencies.
	 * 
	 * @param classpath the class path
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(Classpath classpath, Node node, AbstractBlockchain blockchain) throws Exception {
		this.parent = TakamakaClassLoader.of(collectURLs(Stream.of(classpath), node, blockchain, null));
		Class<?> contract = getContract(), redGreenContract = getRedGreenContract(), storage = getStorage();
		this.entry = contract.getDeclaredMethod("entry", contract);
		this.entry.setAccessible(true); // it was private
		this.payableEntryInt = contract.getDeclaredMethod("payableEntry", contract, int.class);
		this.payableEntryInt.setAccessible(true); // it was private
		this.payableEntryLong = contract.getDeclaredMethod("payableEntry", contract, long.class);
		this.payableEntryLong.setAccessible(true); // it was private
		this.payableEntryBigInteger = contract.getDeclaredMethod("payableEntry", contract, BigInteger.class);
		this.payableEntryBigInteger.setAccessible(true); // it was private
		this.redPayableInt = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, int.class);
		this.redPayableInt.setAccessible(true); // it was private
		this.redPayableLong = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, long.class);
		this.redPayableLong.setAccessible(true); // it was private
		this.redPayableBigInteger = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, BigInteger.class);
		this.redPayableBigInteger.setAccessible(true); // it was private
		this.storageReference = storage.getDeclaredField(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME);
		this.storageReference.setAccessible(true); // it was private
		this.inStorage = storage.getDeclaredField(InstrumentationConstants.IN_STORAGE);
		this.inStorage.setAccessible(true); // it was private
	}

	/**
	 * Builds the class loader for the given jar and its dependencies.
	 * 
	 * @param jar the jar
	 * @param dependencies the dependencies
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(Path jar, Stream<Classpath> dependencies, Node node, AbstractBlockchain blockchain) throws Exception {
		this.parent = TakamakaClassLoader.of(collectURLs(dependencies, node, blockchain, jar.toUri()));
		Class<?> contract = getContract(), redGreenContract = getRedGreenContract(), storage = getStorage();
		this.entry = contract.getDeclaredMethod("entry", contract);
		this.entry.setAccessible(true); // it was private
		this.payableEntryInt = contract.getDeclaredMethod("payableEntry", contract, int.class);
		this.payableEntryInt.setAccessible(true); // it was private
		this.payableEntryLong = contract.getDeclaredMethod("payableEntry", contract, long.class);
		this.payableEntryLong.setAccessible(true); // it was private
		this.payableEntryBigInteger = contract.getDeclaredMethod("payableEntry", contract, BigInteger.class);
		this.payableEntryBigInteger.setAccessible(true); // it was private
		this.redPayableInt = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, int.class);
		this.redPayableInt.setAccessible(true); // it was private
		this.redPayableLong = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, long.class);
		this.redPayableLong.setAccessible(true); // it was private
		this.redPayableBigInteger = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, BigInteger.class);
		this.redPayableBigInteger.setAccessible(true); // it was private
		this.storageReference = storage.getDeclaredField(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME);
		this.storageReference.setAccessible(true); // it was private
		this.inStorage = storage.getDeclaredField(InstrumentationConstants.IN_STORAGE);
		this.inStorage.setAccessible(true); // it was private
	}

	private URL[] collectURLs(Stream<Classpath> classpaths, Node node, AbstractBlockchain blockchain, URI start) throws Exception {
		List<URL> urls = new ArrayList<>();
		if (start != null) {
			urls.add(start.toURL());
			classpathElements.add(Paths.get(start));
		}

		for (Classpath classpath: classpaths.toArray(Classpath[]::new))
			urls = addURLs(classpath, node, blockchain, urls);

		return urls.toArray(new URL[urls.size()]);
	}

	/**
	 * Yields the request that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the request
	 * @throws Exception if the request could not be found
	 */
	private static TransactionRequest<?> getRequestAndCharge(TransactionReference transaction, Node node, AbstractBlockchain blockchain) throws Exception {
		blockchain.chargeForCPU(blockchain.gasCostModel.cpuCostForGettingRequestAt(transaction));
		return node.getRequestAt(transaction);
	}

	/**
	 * Yields the response that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	private static TransactionResponse getResponseAtAndCharge(TransactionReference transaction, Node node, AbstractBlockchain blockchain) throws Exception {
		blockchain.chargeForCPU(blockchain.gasCostModel.cpuCostForGettingResponseAt(transaction));
		return node.getResponseAt(transaction);
	}

	private List<URL> addURLs(Classpath classpath, Node node, AbstractBlockchain blockchain, List<URL> bag) throws Exception {
		// if the class path is recursive, we consider its dependencies as well, recursively
		if (classpath.recursive) {
			TransactionRequest<?> request = getRequestAndCharge(classpath.transaction, node, blockchain);
			if (!(request instanceof AbstractJarStoreTransactionRequest))
				throw new IllegalTransactionRequestException("classpath does not refer to a jar store transaction");

			Stream<Classpath> dependencies = ((AbstractJarStoreTransactionRequest) request).getDependencies();
			for (Classpath dependency: dependencies.toArray(Classpath[]::new))
				addURLs(dependency, node, blockchain, bag);
		}

		TransactionResponse response = getResponseAtAndCharge(classpath.transaction, node, blockchain);
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new IllegalTransactionRequestException("classpath does not refer to a successful jar store transaction");

		byte[] instrumentedJarBytes = ((TransactionResponseWithInstrumentedJar) response).getInstrumentedJar();
		blockchain.chargeForCPU(blockchain.gasCostModel.cpuCostForLoadingJar(instrumentedJarBytes.length));
		blockchain.chargeForRAM(blockchain.gasCostModel.ramCostForLoading(instrumentedJarBytes.length));

		try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(instrumentedJarBytes))) {
			Path classpathElement = Files.createTempFile("takamaka_", "@" + classpath.transaction + ".jar");
			Files.copy(is, classpathElement, StandardCopyOption.REPLACE_EXISTING);

			// we add, for class loading, the jar containing the instrumented code
			URI uri = classpathElement.toFile().toURI();
			bag.add(uri.toURL());
			classpathElements.add(Paths.get(uri));
		}

		return bag;
	}

	@Override
	public void close() throws IOException {
		// we delete all paths elements that were used to build this class loader
		for (Path classpathElement: classpathElements)
			Files.deleteIfExists(classpathElement);

		parent.close();
	}

	@Override
	public final Class<?> loadClass(String className) throws ClassNotFoundException {
		return parent.loadClass(className);
	}

	@Override
	public WhiteListingWizard getWhiteListingWizard() {
		return parent.getWhiteListingWizard();
	}

	@Override
	public Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return parent.resolveField(className, name, type);
	}

	@Override
	public Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) {
		return parent.resolveField(clazz, name, type);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return parent.resolveConstructor(className, args);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) {
		return parent.resolveConstructor(clazz, args);
	}

	@Override
	public Optional<Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveMethod(clazz, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveInterfaceMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveInterfaceMethod(clazz, methodName, args, returnType);
	}

	@Override
	public boolean isStorage(String className) {
		return parent.isStorage(className);
	}

	@Override
	public boolean isContract(String className) {
		return parent.isContract(className);
	}

	@Override
	public boolean isRedGreenContract(String className) {
		return parent.isRedGreenContract(className);
	}

	@Override
	public boolean isInterface(String className) {
		return parent.isInterface(className);
	}

	@Override
	public boolean isLazilyLoaded(Class<?> type) {
		return parent.isLazilyLoaded(type);
	}

	@Override
	public boolean isEagerlyLoaded(Class<?> type) {
		return parent.isEagerlyLoaded(type);
	}

	@Override
	public Class<?> getContract() {
		return parent.getContract();
	}

	@Override
	public Class<?> getRedGreenContract() {
		return parent.getRedGreenContract();
	}

	@Override
	public Class<?> getStorage() {
		return parent.getStorage();
	}

	@Override
	public Class<?> getExternallyOwnedAccount() {
		return parent.getExternallyOwnedAccount();
	}

	@Override
	public Class<?> getRedGreenExternallyOwnedAccount() {
		return parent.getRedGreenExternallyOwnedAccount();
	}

	@Override
	public ClassLoader getJavaClassLoader() {
		return parent.getJavaClassLoader();
	}
}