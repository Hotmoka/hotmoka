package io.takamaka.tests.collections;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.ModifiableStorageMap;

/**
 * This class defines methods that test the storage map implementation
 * from BigIntegers to enums.
 */
public class MapHolder extends Storage {
	private final ModifiableStorageMap<BigInteger, State> map = ModifiableStorageMap.empty();

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

	public @View boolean isRunning(Object state) {
		return state == State.RUNNING;
	}

	public @View boolean isSleeping(Object state) {
		return state == State.SLEEPING;
	}

	public @View boolean isWaiting(Object state) {
		return state == State.WAITING;
	}

	public @View boolean isRunning2(State state) {
		return state == State.RUNNING;
	}

	public @View boolean isSleeping2(State state) {
		return state == State.SLEEPING;
	}

	public @View boolean isWaiting2(State state) {
		return state == State.WAITING;
	}

	public @View boolean isRunning3(Comparable<?> state) {
		return state == State.RUNNING;
	}

	public @View boolean isSleeping3(Comparable<?> state) {
		return state == State.SLEEPING;
	}

	public @View boolean isWaiting3(Comparable<?> state) {
		return state == State.WAITING;
	}
}