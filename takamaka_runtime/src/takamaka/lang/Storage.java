package takamaka.lang;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.FieldReference;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.Update;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.ByteValue;
import takamaka.blockchain.values.CharValue;
import takamaka.blockchain.values.DoubleValue;
import takamaka.blockchain.values.FloatValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.NullValue;
import takamaka.blockchain.values.ShortValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;

public abstract class Storage {
	public final StorageReference storageReference;
	protected final boolean inStorage;
	private static AbstractBlockchain blockchain;
	private static BlockchainClassLoader classLoader;
	private static short nextProgressive;

	/**
	 * Resets static data at the beginning of a transaction.
	 * 
	 * @param blockchain the blockchain used for the new transaction
	 */
	public static void init(AbstractBlockchain blockchain, BlockchainClassLoader classLoader) {
		Storage.blockchain = blockchain;
		Storage.classLoader = classLoader;
		nextProgressive = 0;
	}
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

	protected final void event(String tag, Object... objects) {
		blockchain.event(tag + ": " + Arrays.toString(objects));
	}

	/**
	 * Takamaka calls this to collect the updates to this object and to
	 * the objects that are reachable from it.
	 * 
	 * @param result the set where the updates will be added
	 * @param seen a set of storage references that have already been scanned
	 * @return The updates.
	 */
	public final void updates(Set<Update> result, Set<StorageReference> seen) {
		if (seen.add(storageReference)) {
			List<Storage> workingSet = new ArrayList<>(16);
			workingSet.add(this);

			do {
				workingSet.remove(workingSet.size() - 1).extractUpdates(result, seen, workingSet);
			}
			while (!workingSet.isEmpty());
		}
	}

	/**
	 * Collects the updates to this object and to those reachable from it.
	 * Storage classes will redefine this method to include updates to all their fields.
	 * 
	 * @param updates the set where storage updates will be collected
	 * @param seen the storage references of the objects already considered during the scan of the storage
	 * @param workingSet the list of storage objects that still need to be processed. This can get enlarged by a call to this method,
	 *                   in order to simulate recursive calls without risking a Java stack overflow
	 */
	protected void extractUpdates(Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet) {
		if (!inStorage)
			updates.add(Update.mkForClassTag(storageReference, getClass().getName()));

		// subclasses will override, call this super-implementation and add potential updates to their instance fields
	}

	/**
	 * Utility method that will be used in subclasses to implement
	 * method extractUpdates to recur on the old value of fields of reference type.
	 */
	protected final void recursiveExtract(Object s, Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet) {
		if (s instanceof Storage) {
			if (seen.add(((Storage) s).storageReference))
				workingSet.add((Storage) s);
		}
		else if (s instanceof String || s instanceof BigInteger) {} // these types are not recursively followed
		else if (s != null)
			throw new RuntimeException("a field of a storage object cannot hold a " + s.getClass().getName());
	}

	protected final Object deserializeLastUpdateFor(String definingClass, String name, String className) throws TransactionException {
		return blockchain.deserializeLastUpdateFor(classLoader, storageReference, new FieldReference(definingClass, name, className));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet, String fieldClassName, Object s) {
		// these values are not recursively followed
		FieldReference field = new FieldReference(fieldDefiningClass, fieldName, fieldClassName);

		if (s == null)
			updates.add(Update.mk(storageReference, field, NullValue.INSTANCE));
		else if (s instanceof Storage) {
			Storage storage = (Storage) s;

			if (seen.add(storage.storageReference)) {
				// general case, recursively followed
				updates.add(Update.mk(storageReference, field, storage.storageReference));
				workingSet.add(storage);
			}
			else
				updates.add(Update.mk(storageReference, field, storageReference));
		}
		else
			throw new RuntimeException("field " + field + " of a storage class cannot hold a " + s.getClass().getName());
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

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, String s) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, ClassType.STRING), s == null ? NullValue.INSTANCE : new StringValue(s)));
	}

	protected final void addUpdateFor(String fieldDefiningClass, String fieldName, Set<Update> updates, BigInteger bi) {
		updates.add(Update.mk(storageReference, new FieldReference(fieldDefiningClass, fieldName, ClassType.BIG_INTEGER), bi == null ? NullValue.INSTANCE : new BigIntegerValue(bi)));
	}
}