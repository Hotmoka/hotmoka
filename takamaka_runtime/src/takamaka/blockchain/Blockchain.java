package takamaka.blockchain;

import java.util.jar.JarFile;

import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Storage;

public interface Blockchain {
	public TransactionReference getCurrentTransactionReference();
	public Storage deserialize(StorageReference reference);
	public Storage deserializeLastUpdateFor(StorageReference reference, FieldReference field);
	public TransactionReference addJarStoreTransaction(JarFile jar, Classpath[] dependencies);
	public TransactionReference addCodeExecutionTransaction(Classpath classpath, CodeReference sig, StorageValue[] pars, StorageValue result, Update[] updates);
}