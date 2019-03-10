package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;

public interface StorageValue {
	Object deserialize(Blockchain blockchain) throws TransactionException;
}