package io.hotmoka.examples.inner;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

@Exported
public class TestInner extends Storage {

	public class Inside extends Contract {

		public @FromContract @Payable Inside(long amount) {
		}

		public @View BigInteger getBalance() {
			return balance();
		}

		public @View TestInner getParent() {
			return TestInner.this;
		}
	}
}