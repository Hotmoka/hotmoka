package io.takamaka.code.dao;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.*;

import java.math.BigInteger;
import java.util.stream.Stream;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

/**
 * A simple implementation of a shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * Selling and buying shares do not require to pay a ticket.
 * 
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class MyClassSharedEntity<O extends SharedEntity3.Offer<MyClass>> extends SimpleSharedEntity3<O, MyClass> {


	public MyClassSharedEntity(MyClass[] shareholders, BigInteger[] shares) {
		super(shareholders, shares);
	}

	public MyClassSharedEntity(MyClass shareholder, BigInteger share) {
		super(shareholder, share);
	}

	public MyClassSharedEntity(MyClass shareholder1, MyClass shareholder2, BigInteger share1, BigInteger share2) {
		super(shareholder1, shareholder2, share1, share2);
	}

	public MyClassSharedEntity(MyClass shareholder1, MyClass shareholder2, MyClass shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
		super(shareholder1, shareholder2, shareholder3, share1, share2, share3);
	}

	public MyClassSharedEntity(MyClass shareholder1, MyClass shareholder2, MyClass shareholder3, MyClass shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
		super(shareholder1, shareholder2, shareholder3, shareholder4, share1, share2, share3, share4);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, MyClass buyer, O offer) {
		super.accept(amount, buyer, offer);
	}
}