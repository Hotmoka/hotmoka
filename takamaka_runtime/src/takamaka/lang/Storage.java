package takamaka.lang;

import java.util.HashSet;
import java.util.Set;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.StorageReference;
import takamaka.blockchain.Update;

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
	public Set<Update> extractUpdates() {
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
}