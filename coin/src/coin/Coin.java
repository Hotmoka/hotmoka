package coin;

import static takamaka.lang.Takamaka.event;
import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Event;
import takamaka.util.StorageMap;

public class Coin extends Contract {
	private final Contract minter;
	private final StorageMap<Contract, BigInteger> balances = new StorageMap<>(__ -> BigInteger.ZERO);

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
        require(amount < 1000, "You can mint up to 1000 coins at a time");
        balances.put(receiver, balances.get(receiver).add(BigInteger.valueOf(amount)));
    }

	public @Entry void send(Contract receiver, BigInteger amount) {
		require(balances.get(caller()).compareTo(amount) <= 0, "Insufficient balance");
		balances.put(caller(), balances.get(caller()).subtract(amount));
        balances.put(receiver, balances.get(receiver).add(amount));
        event(new Send(caller(), receiver, amount));
    }
}