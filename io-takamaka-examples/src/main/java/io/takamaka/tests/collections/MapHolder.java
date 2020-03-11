package io.takamaka.tests.collections;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;

/**
 * This class defines methods that test the storage map implementation
 * from BigIntegers to enums.
 */
public class MapHolder extends Storage {
	private final StorageMap<BigInteger, State> map = new StorageMap<>();

	public static enum State {
		RUNNING, SLEEPING, WAITING
	};

	public MapHolder() {
		map.put(BigInteger.ZERO, State.RUNNING);
		map.put(BigInteger.ONE, State.SLEEPING);
		map.put(BigInteger.TEN, State.WAITING);
	}

	public @View State get0() {
		return map.get(BigInteger.ZERO);
	}

	public @View State get1() {
		return map.get(BigInteger.ONE);
	}

	public @View State get10() {
		return map.get(BigInteger.TEN);
	}

	public boolean isRunning(Object state) {
		return state == State.RUNNING;
	}

	public boolean isSleeping(Object state) {
		return state == State.SLEEPING;
	}

	public boolean isWaiting(Object state) {
		return state == State.WAITING;
	}

	public boolean isRunning2(State state) {
		return state == State.RUNNING;
	}

	public boolean isSleeping2(State state) {
		return state == State.SLEEPING;
	}

	public boolean isWaiting2(State state) {
		return state == State.WAITING;
	}

	public boolean isRunning3(Comparable<?> state) {
		return state == State.RUNNING;
	}

	public boolean isSleeping3(Comparable<?> state) {
		return state == State.SLEEPING;
	}

	public boolean isWaiting3(Comparable<?> state) {
		return state == State.WAITING;
	}
}