package io.hotmoka.tests.errors.illegalcalltofromcontract2;

public class Caller {

	public void m() {
		new C().entry(); // KO
	}
}