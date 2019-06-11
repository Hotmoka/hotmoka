package takamaka.tests.lambdas;

import java.math.BigInteger;
import java.util.function.Function;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.ExternallyOwnedAccount;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.translator.Dummy;
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

	public @Payable @Entry Lambdas(BigInteger amount) {}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		Lambdas other = new Lambdas(BigInteger.TEN);
		BigInteger one = BigInteger.ONE;
		investors.add((PayableContract) caller());
		//investors.stream().forEach(investor -> investor.receive(MINIMUM_INVESTMENT));
		//investors.stream().forEach(investor -> investor.receive(one));
		investors.stream().forEach(other::entry1);
		investors.stream().forEach(investor -> other.entry1instrumented(investor, this, null));
		//investors.stream().forEach(investor -> other.entry2(other::entry3));
		//investors.stream().forEach(investor -> other.entry4(Lambdas::new));
	}

	public @Entry void entry1(PayableContract p) {
		p.receive(1);
	}

	public void entry1instrumented(PayableContract p, Contract caller, Dummy dummy) {
		p.receive(1);
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