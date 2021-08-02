package io.hotmoka.crypto;

import io.hotmoka.beans.references.TransactionReference;

public class Account {
	private final byte[] entropy;
	public final TransactionReference transaction;
	
	public Account(byte[] entropy, TransactionReference transaction) {
		this.entropy = entropy.clone();
		this.transaction = transaction;
	}

	public byte[] getEntropy() {
		return entropy.clone();
	}
}