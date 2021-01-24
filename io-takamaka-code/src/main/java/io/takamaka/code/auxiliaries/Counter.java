package io.takamaka.code.auxiliaries;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.UnsignedBigInteger;

import java.math.BigInteger;

/**
 * Implementation inspired by Counters - Matt Condon (@shrugs)
 *
 * Dev: Provides counter that can only be incremented or decremented by one.
 */
public class Counter extends Storage {
    // Counter value
    private UnsignedBigInteger _value = new UnsignedBigInteger(BigInteger.ZERO); // default: 0

    /**
     * Returns the current counter value
     *
     * @return current counter value
     */
    public @View UnsignedBigInteger current() {
        return _value;
    }

    /**
     * Increases the current counter value by one
     */
    public void increment() {
        _value = _value.add(new UnsignedBigInteger(BigInteger.ONE));
    }

    /**
     * Decreases the current counter value by one
     */
    public void decrement() {
        _value = _value.subtract(new UnsignedBigInteger(BigInteger.ONE));
    }
}
