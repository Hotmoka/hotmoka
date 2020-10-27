package io.takamaka.code.auxiliaries;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.UnsignedBigInteger;

import java.math.BigInteger;

/**
 * Implementation inspired by Counters - Matt Condon (@shrugs)
 *
 * Dev: Provides counter that can only be incremented or decremented by one.
 * TODO documenta le funzioni
 */
public class Counter extends Storage { //TODO Storage because it is only a library
    private UnsignedBigInteger _value = new UnsignedBigInteger(BigInteger.ZERO); // default: 0

    public @View UnsignedBigInteger current() {
        return _value;
    }

    public void increment() {
        _value = _value.add(new UnsignedBigInteger(BigInteger.ONE));
    }

    public void decrement() {
        _value = _value.subtract(new UnsignedBigInteger(BigInteger.ONE));
    }
}
