package takamaka.blockchain;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			executor = new ConstructorExecutor(classLoader, constructor, actuals);
			executor.start();
			executor.join();
		}
		catch (TransactionException e) {
			throw e;
		}
		catch (InterruptedException e) {
			throw new TransactionException("The transaction executor thread was unexpectedly interrupted");
		}
		catch (Throwable e) {
			throw new TransactionException("Cannot complete the transaction", e);
		}

		if (executor.exception instanceof TransactionException)
			throw (TransactionException) executor.exception;

		moveToNextTransaction();
		//TODO: store updates
		if (executor.exception != null)
			throw new CodeExecutionException("Constructor threw exception", executor.exception);
		else
			return (StorageReference) executor.result;
	}

	private abstract class CodeExecutor extends Thread {
		protected Throwable exception;
		protected StorageValue result;

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
		private final StorageValue[] actuals;

		private ConstructorExecutor(BlockchainClassLoader classLoader, ConstructorReference constructor, StorageValue... actuals) {
			super(classLoader);

			this.constructor = constructor;
			this.actuals = actuals;
		}

		@Override
		public void run() {
			Constructor<?> constructorJVM;
			Object[] deserializedActuals;

			try {
				Class<?> clazz = getContextClassLoader().loadClass(constructor.definingClass.name);
				constructorJVM = clazz.getConstructor(formalsAsClass(constructor));
				deserializedActuals = deserialize(actuals);
				Storage.blockchain = AbstractBlockchain.this; // this blockchain will be used during the execution of the code
			}
			catch (TransactionException e) {
				exception = e;
				return;
			}
			catch (Throwable e) {
				exception = new TransactionException("Could not call the constructor", e);
				return;
			}

			try {
				result = ((Storage) constructorJVM.newInstance(deserializedActuals)).storageReference;
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