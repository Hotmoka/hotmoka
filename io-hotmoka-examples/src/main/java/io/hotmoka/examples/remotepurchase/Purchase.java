/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.examples.remotepurchase;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;

public class Purchase extends Contract {

	public class State extends Storage {
		private final String s;

		private State(String s) {
			this.s = s;
		}

		@Override
		public String toString() {
			return s;
		}
	}

	private final State Created = new State("CREATED");
	private final State Locked = new State("LOCKED");
	private final State Inactive = new State("INACTIVE");

    public static class Aborted extends Event {
		private @FromContract Aborted() {}
	}
	public static class PurchaseConfirmed extends Event {
		private @FromContract PurchaseConfirmed() {}
	}
	public static class ItemReceived extends Event {
		private @FromContract ItemReceived() {}
	}

	private final int value; // the value of the item that is sold
	private final PayableContract seller;
	private PayableContract buyer;
	private State state;

    // Ensure that the received money is an even number.
	public @Payable @FromContract(PayableContract.class) Purchase(int amount) {
		require(amount % 2 == 0, "You must deposit an even amount of money");
		seller = (PayableContract) caller();
        value = amount / 2;
        setState(Created);
    }

	private void isBuyer(Contract payer) {
        require(payer == buyer, "Only buyer can call this");
    }

	private void isSeller(Contract payer) {
        require(payer == seller, "Only seller can call this");
    }

	private void inState(State state) {
        require(this.state == state, "Invalid state");
    }

	private void setState(State state) {
		this.state = state;
	}

	/// Abort the purchase and reclaim the money.
    /// Can only be called by the seller before the contract is locked.
	public @FromContract void abort() {
        isSeller(caller());
        inState(Created);
        event(new Aborted());
        setState(Inactive);
        seller.receive(value * 2);
    }

    /// Confirm the purchase as buyer.
    /// Transaction has to include `2 * value` money.
    /// The money will be locked until confirmReceived is called.
	public @Payable @FromContract(PayableContract.class) void confirmPurchase(int amount) {
        inState(Created);
        require(amount == 2 * value, "amount must be twice as value");
        event(new PurchaseConfirmed());
        buyer = (PayableContract) caller();
        setState(Locked);
    }

    /// Confirm that you (the buyer) received the item.
    /// This will release the locked money of both parties.
	public @FromContract void confirmReceived() {
        isBuyer(caller());
        inState(Locked);
        event(new ItemReceived());
        setState(Inactive);
        buyer.receive(value);
        seller.receive(value * 3);
    }
}