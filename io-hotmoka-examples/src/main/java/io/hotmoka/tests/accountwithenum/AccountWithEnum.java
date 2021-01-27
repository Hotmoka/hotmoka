package io.hotmoka.tests.accountwithenum;

import io.takamaka.code.lang.ExternallyOwnedAccount;

public class AccountWithEnum extends ExternallyOwnedAccount {
	private static enum MyEnum { SMALL, BIG };
	private final MyEnum me;

	public AccountWithEnum(String publicKey) {
		super(publicKey);

		this.me = publicKey.endsWith("a") ? MyEnum.BIG : MyEnum.SMALL;
	}

	public int ordinal() {
		return me.ordinal();
	}
}