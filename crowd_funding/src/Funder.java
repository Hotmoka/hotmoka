import takamaka.lang.Contract;

public class Funder {
	private final Contract who;
	private final int amount;

	public Funder(Contract who, int amount) {
		this.who = who;
		this.amount = amount;
	}
}