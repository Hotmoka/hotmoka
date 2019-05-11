import java.math.BigInteger;

import takamaka.lang.Contract;

public class Funder {
	@SuppressWarnings("unused")
	private final Contract who;
	@SuppressWarnings("unused")
	private final BigInteger amount;

	public Funder(Contract who, BigInteger amount) {
		this.who = who;
		this.amount = amount;
	}
}