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
		extractUpdates(result);
		
		return result;
	}

	/**
	 * Collects the updates to this object and to those reachable from it.
	 * 
	 * @return The storage reference used for this object in blockchain.
	 */
	protected StorageReference extractUpdates(Set<Update> updates) {
		if (!inStorage)
			updates.add(Update.mkForClassTag(storageReference, getClass().getName()));

		// subclasses will override and add updates to their instance fields
		return storageReference;
	}

	/**
	 * Utility method that will be used in subclasses to implement
	 * method extractUpdates to recur on fields of reference type.
	 */
	protected final StorageReference recursiveExtract(Object s, Set<Update> updates) {
		if (s == null)
			return null;
		else if (s instanceof Storage)
			return ((Storage) s).extractUpdates(updates);
		else
			throw new RuntimeException("storage objects must implement Storage");
	}

	protected final Object deserializeLastUpdateFor(String definingClass, String name, String className) {
		return blockchain.deserializeLastUpdateFor(storageReference, new FieldReference(definingClass, name, className));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, String fieldClassName, Object s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, fieldClassName), recursiveExtract(s, updates)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, BigInteger s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.BIGINTEGER), new BigIntegerValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, String s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.STRING), new StringValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, boolean s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.BOOLEAN), new BooleanValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, byte s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.BYTE), new ByteValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, char s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.CHAR), new CharValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, double s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.DOUBLE), new DoubleValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, float s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.FLOAT), new FloatValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, int s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.INT), new IntValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, long s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.LONG), new LongValue(s)));
	}

	protected final void addUpdateFor(StorageReference storageReference, String fieldDefiningClass, String fieldName, Set<Update> updates, short s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, BasicTypes.SHORT), new ShortValue(s)));
	}
}