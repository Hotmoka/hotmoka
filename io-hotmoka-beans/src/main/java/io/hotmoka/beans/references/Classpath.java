package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A class path, that points to a given jar in the blockchain.
 */

@Immutable
public final class Classpath implements Serializable {

	private static final long serialVersionUID = -9014808081346651444L;

	/**
	 * The transaction that stored the jar.
	 */
	public final TransactionReference transaction;

	/**
	 * True if the dependencies of the jar must be included in the class path.
	 */
	public final boolean recursive;

	/**
	 * Builds a class path.
	 * 
	 * @param transaction The transaction that stored the jar
	 * @param recursive True if the dependencies of the jar must be included in the class path
	 */
	public Classpath(TransactionReference transaction, boolean recursive) {
		this.transaction = transaction;
		this.recursive = recursive;
	}

	@Override
	public String toString() {
		return transaction + (recursive ? " recursively revolved" : " non recursively resolved");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Classpath && ((Classpath) other).transaction.equals(transaction) && ((Classpath) other).recursive == recursive;
	}

	@Override
	public int hashCode() {
		return transaction.hashCode();
	}

	/**
	 * Marshals this classpath into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the classpath cannot be marshalled
	 */
	void into(ObjectOutputStream oos) throws IOException {
		if (recursive)
			oos.writeByte(1);
		else
			oos.writeByte(0);

		transaction.into(oos);
	}

	/**
	 * Factory method that unmarshals a classpath from the given stream.
	 * 
	 * @param ois the stream
	 * @return the classpath
	 * @throws IOException if the classpath could not be unmarshalled
	 * @throws ClassNotFoundException if the classpath could not be unmarshalled
	 */
	static Classpath from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte kind = ois.readByte();

		if (kind == 0)
			return new Classpath(TransactionReference.from(ois), false);
		else if (kind == 1)
			return new Classpath(TransactionReference.from(ois), true);
		else
			throw new IOException("unexpected classpath kind: " + kind);
	}
}