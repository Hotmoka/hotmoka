package takamaka.blockchain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import takamaka.blockchain.types.StorageType;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Entry;
import takamaka.lang.Storage;
import takamaka.translator.JarInstrumentation;
import takamaka.translator.Program;

public abstract class AbstractBlockchain implements Blockchain {
	private static final String CONTRACT_NAME = "takamaka.lang.Contract";
	private static final String EXTERNALLY_OWNED_ACCOUNT_NAME = "takamaka.lang.ExternallyOwnedAccount";
	protected long currentBlock;
	protected short currentTransaction;
	private boolean isInitialized = false;

	/**
	 * The events accumulated during the ongoing transaction.
	 */
	private final List<String> events = new ArrayList<>();

	@Override
	public final StorageReference setAsInitialized(Classpath takamakaBase, BigInteger initialAmount) throws TransactionException {
		checkNotFull();

		Storage gamete;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(takamakaBase)) {
			// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
			Class<?> gameteClass = classLoader.loadClass(EXTERNALLY_OWNED_ACCOUNT_NAME);
			Class<?> contractClass = classLoader.loadClass(CONTRACT_NAME);
			initTransaction(classLoader);
			gamete = (Storage) gameteClass.newInstance();
			// we set the balance field of the gamete
			Field balanceField = contractClass.getDeclaredField("balance");
			balanceField.setAccessible(true); // since it is private
			balanceField.set(gamete, initialAmount);
			SortedSet<Update> updates = collectUpdates(new Storage[0], null, null, gamete);
			addGameteCreationTransactionInternal(takamakaBase, initialAmount, gamete.storageReference, updates);
			moveToNextTransaction();
			isInitialized = true;
			return gamete.storageReference;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	protected abstract void addGameteCreationTransactionInternal(Classpath takamakaBase, BigInteger initialAmount, StorageReference gamete, SortedSet<Update> updates) throws TransactionException;

	public final TransactionReference getCurrentTransactionReference() {
		return new TransactionReference(currentBlock, currentTransaction);
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(Path jar, Classpath... dependencies) throws TransactionException {
		if (isInitialized)
			throw new TransactionException("Blockchain already initialized");

		return addJarStoreTransactionCommon(jar, dependencies);
	}

	@Override
	public final TransactionReference addJarStoreTransaction(StorageReference caller, Classpath classpath, Path jar, Classpath... dependencies) throws TransactionException {
		checkNotFull();

		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			Storage deserializedCaller = caller.deserialize(classLoader, this);
			checkIsExternallyOwned(deserializedCaller);
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}

		TransactionReference ref = addJarStoreTransactionCommon(jar, dependencies);
		// TODO: update balance of caller
		return ref;
	}

	private TransactionReference addJarStoreTransactionCommon(Path jar, Classpath... dependencies) throws TransactionException {
		checkNotFull();

		Path jarName = jar.getFileName();
		String jn = jarName.toString();
		if (!jn.endsWith(".jar"))
			throw new TransactionException("Jar file should end in .jar");

		if (jn.length() > 100)
			throw new TransactionException("Jar file name too long");

		TransactionReference ref = getCurrentTransactionReference();
		for (Classpath dependency: dependencies)
			if (!dependency.transaction.isOlderThan(ref))
				throw new TransactionException("A transaction can only depend on older transactions");

		addJarStoreTransactionInternal(jar, path -> new JarInstrumentation(jar, path, mkProgram(jar, dependencies)), dependencies);

		moveToNextTransaction();
		return ref;
	}

	@Override
	public final StorageReference addConstructorCallTransaction(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		checkNotFull();

		CodeExecutor executor;
		Object[] deserializedActuals;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			Storage deserializedCaller = caller.deserialize(classLoader, this);
			checkIsExternallyOwned(deserializedCaller);
			deserializedActuals = deserialize(classLoader, actuals);
			executor = new ConstructorExecutor(classLoader, constructor, deserializedActuals);
			executor.start();
			executor.join();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		Storage newObject;
		SortedSet<Update> updates;

		try {
			newObject = (Storage) executor.result;
			updates = collectUpdates(deserializedActuals, null, null, newObject);
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		addConstructorCallTransactionInternal(caller, classpath, constructor, actuals, executor.exception == null ? StorageValue.serialize(newObject) : null, executor.exception, updates, events);

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Constructor threw exception", executor.exception);
		else
			return newObject.storageReference;
	}

	protected abstract void addConstructorCallTransactionInternal
		(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue[] actuals, StorageValue result, Throwable exception, SortedSet<Update> updates, List<String> events)
		throws TransactionException;

	@Override
	public final StorageReference addEntryConstructorCallTransaction(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		checkNotFull();

		CodeExecutor executor;
		Storage deserializedCaller;
		Object[] deserializedActuals;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			deserializedActuals = deserialize(classLoader, actuals);
			deserializedCaller = caller.deserialize(classLoader, this);
			checkIsExternallyOwned(deserializedCaller);
			executor = new EntryConstructorExecutor(classLoader, constructor, deserializedCaller, deserializedActuals);
			executor.start();
			executor.join();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		Storage newObject;
		SortedSet<Update> updates;

		try {
			newObject = (Storage) executor.result;
			updates = collectUpdates(deserializedActuals, deserializedCaller, null, newObject);
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		addEntryConstructorCallTransactionInternal(caller, classpath, constructor, actuals, executor.exception == null ? StorageValue.serialize(newObject) : null, executor.exception, updates, events);

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Constructor threw exception", executor.exception);
		else
			return newObject.storageReference;
	}

	protected abstract void addEntryConstructorCallTransactionInternal
		(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue[] actuals, StorageValue result, Throwable exception, SortedSet<Update> updates, List<String> events)
		throws TransactionException;

	@Override
	public final StorageValue addInstanceMethodCallTransaction(StorageReference caller, Classpath classpath, MethodReference method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		checkNotFull();

		CodeExecutor executor;
		Storage deserializedReceiver;
		Object[] deserializedActuals;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			Storage deserializedCaller = caller.deserialize(classLoader, this);
			checkIsExternallyOwned(deserializedCaller);
			deserializedReceiver = receiver.deserialize(classLoader, this);
			deserializedActuals = deserialize(classLoader, actuals);
			executor = new InstanceMethodExecutor(classLoader, method, deserializedReceiver, deserializedActuals);
			executor.start();
			executor.join();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		Object result;
		SortedSet<Update> updates;
		StorageValue serializedResult;

		try {
			result = executor.result;
			updates = collectUpdates(deserializedActuals, null, deserializedReceiver, result);
			serializedResult = executor.exception == null ? StorageValue.serialize(result) : null;
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		addInstanceMethodCallTransactionInternal(caller, classpath, method, receiver, actuals, serializedResult, executor.exception, updates, events);

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Method threw exception", executor.exception);
		else
			return serializedResult;
	}

	protected abstract void addInstanceMethodCallTransactionInternal(StorageReference caller, Classpath classpath, MethodReference method,
			StorageValue receiver, StorageValue[] actuals, StorageValue result, Throwable exception,
			SortedSet<Update> updates, List<String> events) throws TransactionException;

	@Override
	public final StorageValue addEntryInstanceMethodCallTransaction(StorageReference caller, Classpath classpath, MethodReference method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		checkNotFull();

		CodeExecutor executor;
		Storage deserializedReceiver, deserializedCaller;
		Object[] deserializedActuals;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			deserializedCaller = caller.deserialize(classLoader, this);
			checkIsExternallyOwned(deserializedCaller);
			deserializedReceiver = receiver.deserialize(classLoader, this);
			deserializedActuals = deserialize(classLoader, actuals);
			executor = new EntryInstanceMethodExecutor(classLoader, method, deserializedCaller, deserializedReceiver, deserializedActuals);
			executor.start();
			executor.join();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		Object result;
		SortedSet<Update> updates;
		StorageValue serializedResult;

		try {
			result = executor.result;
			updates = collectUpdates(deserializedActuals, deserializedCaller, deserializedReceiver, result);
			serializedResult = executor.exception == null ? StorageValue.serialize(result) : null;
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		addEntryInstanceMethodCallTransactionInternal(caller, classpath, method, receiver, actuals, serializedResult, executor.exception, updates, events);

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Method threw exception", executor.exception);
		else
			return serializedResult;
	}

	protected abstract void addEntryInstanceMethodCallTransactionInternal(StorageReference caller, Classpath classpath, MethodReference method,
			StorageValue receiver, StorageValue[] actuals, StorageValue result, Throwable exception,
			SortedSet<Update> updates, List<String> events) throws TransactionException;

	@Override
	public final StorageValue addStaticMethodCallTransaction(StorageReference caller, Classpath classpath, MethodReference method, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		checkNotFull();

		CodeExecutor executor;
		Object[] deserializedActuals;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			Storage deserializedCaller = caller.deserialize(classLoader, this);
			checkIsExternallyOwned(deserializedCaller);
			deserializedActuals = deserialize(classLoader, actuals);
			executor = new StaticMethodExecutor(classLoader, method, deserializedActuals);
			executor.start();
			executor.join();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		Object result;
		SortedSet<Update> updates;
		StorageValue serializedResult;

		try {
			result = executor.result;
			updates = collectUpdates(deserializedActuals, null, null, result);
			serializedResult = executor.exception == null ? StorageValue.serialize(result) : null;
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		addStaticMethodCallTransactionInternal(caller, classpath, method, actuals, serializedResult, executor.exception, updates, events);

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Method threw exception", executor.exception);
		else
			return serializedResult;
	}

	protected abstract void addStaticMethodCallTransactionInternal(StorageReference caller, Classpath classpath, MethodReference method,
			StorageValue[] actuals, StorageValue result, Throwable exception,
			SortedSet<Update> updates, List<String> events) throws TransactionException;

	public final Storage deserialize(BlockchainClassLoader classLoader, StorageReference reference) throws TransactionException {
		// this comparator puts updates in the order required for the parameter
		// of the deserialization constructor of storage objects: fields of superclasses first;
		// for the same class, fields are ordered by name and then by type
		Comparator<Update> updateComparator = new Comparator<Update>() {
	
			@Override
			public int compare(Update update1, Update update2) {
				FieldReference field1 = update1.field;
				FieldReference field2 = update2.field;
	
				try {
					String className1 = field1.definingClass.name;
					String className2 = field2.definingClass.name;
	
					if (className1.equals(className2)) {
						int diff = field1.name.compareTo(field2.name);
						if (diff != 0)
							return diff;
						else
							return field1.type.toString().compareTo(field2.type.toString());
					}
	
					Class<?> clazz1 = classLoader.loadClass(className1);
					Class<?> clazz2 = classLoader.loadClass(className2);
					if (clazz1.isAssignableFrom(clazz2)) // clazz1 superclass of clazz2
						return -1;
					else if (clazz2.isAssignableFrom(clazz1)) // clazz2 superclass of clazz1
						return 1;
					else
						throw new IllegalStateException("Updates are not on the same supeclass chain");
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
		};
	
		try {
			SortedSet<Update> updates = new TreeSet<>(updateComparator);
			collectUpdatesFor(reference, updates);
	
			Optional<Update> classTag = updates.stream()
					.filter(Update::isClassTag)
					.findAny();
	
			if (!classTag.isPresent())
				throw new TransactionException("No class tag found for " + reference);
	
			String className = classTag.get().field.definingClass.name;
			List<Class<?>> formals = new ArrayList<>();
			List<Object> actuals = new ArrayList<>();
			// the constructor for deserialization has a first parameter
			// that receives the storage reference of the object
			formals.add(StorageReference.class);
			actuals.add(reference);
	
			for (Update update: updates)
				if (!update.isClassTag()) {
					formals.add(update.field.type.toClass(classLoader));
					actuals.add(update.value.deserialize(classLoader, this));
				}
	
			Class<?> clazz = classLoader.loadClass(className);
			Constructor<?> constructor = clazz.getConstructor(formals.toArray(new Class<?>[formals.size()]));
			return (Storage) constructor.newInstance(actuals.toArray(new Object[actuals.size()]));
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Could not deserialize " + reference);
		}
	}

	public final Object deserializeLastUpdateFor(BlockchainClassLoader classLoader, StorageReference reference, FieldReference field) throws TransactionException {
		try {
			return getLastUpdateFor(reference, field).value.deserialize(classLoader, this);
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Could not deserialize " + reference);
		}
	}

	protected abstract Update getLastUpdateFor(StorageReference reference, FieldReference field) throws TransactionException;

	private void checkIsExternallyOwned(Storage deserializedCaller) {
		if (!deserializedCaller.getClass().getName().equals(EXTERNALLY_OWNED_ACCOUNT_NAME))
			throw new IllegalArgumentException("Only an externally owned contract can start a transaction");
	}

	/**
	 * Collects all updates reachable from the actual or from the caller, receiver or result of a method call.
	 * 
	 * @param deserializedActuals the actuals; only {@code Storage} are relevant
	 * @param caller the caller of an {@code @@Entry} method; this might be {@code null}
	 * @param receiver the receiver of the call; this might be {@code null}
	 * @param result the result; relevant only if {@code Storage}
	 * @return the ordered updates
	 */
	private static SortedSet<Update> collectUpdates(Object[] deserializedActuals, Storage caller, Storage receiver, Object result) {
		List<Storage> potentiallyAffectedObjects = new ArrayList<>();
		if (caller != null)
			potentiallyAffectedObjects.add(caller);
		if (receiver != null)
			potentiallyAffectedObjects.add(receiver);
		if (result instanceof Storage)
			potentiallyAffectedObjects.add((Storage) result);

		for (Object actual: deserializedActuals)
			if (actual instanceof Storage)
				potentiallyAffectedObjects.add((Storage) actual);

		Set<StorageReference> seen = new HashSet<>();
		SortedSet<Update> updates = new TreeSet<>();
		potentiallyAffectedObjects.forEach(storage -> storage.updates(updates, seen));

		return updates;
	}

	private abstract class CodeExecutor extends Thread {
		protected Throwable exception;
		protected Object result;
		protected final BlockchainClassLoader classLoader;

		private CodeExecutor(BlockchainClassLoader classLoader) {
			this.classLoader = classLoader;

			setContextClassLoader(new ClassLoader(classLoader.getParent()) {

				@Override
				public Class<?> loadClass(String name) throws ClassNotFoundException {
					return classLoader.loadClass(name);
				}
			});
		}
	}

	private class ConstructorExecutor extends CodeExecutor {
		private final ConstructorReference constructor;
		private final Object[] actuals;

		private ConstructorExecutor(BlockchainClassLoader classLoader, ConstructorReference constructor, Object... actuals) {
			super(classLoader);

			this.constructor = constructor;
			this.actuals = actuals;
		}

		@Override
		public void run() {
			try {
				Class<?> clazz = classLoader.loadClass(constructor.definingClass.name);
				Constructor<?> constructorJVM = clazz.getConstructor(formalsAsClass(classLoader, constructor));
				if (constructorJVM.isAnnotationPresent(Entry.class))
					throw new NoSuchMethodException("Cannot call an @Entry constructor: use addEntryConstructorCallTransaction instead");

				initTransaction(classLoader);
				result = ((Storage) constructorJVM.newInstance(actuals));
			}
			catch (InvocationTargetException e) {
				exception = e.getCause();
			}
			catch (Throwable t) {
				exception = wrapAsTransactionException(t, "Could not call the constructor");
			}
		}
	}

	private class EntryConstructorExecutor extends CodeExecutor {
		private final ConstructorReference constructor;
		private final Object[] actuals;

		private EntryConstructorExecutor(BlockchainClassLoader classLoader, ConstructorReference constructor, Storage caller, Object... actuals) {
			super(classLoader);

			this.constructor = constructor;
			this.actuals = addTrailingCaller(actuals, caller);
		}

		@Override
		public void run() {
			try {
				Class<?> clazz = classLoader.loadClass(constructor.definingClass.name);
				Constructor<?> constructorJVM = clazz.getConstructor(formalsAsClassWithTrailingContract(classLoader, constructor));
				if (!constructorJVM.isAnnotationPresent(Entry.class))
					throw new NoSuchMethodException("Can only call an @Entry constructor: use addConstructorCallTransaction instead");

				initTransaction(classLoader);
				result = ((Storage) constructorJVM.newInstance(actuals));
			}
			catch (InvocationTargetException e) {
				exception = e.getCause();
			}
			catch (Throwable t) {
				exception = wrapAsTransactionException(t, "Could not call the constructor");
			}
		}
	}

	private class InstanceMethodExecutor extends CodeExecutor {
		private final MethodReference method;
		private final Object receiver;
		private final Object[] actuals;

		private InstanceMethodExecutor(BlockchainClassLoader classLoader, MethodReference method, Object receiver, Object... actuals) {
			super(classLoader);

			this.method = method;
			this.receiver = receiver;
			this.actuals = actuals;
		}

		@Override
		public void run() {
			try {
				Class<?> clazz = classLoader.loadClass(method.definingClass.name);
				Method methodJVM = clazz.getMethod(method.methodName, formalsAsClass(classLoader, method));

				if (methodJVM.isAnnotationPresent(Entry.class))
					throw new NoSuchMethodException("Cannot call an @Entry method: use addEntryInstanceMethodCallTransaction instead");

				if (Modifier.isStatic(methodJVM.getModifiers()))
					throw new NoSuchMethodException("Cannot call a static method: use addStaticMethodCallTransaction instead");

				initTransaction(classLoader);
				result = methodJVM.invoke(receiver, actuals);
			}
			catch (InvocationTargetException e) {
				exception = e.getCause();
			}
			catch (Throwable t) {
				exception = wrapAsTransactionException(t, "Could not call the method");
			}
		}
	}

	private class EntryInstanceMethodExecutor extends CodeExecutor {
		private final MethodReference method;
		private final Object receiver;
		private final Object[] actuals;

		private EntryInstanceMethodExecutor(BlockchainClassLoader classLoader, MethodReference method, Storage caller, Storage receiver, Object... actuals) {
			super(classLoader);

			this.method = method;
			this.receiver = receiver;
			this.actuals = addTrailingCaller(actuals, caller);
		}

		@Override
		public void run() {
			try {
				Class<?> clazz = classLoader.loadClass(method.definingClass.name);
				Method methodJVM = clazz.getMethod(method.methodName, formalsAsClassWithTrailingContract(classLoader, method));

				if (Modifier.isStatic(methodJVM.getModifiers()))
					throw new NoSuchMethodException("Cannot call a static method: use addStaticMethodCallTransaction instead");

				if (!methodJVM.isAnnotationPresent(Entry.class))
					throw new NoSuchMethodException("Can only call an @Entry method: use addInstanceMethodCallTransaction instead");

				initTransaction(classLoader);
				result = methodJVM.invoke(receiver, actuals);
			}
			catch (InvocationTargetException e) {
				exception = e.getCause();
			}
			catch (Throwable t) {
				exception = wrapAsTransactionException(t, "Could not call the method");
			}
		}
	}

	private class StaticMethodExecutor extends CodeExecutor {
		private final MethodReference method;
		private final Object[] actuals;

		private StaticMethodExecutor(BlockchainClassLoader classLoader, MethodReference method, Object... actuals) {
			super(classLoader);

			this.method = method;
			this.actuals = actuals;
		}

		@Override
		public void run() {
			try {
				Class<?> clazz = classLoader.loadClass(method.definingClass.name);
				Method methodJVM = clazz.getMethod(method.methodName, formalsAsClass(classLoader, method));

				if (!Modifier.isStatic(methodJVM.getModifiers()))
					throw new NoSuchMethodException("Cannot call an instance method: use addInstanceMethodCallTransaction instead");

				initTransaction(classLoader);
				result = methodJVM.invoke(null, actuals);
			}
			catch (InvocationTargetException e) {
				exception = e.getCause();
			}
			catch (Throwable t) {
				exception = wrapAsTransactionException(t, "Could not call the method");
			}
		}
	}

	private Program mkProgram(Path jar, Classpath... dependencies) {
		List<Path> result = new ArrayList<>();
		result.add(jar);

		try {
			for (Classpath dependency: dependencies)
				extractPathsRecursively(dependency, result);

			return new Program(result.stream());
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot build the set of all classes in the class path", e);
		}
	}

	protected abstract void extractPathsRecursively(Classpath classpath, List<Path> result) throws IOException;

	protected abstract void collectUpdatesFor(StorageReference reference, Set<Update> where) throws TransactionException;

	protected abstract void addJarStoreTransactionInternal(Path jar, Consumer<Path> instrumentedJarCreator, Classpath... dependencies) throws TransactionException;

	protected abstract BlockchainClassLoader mkBlockchainClassLoader(Classpath classpath) throws TransactionException;

	protected abstract boolean blockchainIsFull();

	protected abstract void moveToNextTransaction();

	protected final static TransactionException wrapAsTransactionException(Throwable t, String message) {
		if (t instanceof TransactionException)
			return (TransactionException) t;
		else
			return new TransactionException(message, t);
	}

	private static Class<?>[] formalsAsClass(BlockchainClassLoader classLoader, CodeReference methodOrConstructor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(type.toClass(classLoader));
	
		return classes.toArray(new Class<?>[classes.size()]);
	}

	private static Class<?>[] formalsAsClassWithTrailingContract(BlockchainClassLoader classLoader, CodeReference methodOrConstructor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(type.toClass(classLoader));

		classes.add(classLoader.loadClass("takamaka.lang.Contract"));

		return classes.toArray(new Class<?>[classes.size()]);
	}

	private static Object[] addTrailingCaller(Object[] actuals, Storage caller) {
		Object[] result = new Object[actuals.length + 1];
		System.arraycopy(actuals, 0, result, 0, actuals.length);
		result[actuals.length] = caller;

		return result;
	}

	private Object[] deserialize(BlockchainClassLoader classLoader, StorageValue[] actuals) throws TransactionException {
		Object[] deserialized = new Object[actuals.length];
		for (int pos = 0; pos < actuals.length; pos++)
			deserialized[pos] = actuals[pos].deserialize(classLoader, this);
		
		return deserialized;
	}

	private void checkNotFull() throws TransactionException {
		if (blockchainIsFull())
			throw new TransactionException("No more transactions available in blockchain");
	}

	private void initTransaction(BlockchainClassLoader classLoader) {
		Storage.init(AbstractBlockchain.this, classLoader); // this blockchain will be used during the execution of the code
		events.clear();
	}

	/**
	 * Adds an event to those occurred during the execution of the last transaction.
	 * 
	 * @param event the event description
	 */
	public void event(String event) {
		events.add(event);
	}
}