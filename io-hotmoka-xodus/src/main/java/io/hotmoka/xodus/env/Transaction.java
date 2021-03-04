package io.hotmoka.xodus.env;

public class Transaction {
	private final jetbrains.exodus.env.Transaction parent;

	Transaction(jetbrains.exodus.env.Transaction parent) {
		this.parent = parent;
	}

	public jetbrains.exodus.env.Transaction toNative() {
		return parent;
	}

	public boolean isFinished() {
		return parent.isFinished();
	}

	public boolean isIdempotent() {
		return parent.isIdempotent();
	}

	public void abort() {
		parent.abort();
	}

	public boolean commit() {
		return parent.commit();
	}
}