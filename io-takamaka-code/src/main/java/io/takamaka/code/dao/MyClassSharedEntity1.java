package io.takamaka.code.dao;

import java.math.BigInteger;

/**
 * A simple implementation of a shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * Selling and buying shares do not require to pay a ticket. Shareholders have type {@link MyClass}.
 * This implementation is bugged since the method {@link #accept(BigInteger, io.takamaka.code.lang.PayableContract, io.takamaka.code.dao.SharedEntity3.Offer)}
 * is not redefined here, which allows anyone to become a shareholder, not just instances of {@link MyClass}.
 * 
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class MyClassSharedEntity1<O extends SharedEntity3.Offer<MyClass>> extends SimpleSharedEntity3<MyClass, O> {

	public MyClassSharedEntity1(MyClass[] shareholders, BigInteger[] shares) {
		super(shareholders, shares);
	}

	public MyClassSharedEntity1(MyClass shareholder, BigInteger share) {
		super(shareholder, share);
	}

	public MyClassSharedEntity1(MyClass shareholder1, MyClass shareholder2, BigInteger share1, BigInteger share2) {
		super(shareholder1, shareholder2, share1, share2);
	}

	public MyClassSharedEntity1(MyClass shareholder1, MyClass shareholder2, MyClass shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
		super(shareholder1, shareholder2, shareholder3, share1, share2, share3);
	}

	public MyClassSharedEntity1(MyClass shareholder1, MyClass shareholder2, MyClass shareholder3, MyClass shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
		super(shareholder1, shareholder2, shareholder3, shareholder4, share1, share2, share3, share4);
	}
}