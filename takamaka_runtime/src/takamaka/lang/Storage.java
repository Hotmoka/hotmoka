package takamaka.lang;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.FieldReference;
import takamaka.blockchain.StorageReference;
import takamaka.blockchain.Update;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.ByteValue;
import takamaka.blockchain.values.CharValue;
import takamaka.blockchain.values.DoubleValue;
import takamaka.blockchain.values.FloatValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.ShortValue;
import takamaka.blockchain.values.StringValue;

public abstract class Storage {
	protected final StorageReference storageReference;
	protected final boolean inStorage;
	protected static Blockchain blockchain; //TODO = Blockchain.getInstance();
	private static short nextProgressive;

	/**
	 * Constructor used by the programmer to build objects not yet in storage
	 */
	protected Storage() {
		this.inStorage = false;
		this.storageReference = new StorageReference(blockchain.getCurrentTransactionReference(), nextProgressive++);
	}

	/**
	 * Constructor used by Takamaka for deserialisation from blockchain.
	 */
	protected Storage(StorageReference storageReference) {
		this.inStorage = true;
		this.storageReference = storageReference;
	}

	/**
	 * Takamaka calls this to collect the updates to this object and to
	 * the objects that are reachable from it.
	 * 
	 * @return The updates.
	 */
	public final Set<Update> extractUpdates() {
		Set<Update> result = new HashSet<>();
		Set<StorageReference> seen = new HashSet<>();
		seen.add(storageReference);
		extractUpdates(result, seen);
		
		return result;
	}

	/**
	 * Collects the updates to this object and to those reachable from it.
	 * Storage classes will redefine this method to include updates to all their fields.
	 * 
	 * @param updates the set where storage updates will be collected
	 * @param seen the storage references of the objects already considered during the scan of the storage
	 */
	protected void extractUpdates(Set<Update> updates, Set<StorageReference> seen) {
		if (!inStorage)
			updates.add(Update.mkForClassTag(storageReference, getClass().getName()));

		// subclasses will override and add updates to their instance fields
	}

	/**
	 * Utility method that will be used in subclasses to implement
	 * method extractUpdates to recur on the old value of fields of reference type.
	 */
	protected final void recursiveExtract(Object s, Set<Update> updates, Set<StorageReference> seen) {
		if (s instanceof Storage) {
			if (seen.add(((Storage) s).storageReference))
				((Storage) s).extractUpdates(updates, seen);
		}
		else if (s instanceof String || s instanceof BigInteger) {} // these types are not recursively followed
		else if (s != null)
			throw new RuntimeException("a field of a storage object cannot hold a " + s.getClass().getName());
	}

	protected final Object deserializeLastUpdateFor(String definingClass, String name, String className) {
		return blockchain.deserializeLastUpdateFor(storageReference, new FieldReference(definingClass, name, className));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, Set<StorageReference> seen, String fieldClassName, Object s) {
		// these values are not recursively followed
		if (s == null)
			updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, fieldClassName), null));
		else if (s instanceof String)
			updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, fieldClassName), new StringValue((String) s)));
		else if (s instanceof BigInteger)
			updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, fieldClassName), new BigIntegerValue((BigInteger) s)));
		else if (s instanceof Storage)
			if (seen.add(((Storage) s).storageReference)) {
				// general case, recursively followed
				updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, fieldClassName), ((Storage) s).storageReference));
				((Storage) s).extractUpdates(updates, seen);
			}
			else
				updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, fieldClassName), storageReference));
		else
			throw new RuntimeException("a field of a storage object cannot hold a " + s.getClass().getName());
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, boolean s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.BOOLEAN), new BooleanValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, byte s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.BYTE), new ByteValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, char s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.CHAR), new CharValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, double s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.DOUBLE), new DoubleValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, float s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.FLOAT), new FloatValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, int s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.INT), new IntValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, long s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.LONG), new LongValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, short s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.SHORT), new ShortValue(s)));
	}
}