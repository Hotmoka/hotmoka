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

package io.takamaka.code.whitelisting.internal.database.version0.java.lang;

import io.takamaka.code.whitelisting.HasDeterministicTerminatingToString;

public abstract class StringBuilder {

	public StringBuilder() {
	}

	public StringBuilder(java.lang.String message) {
	}

	public StringBuilder(int i) {
	}

	public abstract java.lang.StringBuilder append(boolean b);
	public abstract java.lang.StringBuilder append(char c);
	public abstract java.lang.StringBuilder append(int i);
	public abstract java.lang.StringBuilder append(long l);
	public abstract java.lang.StringBuilder append(float f);
	public abstract java.lang.StringBuilder append(double d);
	public abstract java.lang.StringBuilder append(java.lang.String s);
	public abstract java.lang.StringBuilder append(@HasDeterministicTerminatingToString java.lang.Object o);
	public abstract java.lang.String toString();
}