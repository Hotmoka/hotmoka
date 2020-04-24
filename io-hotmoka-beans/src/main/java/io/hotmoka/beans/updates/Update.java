package io.hotmoka.beans.updates;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.values.StorageReference;

/**
 * An update states that a property of an object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class Update implements Serializable, Comparable<Update> {

	private static final long serialVersionUID = 1921751386937488337L;

	/**
	 * The storage reference of the object whose field is modified.
	 */
	public final StorageReference object;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 */
	protected Update(StorageReference object) {
		this.object = object;
	}

	/**
	 * Yields the storage reference of the object whose field is modified.
	 * 
	 * @return the storage reference
	 */
	public final StorageReference getObject() {
		return object;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Update && ((Update) other).object.equals(object);
	}

	@Override
	public int hashCode() {
		return object.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return object.compareTo(other.object);
	}

	/**
	 * Determines if the information expressed by this update is set immediately
	 * when a storage object is deserialized from blockchain. Otherwise, the
	 * information will only be set on-demand.
	 * 
	 * @return true if and only if the information is eager
	 */
	public boolean isEager() {
		return true; // subclasses may redefine
	}

	/**
	 * Determines if this update carries information for the same property as another.
	 * 
	 * @param other the other update
	 * @return true if and only if that condition holds
	 */
	public boolean isForSamePropertyAs(Update other) {
		return getClass() == other.getClass() && object.equals(other.object);
	}

	/**
	 * Marshals this update into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the update cannot be marshalled
	 */
	public void into(ObjectOutputStream oos) throws IOException {
		object.into(oos);
	}

	/**
	 * Factory method that unmarshals an update from the given stream.
	 * 
	 * @param ois the stream
	 * @return the update
	 * @throws IOException if the update could not be unmarshalled
	 * @throws ClassNotFoundException if the update could not be unmarshalled
	 */
	static Update from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return null;
		//TODO
		/*
		byte selector = ois.readByte();
		String definingClass = ois.readUTF();
		int formalsCount = ois.readInt();
		StorageType[] formals = new StorageType[formalsCount];
		for (int pos = 0; pos < formalsCount; pos++)
			formals[pos] = StorageType.from(ois);

		switch (selector) {
		case ConstructorSignature.SELECTOR: return new ConstructorSignature(definingClass, formals);
		case VoidMethodSignature.SELECTOR: return new VoidMethodSignature(definingClass, ois.readUTF(), formals);
		case NonVoidMethodSignature.SELECTOR: return new NonVoidMethodSignature(definingClass, ois.readUTF(), StorageType.from(ois), formals);
		default: throw new IOException("unexpected code signature selector: " + selector);
		}
		*/
	}
}