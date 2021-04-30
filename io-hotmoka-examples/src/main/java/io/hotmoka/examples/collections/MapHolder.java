/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.examples.collections;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * This class defines methods that test the storage map implementation
 * from BigIntegers to enums.
 */
public class MapHolder extends Storage {
	private final StorageMap<BigInteger, State> map = new StorageTreeMap<>();

	public enum State {
		RUNNING, SLEEPING, WAITING
	}

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