package coin;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.util.StorageMap;

public class Coin extends Contract {
	private final Contract minter;
	private final StorageMap<Contract, BigInteger> balances = new StorageMap<>(__ -> BigInteger.ZERO);

	public @Entry Coin() {
		minter = payer();
    }

	public @Entry void mint(Contract receiver, int amount) {
        require(payer() == minter, "Only the minter can mint new coins");
        require(amount < 1000, "You can mint up to 1000 coins at a time");
        balances.put(receiver, balances.get(receiver).add(BigInteger.valueOf(amount)));
    }

	public @Entry void send(Contract receiver, int amount) {
		BigInteger amountAsBigInteger = BigInteger.valueOf(amount);
		require(balances.get(payer()).compareTo(amountAsBigInteger) <= 0, "Insufficient balance.");
		balances.put(payer(), balances.get(payer()).subtract(amountAsBigInteger));
        balances.put(receiver, balances.get(receiver).add(amountAsBigInteger));
        log("Send", payer(), receiver, amount);
    }
}