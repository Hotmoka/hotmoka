package takamaka.tests.lambdas;

import java.math.BigInteger;
import java.util.function.Function;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.util.StorageList;

/**
 * A test about lambda expressions that call @Entry methods.
 * Since such methods get an extra parameter at instrumentation time,
 * also the bootstrap methods of the lambdas and their bridge needs to
 * be modified. This class tests these situations.
 */
public class Lambdas extends PayableContract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<Lambdas> investors = new StorageList<>();

	public @Payable @Entry(Lambdas.class) Lambdas(BigInteger amount) {}

	public @Payable @Entry(Lambdas.class) void invest(BigInteger amount) {
		Lambdas other = new Lambdas(BigInteger.TEN);
		BigInteger one = BigInteger.ONE;
		investors.add((Lambdas) caller());
		System.out.println("FIRST");
		investors.stream().forEach(investor -> investor.receive(MINIMUM_INVESTMENT));
		System.out.println("SECOND");
		investors.stream().forEach(investor -> investor.receive(one));
		System.out.println("THIRD");
		investors.stream().forEach(other::entry1);
		System.out.println("FOUR");
		investors.stream().forEach(investor -> other.entry2(other::entry3));
		System.out.println("FIVE");
		investors.stream().forEach(investor -> other.entry4(Lambdas::new));
	}

	public @Entry void entry1(Lambdas p) {
		p.receive(1);
	}

	public @Entry void entry2(Function<Lambdas, Lambdas> fun) {
		fun.apply(this).receive(2);
	}

	public @Entry Lambdas entry3(Lambdas p) {
		p.receive(3);
		return this;
	}

	public @Entry void entry4(Function<BigInteger, Lambdas> fun) {
		fun.apply(BigInteger.ONE).receive(4);
	}
}