package io.takamaka.tests.lambdas;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.util.StorageList;

/**
 * A test about lambda expressions that call @Entry methods.
 * Since such methods get an extra parameter at instrumentation time,
 * also the bootstrap methods of the lambdas and their bridge needs to
 * be modified. This class tests these situations.
 */
public class Lambdas extends ExternallyOwnedAccount {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<PayableContract> investors = new StorageList<>();
	private Lambdas other;
	private final BigInteger amount;

	public @Payable @Entry Lambdas(BigInteger amount) {
		this.amount = amount;
		this.investors.add((PayableContract) caller());
	}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		Lambdas other = new Lambdas(BigInteger.TEN);
		Lambdas other2 = new Lambdas(BigInteger.TEN);
		investors.add((PayableContract) caller());
		investors.stream().forEachOrdered(investor -> other.entry2(other2::entry3));
		investors.stream().forEachOrdered(investor -> other.entry4(Lambdas::new));
	}

	public void testLambdaWithoutThis() {
		BigInteger one = BigInteger.ONE;
		investors.stream().forEachOrdered(investor -> investor.receive(one));
	}

	public void testLambdaWithoutThisGetStatic() {
		investors.stream().forEachOrdered(investor -> investor.receive(BigInteger.ONE));
	}

	public void testLambdaWithThis() {
		investors.stream().forEachOrdered(investor -> investor.receive(MINIMUM_INVESTMENT));
	}

	public int testMethodReferenceToEntry() {
		other = new Lambdas(BigInteger.TEN);
		return Stream.of(BigInteger.TEN, BigInteger.ONE).map(other::through).mapToInt(BigInteger::intValue).sum();
	}

	public void testMethodReferenceToEntryOfOtherClass() {
		PayableContract investor = investors.first();
		Stream.of(BigInteger.TEN, BigInteger.ONE).forEachOrdered(investor::receive);
	}

	public int testMethodReferenceToEntrySameContract() {
		other = new Lambdas(BigInteger.TEN);
		return Stream.of(BigInteger.TEN, BigInteger.ONE).map(this::through).mapToInt(BigInteger::intValue).sum();
	}

	public int testConstructorReferenceToEntry() {
		return Stream.of(BigInteger.TEN, BigInteger.ONE)
			.map(Lambdas::new)
			.map(Lambdas::getAmount)
			.mapToInt(BigInteger::intValue).sum();
	}

	public @Entry void testConstructorReferenceToEntryPopResult() {
		Stream.of(BigInteger.TEN, BigInteger.ONE)
			.forEachOrdered(Lambdas::new);
	}

	private BigInteger getAmount() {
		return amount;
	}

	public @Entry BigInteger through(BigInteger bi) {
		return bi;
	}

	public @Entry void entry2(Function<Lambdas, Lambdas> fun) {
		fun.apply(this).receive(2);
	}

	public void nonentry(Function<Lambdas, Lambdas> fun) {
		fun.apply(this).receive(2);
	}

	public @Entry Lambdas entry3(Lambdas p) {
		p.receive(3);
		return this;
	}

	public @Entry void entry4(Function<BigInteger, Lambdas> fun) {
		fun.apply(BigInteger.ONE).receive(4);
	}

	public int whiteListChecks(Object o1, Object o2, Object o3) {
		return Stream.of(o1, o2, o3)
			.map(Objects::toString) // the parameter of this lambda must be checked at run time
			.collect(Collectors.joining())
			.length();
	}

	public String concatenation(String s1, Object s2, Lambdas s3, long s4, int s5) {
		return s1 + s2 + s3 + s4 + s5; // this generates (from Java 8) optimized code with invokedynamic
	}
}