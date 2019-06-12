package takamaka.tests.lambdas;

import java.math.BigInteger;
import java.util.stream.Stream;

import takamaka.lang.Entry;
import takamaka.lang.ExternallyOwnedAccount;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.util.StorageList;

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
	}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		Lambdas other = new Lambdas(BigInteger.TEN);
		BigInteger one = BigInteger.ONE;
		investors.add((PayableContract) caller());
		//investors.stream().forEach(investor -> investor.receive(one));
		//investors.stream().forEach(investor -> other.entry2(other::entry3));
		//investors.stream().forEach(investor -> other.entry4(Lambdas::new));
	}

	public void testLambdaWithThis() {
		investors.stream().forEach(investor -> investor.receive(MINIMUM_INVESTMENT));
	}

	public int testMethodReferenceToEntry() {
		other = new Lambdas(BigInteger.TEN);
		return Stream.of(BigInteger.TEN, BigInteger.ONE).map(other::through).mapToInt(BigInteger::intValue).sum();
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
			.forEach(Lambdas::new);
	}

	private BigInteger getAmount() {
		return amount;
	}

	public @Entry BigInteger through(BigInteger bi) {
		return bi;
	}

	/*public @Entry void entry2(Function<Lambdas, Lambdas> fun) {
		fun.apply(this).receive(2);
	}

	public @Entry Lambdas entry3(Lambdas p) {
		p.receive(3);
		return this;
	}

	public @Entry void entry4(Function<BigInteger, Lambdas> fun) {
		fun.apply(BigInteger.ONE).receive(4);
	}*/
}