package io.hotmoka.tests.sharedentities;

import java.math.BigInteger;

import io.takamaka.code.dao.SharedEntity;
import io.takamaka.code.dao.SimpleSharedEntity;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

/**
 * A simple implementation of a shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * Selling and buying shares do not require to pay a ticket. Shareholders have type {@link MyClass}.
 * This implementation is correct since the method {@link #accept(BigInteger, io.takamaka.code.lang.PayableContract, io.takamaka.code.dao.SharedEntity.Offer)}
 * is redefined here, which only allows instances of {@link MyClass} to become shareholders.
 * 
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class MyClassSharedEntity2<O extends SharedEntity.Offer<MyClass>> extends SimpleSharedEntity<MyClass, O> {


	public MyClassSharedEntity2(MyClass[] shareholders, BigInteger[] shares) {
		super(shareholders, shares);
	}

	public MyClassSharedEntity2(MyClass shareholder, BigInteger share) {
		super(shareholder, share);
	}

	public MyClassSharedEntity2(MyClass shareholder1, MyClass shareholder2, BigInteger share1, BigInteger share2) {
		super(shareholder1, shareholder2, share1, share2);
	}

	public MyClassSharedEntity2(MyClass shareholder1, MyClass shareholder2, MyClass shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
		super(shareholder1, shareholder2, shareholder3, share1, share2, share3);
	}

	public MyClassSharedEntity2(MyClass shareholder1, MyClass shareholder2, MyClass shareholder3, MyClass shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
		super(shareholder1, shareholder2, shareholder3, shareholder4, share1, share2, share3, share4);
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, MyClass buyer, O offer) {
		// it is important to redefine this method, since it will let the compiler create a bridge method
		// for buyer of type "PayableContract" that overrides the same method from the superclass,
		// casts the buyer parameter to MyClass and calls this method
		super.accept(amount, buyer, offer);
	}
}