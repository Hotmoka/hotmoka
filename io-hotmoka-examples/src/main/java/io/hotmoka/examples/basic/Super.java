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

package io.hotmoka.examples.basic;

import java.math.BigInteger;

import io.hotmoka.examples.basicdependency.InternationalTime;
import io.hotmoka.examples.basicdependency.Time;
import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public abstract class Super extends Contract {
	private String s;
	@SuppressWarnings("unused")
	private InternationalTime t;

	protected Super(int a) {}
	public @FromContract Super(boolean b) {}
	public @FromContract @Payable Super(int amount, int a, boolean b) {}
	public @FromContract void m1() {}
	public void m2() {}
	public abstract @FromContract void m3();
	public abstract @FromContract @Payable String m4(int amount);
	public abstract @FromContract @Payable String m4_1(long amount);
	public abstract @FromContract @Payable String m4_2(BigInteger amount);
	public @FromContract void print(Time time) {
		s = time.toString();
	}

	public @FromContract void storeItalian(Time time) {
		if (time instanceof InternationalTime)
			t = (InternationalTime) time;
	}

	@Override
	public String toString() {
		return s;
	}
}