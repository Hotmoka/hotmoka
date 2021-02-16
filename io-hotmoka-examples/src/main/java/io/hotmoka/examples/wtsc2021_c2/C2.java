package io.hotmoka.examples.wtsc2021_c2;

import io.takamaka.code.lang.Contract;

public class C2 extends Contract {

	public void m() {
		Contract c = caller(); // error at deployment time
	}
}