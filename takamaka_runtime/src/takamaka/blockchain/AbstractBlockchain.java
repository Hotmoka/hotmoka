package takamaka.blockchain;

import java.util.jar.JarFile;

import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Storage;

public abstract class AbstractBlockchain implements Blockchain {
	protected long currentBlock;
	protected short currentTransaction;

	@Override
	public final TransactionReference getCurrentTransactionReference() {
		return new TransactionReference(currentBlock, currentTransaction);
	}

	@Override
	public Storage deserialize(StorageReference reference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object deserializeLastUpdateFor(StorageReference reference, FieldReference field) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final TransactionReference addJarStoreTransaction(JarFile jar, Classpath... dependencies) throws TransactionException {
		checkNotFull();

		TransactionReference ref = getCurrentTransactionReference();
		for (Classpath dependency: dependencies)
			if (!dependency.transaction.isOlderThan(ref))
				throw new TransactionException("A transaction can only depend on older transactions");

		addJarStoreTransactionInternal(jar, dependencies);

		moveToNextTransaction();
		return ref;
	}

	protected abstract void addJarStoreTransactionInternal(JarFile jar, Classpath... dependencies) throws TransactionException;

	@Override
	public final StorageValue addCodeExecutionTransaction(Classpath classpath, CodeReference sig, StorageValue... pars) throws TransactionException {
		checkNotFull();

		CodeExecutor executor;
		try (BlockchainClassLoader classLoader = mkBlockchainClassLoader(classpath)) {
			executor = new CodeExecutor(classLoader);
			executor.start();
			executor.join();
		}
		catch (InterruptedException e) {
			throw new TransactionException("The transaction executor thread was unexpectedly interrupted");
		}
		catch (Exception e) {
			throw new TransactionException("Cannot close the blockchain classloader");
		}

		moveToNextTransaction();
		return null;
	}

	private class CodeExecutor extends Thread {
		private CodeExecutor(BlockchainClassLoader classLoader) {
			setContextClassLoader(new ClassLoader(classLoader.getParent()) {

				@Override
				public Class<?> loadClass(String name) throws ClassNotFoundException {
					return classLoader.loadClass(name);
				}
			});
		}

		@Override
		public void run() {
			try {
				System.out.println(getContextClassLoader().loadClass("takamaka.tests.ItalianTime"));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract BlockchainClassLoader mkBlockchainClassLoader(Classpath classpath) throws TransactionException;

	protected abstract boolean blockchainIsFull();

	protected abstract void moveToNextTransaction();

	private void checkNotFull() throws TransactionException {
		if (blockchainIsFull())
			throw new TransactionException("No more transactions available in blockchain");
	}

}
