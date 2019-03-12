package takamaka.blockchain;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.types.StorageType;
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

	@Override
	public Storage deserialize(StorageReference reference) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
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
			deserializedActuals = deserialize(actuals);
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

		try {
			newObject = (Storage) executor.result;
			collectUpdates(deserializedActuals, newObject);
		}
		catch (Throwable t) {
			throw new TransactionException("Cannot complete the transaction", t);
		}

		// the transaction was successful, regardless of the fact that the constructor might have thrown an exception,
		// hence we move further to the next transaction
		moveToNextTransaction();

		if (executor.exception != null)
			throw new CodeExecutionException("Constructor threw exception", executor.exception);

		return newObject.storageReference;
	}

	private static Stream<Update> collectUpdates(Object[] deserializedActuals, Object result) {
		List<Storage> potentiallyAffectedObjects = new ArrayList<>();
		if (result instanceof Storage)
			potentiallyAffectedObjects.add((Storage) result);

		for (Object actual: deserializedActuals)
			if (actual instanceof Storage)
				potentiallyAffectedObjects.add((Storage) actual);

		Set<StorageReference> seen = new HashSet<>();
		Set<Update> updates = new HashSet<>();
		potentiallyAffectedObjects.forEach(storage -> storage.updates(updates, seen));

		return updates.stream();
	}

	private abstract class CodeExecutor extends Thread {
		protected Throwable exception;
		protected Object result;

		private CodeExecutor(BlockchainClassLoader classLoader) {
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
				Class<?> clazz = getContextClassLoader().loadClass(constructor.definingClass.name);
				constructorJVM = clazz.getConstructor(formalsAsClass(constructor));
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

	protected abstract void addJarStoreTransactionInternal(Path jar, Classpath... dependencies) throws TransactionException;

	protected abstract BlockchainClassLoader mkBlockchainClassLoader(Classpath classpath) throws TransactionException;

	protected abstract boolean blockchainIsFull();

	protected abstract void moveToNextTransaction();

	private Class<?>[] formalsAsClass(CodeReference methodOrConstructor) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		for (StorageType type: methodOrConstructor.formals().collect(Collectors.toList()))
			classes.add(type.toClass());
	
		return classes.toArray(new Class<?>[classes.size()]);
	}

	private Object[] deserialize(StorageValue[] actuals) throws TransactionException {
		Object[] deserialized = new Object[actuals.length];
		for (int pos = 0; pos < actuals.length; pos++)
			deserialized[pos] = actuals[pos].deserialize(this);
		
		return deserialized;
	}

	private void checkNotFull() throws TransactionException {
		if (blockchainIsFull())
			throw new TransactionException("No more transactions available in blockchain");
	}
}