package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract is a storage object with a balance of coin. It is controlled
 * by the methods of its code.
 */
@Exported
public abstract class Contract extends Storage {

	/**
	 * The balance of this contract.
	 */
	private BigInteger balance;

	/**
	 * Builds a contract with zero balance.
	 */
	protected Contract() {
		this.balance = BigInteger.ZERO;
	}

	/**
	 * Yields the balance of this contract.
	 * 
	 * @return the balance
	 */
	protected final BigInteger balance() {
		return balance;
	}

	@Override
	public String toString() {
		return "a contract";
	}

	/**
	 * Increases the balance of a contract by the given amount of coins,
	 * taken away from the balance of this contract.
	 * 
	 * @param beneficiary the beneficiary of the amount of coins
	 * @param amount the amount of coins
	 */
	private void pay(Contract beneficiary, BigInteger amount) {
		Takamaka.require(amount != null, "Payed amount cannot be null");
		Takamaka.require(amount.signum() >= 0, "Payed amount cannot be negative");
		if (balance.compareTo(amount) < 0)
			throw new InsufficientFundsError(amount);

		balance = balance.subtract(amount);
		beneficiary.balance = beneficiary.balance.add(amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable method or constructor.
	 * It sets the caller of the code and transfers the amount of coins to the contract.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param payer the payer
	 * @param amount the amount of coins
	 */
	private void payableFromContract(Contract payer, BigInteger amount) {
		payer.pay(this, amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable method or constructor.
	 * It sets the caller of the code and transfers the amount of coins to the contract.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 *
	 * @param payer the payer
	 * @param amount the amount of coins
	 */
	@SuppressWarnings("unused")
	private void payableFromContract(Contract payer, int amount) {
		payableFromContract(payer, BigInteger.valueOf(amount));
	}

	/**
	 * Called at the beginning of the instrumentation of a payable method or constructor.
	 * It sets the caller of the code and transfers the amount of coins to the contract.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param payer the payer
	 * @param amount the amount of coins
	 */
	@SuppressWarnings("unused")
	private void payableFromContract(Contract payer, long amount) {
		payableFromContract(payer, BigInteger.valueOf(amount));
	}
}