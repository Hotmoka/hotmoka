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

package io.hotmoka.examples.basicdependency;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;

public class Wrapper extends Storage {
	private final Time time;
	private final String s;
	private final BigInteger bi;
	private final long l;

	public Wrapper(Time time) {
		this(time, null, null, 0L);
	}

	public Wrapper(Time time, String s, BigInteger bi, long l) {
		this.time = time;
		this.s = s;
		this.bi = bi;
		this.l = l;
	}

	@Override @View
	public String toString() {
		return StringSupport.concat("wrapper(", time, ",", s, ",", bi, ",", l, ")");
	}
}