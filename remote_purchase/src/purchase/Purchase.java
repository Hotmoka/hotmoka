package purchase;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class Purchase extends Contract {
	private static enum State { Created, Locked, Inactive };

	private final int value; // the value of the item that is sold
	private final Contract seller;
	private Contract buyer;
	private State state;

    // Ensure that `msg.value` is an even number.
	public @Payable @Entry Purchase(int amount) {
		require(amount % 2 == 0, "You must deposit an even amount of money.");
		seller = payer();
        value = amount / 2;
        state = State.Created;
    }

	private void isBuyer(Contract payer) {
        require(payer == buyer, "Only buyer can call this.");
    }

	private void isSeller(Contract payer) {
        require(payer == seller, "Only seller can call this.");
    }

	private void inState(State state) {
        require(this.state == state, "Invalid state.");
    }

    /// Abort the purchase and reclaim the money.
    /// Can only be called by the seller before the contract is locked.
	public @Entry void abort() {
        isSeller(payer());
        inState(State.Created);
        log("Aborted.");
        state = State.Inactive;
        pay(seller, value * 2);
    }

    /// Confirm the purchase as buyer.
    /// Transaction has to include `2 * value` money.
    /// The money will be locked until confirmReceived is called.
	public @Payable @Entry void confirmPurchase(int amount) {
        inState(State.Created);
        require(amount == 2 * value);
        log("Purchase confirmed.");
        buyer = payer();
        state = State.Locked;
    }

    /// Confirm that you (the buyer) received the item.
    /// This will release the locked money of both parties.
	public @Entry void confirmReceived() {
        isBuyer(payer());
        inState(State.Locked);
        log("Item received.");
        state = State.Inactive;
        pay(buyer, value);
        pay(seller, value * 3);
    }
}