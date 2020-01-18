package io.takamaka.code.blockchain.values;

import java.io.Serializable;

/**
 * A value that can be stored in the blockchain, passed as argument to an entry
 * or returned from an entry.
 */
public interface StorageValue extends Serializable, Comparable<StorageValue> {
}