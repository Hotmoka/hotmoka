package purchase;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.LoggableContract;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;

public class Purchase extends LoggableContract {
	private static enum State { Created, Locked, Inactive };

	private final int value; // the value of the item that is sold
	private final PayableContract seller;
	private PayableContract buyer;
	private State state;

    // Ensure that the received money is an even number.
	public @Payable @Entry Purchase(int amount) {
		require(amount % 2 == 0, "You must deposit an even amount of money.");
		require(caller() instanceof PayableContract, "The caller must be payable");
		seller = (PayableContract) caller();
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
        isSeller(caller());
        inState(State.Created);
        log("Aborted.");
        state = State.Inactive;
        seller.receive(value * 2);
    }

    /// Confirm the purchase as buyer.
    /// Transaction has to include `2 * value` money.
    /// The money will be locked until confirmReceived is called.
	public @Payable @Entry void confirmPurchase(int amount) {
        inState(State.Created);
        require(amount == 2 * value, "amount must be twice as value");
        require(caller() instanceof PayableContract, "Buyer must be payable");
        log("Purchase confirmed.");
        buyer = (PayableContract) caller();
        state = State.Locked;
    }

    /// Confirm that you (the buyer) received the item.
    /// This will release the locked money of both parties.
	public @Entry void confirmReceived() {
        isBuyer(caller());
        inState(State.Locked);
        log("Item received.");
        state = State.Inactive;
        buyer.receive(value);
        seller.receive(value * 3);
    }
}