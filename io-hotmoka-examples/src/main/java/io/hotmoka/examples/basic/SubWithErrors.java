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

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.StringSupport;

public class SubWithErrors extends Super {

	public @FromContract @Payable SubWithErrors(int amount) {
		super(amount > 10 ? 13 : 17); // ok
	}

	public @FromContract SubWithErrors(boolean b) {
		super(b);
	}

	public @FromContract SubWithErrors() {
		super(true);
	}

	@Override @FromContract
	public void m1() {
		super.m1();
	}

	@Override
	public void m2() {
		super.m2();
	}

	@Override @FromContract
	public void m3() {
	}

	@Override @Payable @FromContract
	public String m4(int amount) {
		return StringSupport.concat("Sub.m4 receives ", amount, " coins from ", caller());
	}

	@Override @Payable @FromContract
	public String m4_1(long amount) {
		return StringSupport.concat("Sub.m4_1 receives ", amount, " coins from ", caller());
	}

	@Override @Payable @FromContract
	public String m4_2(BigInteger amount) {
		return StringSupport.concat("Sub.m4_2 receives ", amount, " coins from ", caller());
	}

	public void m5() {
		super.m2(); // ok
	}
}