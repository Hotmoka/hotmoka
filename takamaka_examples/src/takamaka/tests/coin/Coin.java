package takamaka.tests.coin;

import static takamaka.lang.Takamaka.event;
import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Event;
import takamaka.util.StorageMap;

public class Coin extends Contract {
	private final Contract minter;
	private final StorageMap<Contract, BigInteger> balances = new StorageMap<>();

	public static class Send extends Event {
		public final Contract caller;
		public final Contract receiver;
		public final BigInteger amount;

		private Send(Contract caller, Contract receiver, BigInteger amount) {
			this.caller = caller;
			this.receiver = receiver;
			this.amount = amount;
		}
	}

	public @Entry Coin() {
		minter = caller();
    }

	public @Entry void mint(Contract receiver, int amount) {
        require(caller() == minter, "Only the minter can mint new coins");
        require(amount > 0, "You can only mint a positive amount of coins");
        require(amount < 1000, "You can mint up to 1000 coins at a time");
        balances.update(receiver, BigInteger.ZERO, old -> old.add(BigInteger.valueOf(amount)));
    }

	public @Entry void send(Contract receiver, BigInteger amount) {
		BigInteger myAmount = balances.get(caller());
		require(myAmount != null && myAmount.compareTo(amount) >= 0, "Insufficient balance");
		balances.put(caller(), myAmount.subtract(amount));
        balances.update(receiver, BigInteger.ZERO, old -> old.add(amount));
        event(new Send(caller(), receiver, amount));
    }
}