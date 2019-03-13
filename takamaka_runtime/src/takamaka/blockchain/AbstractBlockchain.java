package takamaka.blockchain;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import takamaka.blockchain.types.StorageType;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Storage;
import takamaka.translator.Program;

public abstract class AbstractBlockchain implements Blockchain {
	protected long currentBlock;
	protected short currentTransaction;

	@Override
	public final TransactionReference getCurrentTransactionReference() {
		return new TransactionReference(currentBlock, currentTransaction);
	}

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
					Class<?> clazz1 = classLoader.loadClass(field1.definingClass.name);
					Class<?> clazz2 = classLoader.loadClass(field2.definingClass.name);
					if (clazz1.isAssignableFrom(clazz2)) // clazz1 superclass of clazz2
						return -1;
					else if (clazz2.isAssignableFrom(clazz1)) // clazz2 superclass of clazz1
						return 1;
					else if (clazz1 == clazz2) {
						int diff = field1.name.compareTo(field2.name);
						if (diff != 0)
							return diff;
						else
							return field1.type.toString().compareTo(field2.type.toString());
					}
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
		catch (TransactionException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new TransactionException("Could not deserialize " + reference, t);
		}
	}

	@Override
	public Object deserializeLastUpdateFor(StorageReference reference, FieldReference field) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final TransactionReference addJarStoreTransaction(Path jar, Classpath... dependencies) throws TransactionException {
		checkNotFull();

		TransactionReference ref = getCurrentTransactionReference();
		for (Classpath dependency: dependencies)
			if (!dependency.transaction.isOlderThan(ref))
				throw new TransactionException("A transaction can only depend on older transactions");

		addJarStoreTransactionInternal(jar, dependencies);

		moveToNextTransaction();
		return ref;
	}

	@Override
	public final StorageReference addConstructorCallTransaction(Classpath classpath, ConstructorReference constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException {
		checkNotFull();

		CodeExecutor executor;
		Object[] deserializedActuals;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			deserializedActuals = deserialize(classLoader, actuals);
			executor = new ConstructorExecutor(classLoader, constructor, deserializedActuals);
			executor.start();
			executor.join();
		}
		catch (TransactionException e) {
			throw e;
		}
		catch (InterruptedException e) {
			throw new TransactionException("The transaction executor thread was unexpectedly interrupted");
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		Storage newObject;
		SortedSet<Update> updates;

		try {
			newObject = (Storage) executor.result;
			updates = collectUpdates(deserializedActuals, newObject);
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		addConstructorCallTransactionInternal(classpath, constructor, actuals, StorageValue.serialize(newObject), executor.exception, updates);

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Constructor threw exception", executor.exception);
		else
			return newObject.storageReference;
	}

	/**
	 * Collects all updates reachable from the actual or from the result of a method call.
	 * 
	 * @param deserializedActuals the actuals; only {@code Storage} are relevant
	 * @param result the result; relevant only if {@code Storage}
	 * @return the ordered updates
	 */
	private static SortedSet<Update> collectUpdates(Object[] deserializedActuals, Object result) {
		List<Storage> potentiallyAffectedObjects = new ArrayList<>();
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
			Constructor<?> constructorJVM;

			try {
				Class<?> clazz = classLoader.loadClass(constructor.definingClass.name);
				constructorJVM = clazz.getConstructor(formalsAsClass(classLoader, constructor));
				Storage.blockchain = AbstractBlockchain.this; // this blockchain will be used during the execution of the code
			}
			catch (Throwable e) {
				exception = new TransactionException("Could not call the constructor", e);
				return;
			}

			try {
				result = ((Storage) constructorJVM.newInstance(actuals));
			}
			catch (InvocationTargetException e) {
				exception = e.getCause();
			}
			catch (Throwable e) {
				exception = new TransactionException("Could not call the constructor", e);
			}
		}
	}

	protected final Program mkProgram(Path jar, Classpath... dependencies) throws TransactionException {
		List<Path> result = new ArrayList<>();
		result.add(jar);

		for (Classpath dependency: dependencies)
			extractPathsRecursively(dependency, result);

		try {
			return new Program(result.stream());
		}
		catch (IOException e) {
			throw new TransactionException("Cannot build set of all classes in class path", e);
		}
	}

	protected abstract void extractPathsRecursively(Classpath classpath, List<Path> result) throws TransactionException;

	protected abstract void collectUpdatesFor(StorageReference reference, Set<Update> where) throws TransactionException;

	protected abstract void addJarStoreTransactionInternal(Path jar, Classpath... dependencies) throws TransactionException;

	protected abstract void addConstructorCallTransactionInternal
		(Classpath classpath, ConstructorReference constructor, StorageValue[] actuals, StorageValue result, Throwable exception, SortedSet<Update> updates)
		throws TransactionException;

	protected abstract BlockchainClassLoader mkBlockchainClassLoader(Classpath classpath) throws TransactionException;

	protected abstract boolean blockchainIsFull();

	protected abstract void moveToNextTransaction();

	private Class<?>[] formalsAsClass(BlockchainClassLoader classLoader, CodeReference methodOrConstructor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(type.toClass(classLoader));
	
		return classes.toArray(new Class<?>[classes.size()]);
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
}