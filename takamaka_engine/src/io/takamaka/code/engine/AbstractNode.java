package io.takamaka.code.engine;

import java.security.CodeSource;
import java.util.Optional;
import java.util.concurrent.Callable;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class AbstractNode implements Node {

	/**
	 * The gas cost model of this blockchain.
	 */
	public final GasCostModel gasCostModel = mkGasCostModel(); //TODO: too visible

	@Override
	public GasCostModel mkGasCostModel() {
		return GasCostModel.standard();
	}

	/**
	 * Yields the transaction reference that installed the jar
	 * where the given class is defined.
	 * 
	 * @param clazz the class
	 * @return the transaction reference
	 * @throws IllegalStateException if the transaction reference cannot be determined
	 */
	public final TransactionReference transactionThatInstalledJarFor(Class<?> clazz) {
		String className = clazz.getName();
		CodeSource src = clazz.getProtectionDomain().getCodeSource();
		if (src == null)
			throw new IllegalStateException("Cannot determine the jar of class " + className);
		String classpath = src.getLocation().getPath();
		if (!classpath.endsWith(".jar"))
			throw new IllegalStateException("Unexpected class path " + classpath + " for class " + className);
		int start = classpath.lastIndexOf('@');
		if (start < 0)
			throw new IllegalStateException("Class path " + classpath + " misses @ separator");
		return getTransactionReferenceFor(classpath.substring(start + 1, classpath.length() - 4));
	}

	/**
	 * Yields the run-time class of the given object.
	 * 
	 * @param object the object
	 * @return the name of the class
	 * @throws DeserializationError if the class of the object cannot be found
	 */
	public final String getClassNameOf(StorageReference object) {
		try {
			TransactionResponse response = getResponseAt(object.transaction);
			if (response instanceof TransactionResponseWithUpdates) {
				Optional<ClassTag> classTag = ((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst();

				if (classTag.isPresent())
					return classTag.get().className;
			}
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}

		throw new DeserializationError("Did not find the class tag for " + object);
	}

	/**
	 * Calls the given callable. If if throws an exception, it wraps into into a {@link io.hotmoka.beans.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws TransactionException the wrapped exception
	 */
	protected static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
		try {
			return what.call();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message used for the {@link io.hotmoka.beans.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	protected static TransactionException wrapAsTransactionException(Throwable t, String message) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(message, t);
	}
}