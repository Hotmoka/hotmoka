package takamaka.blockchain;

import java.util.jar.JarFile;

import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Storage;

public interface Blockchain {
	public TransactionReference getCurrentTransactionReference();
	public Storage deserialize(StorageReference reference);
	/**
	 * Deserializes the value of the given reference type field from blockchain.
	 * 
	 * @param reference the storage reference of the object holding the field
	 * @param field the field
	 * @return the value of the field. This might be a {@code Storage} but also some special types
	 *         such as {@code String} and {@code BigInteger}
	 */
	public Object deserializeLastUpdateFor(StorageReference reference, FieldReference field);
	public TransactionReference addJarStoreTransaction(JarFile jar, Classpath... dependencies) throws TransactionException;
	public TransactionReference addCodeExecutionTransaction(Classpath classpath, CodeReference sig, StorageValue[] pars) throws TransactionException;
}