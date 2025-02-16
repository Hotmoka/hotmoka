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

package io.hotmoka.examples.lambdas;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

/**
 * A test about lambda expressions that call @Entry methods.
 * Since such methods get an extra parameter at instrumentation time,
 * also the bootstrap methods of the lambdas and their bridge needs to
 * be modified. This class tests these situations.
 */
public class Lambdas extends ExternallyOwnedAccount {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<PayableContract> investors = new StorageLinkedList<>();
	private Lambdas other;
	private final BigInteger amount;
	private final String publicKey;
	
	public @Payable @FromContract Lambdas(BigInteger amount, String publicKey) {
		super(publicKey);

		this.amount = amount;
		this.publicKey = publicKey;
		this.investors.add((PayableContract) caller());
	}

	private @Payable @FromContract Lambdas(BigInteger amount) {
		super("");

		this.amount = amount;
		this.publicKey = "";
		this.investors.add((PayableContract) caller());
	}

	public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
		var other = new Lambdas(BigInteger.TEN, publicKey);
		var other2 = new Lambdas(BigInteger.TEN, publicKey);
		investors.add((PayableContract) caller());
		investors.forEach(investor -> other.entry2(other2::entry3));
		investors.forEach(investor -> other.entry4(Lambdas::new));
	}

	public void testLambdaWithoutThis() {
		var one = BigInteger.ONE;
		investors.forEach(investor -> investor.receive(one));
	}

	public void testLambdaWithoutThisGetStatic() {
		investors.forEach(investor -> investor.receive(BigInteger.ONE));
	}

	public void testLambdaWithThis() {
		investors.forEach(investor -> investor.receive(MINIMUM_INVESTMENT));
	}

	public int testMethodReferenceToEntry() {
		other = new Lambdas(BigInteger.TEN, publicKey);
		return apply(other::through);
	}

	public void testMethodReferenceToEntryOfOtherClass() {
		PayableContract investor = investors.first();
		process(new BigInteger[] { BigInteger.TEN, BigInteger.ONE }, investor::receive);
	}

	public int testMethodReferenceToEntrySameContract() {
		other = new Lambdas(BigInteger.TEN, publicKey);
		return apply(this::through);
	}

	private static int apply(Function<BigInteger, BigInteger> fun) {
		int i = 0;
		BigInteger[] array = { BigInteger.TEN, BigInteger.ONE };
		for (BigInteger bi: array)
			i += fun.apply(bi).intValue();

		return i;
	}

	public int testConstructorReferenceToEntry() {
		return process(Lambdas::new, Lambdas::getAmount);
	}

	private static int process(Function<BigInteger, Lambdas> fun1, Function<Lambdas, BigInteger> fun2) {
		BigInteger[] array = { BigInteger.TEN, BigInteger.ONE };
		int sum = 0;
		for (var bi: array)
			sum += fun2.apply(fun1.apply(bi)).intValue();

		return sum;
	}

	public @FromContract void testConstructorReferenceToEntryPopResult() {
		process(new BigInteger[] { BigInteger.TEN, BigInteger.ONE}, Lambdas::new);
	}

	private static <T> void process(T[] arr, Consumer<T> consumer) {
		for (T bi: arr)
			consumer.accept(bi);
	}

	private BigInteger getAmount() {
		return amount;
	}

	public @FromContract BigInteger through(BigInteger bi) {
		return bi;
	}

	public @FromContract void entry2(Function<Lambdas, Lambdas> fun) {
		fun.apply(this).receive(2);
	}

	public void nonentry(Function<Lambdas, Lambdas> fun) {
		fun.apply(this).receive(2);
	}

	public @FromContract Lambdas entry3(Lambdas p) {
		p.receive(3);
		return this;
	}

	public @FromContract void entry4(Function<BigInteger, Lambdas> fun) {
		fun.apply(BigInteger.ONE).receive(4);
	}

	private static class WrappedString {
		private String s = "";
	}

	public int whiteListChecks(Object o1, Object o2, Object o3) {
		WrappedString ws = new WrappedString();
		process(new Object[] { o1, o2, o3 }, s -> ws.s = StringSupport.concat(ws.s, s));

		return ws.s.length();
	}

	public String concatenation(String s1, Object s2, Lambdas s3, long s4, int s5) {
		return StringSupport.concat(s1, s2, s3, s4, s5); // this generates (from Java 8) optimized code with invokedynamic
	}
}