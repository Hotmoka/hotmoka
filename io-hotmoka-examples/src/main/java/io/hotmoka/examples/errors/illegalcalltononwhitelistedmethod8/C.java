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

package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod8;

public class C {

	public static String foo() {
		Object o1 = new Object();
		Object o2 = new Object();
		test1(o1, o2);
		return test2(o1, o2);
	}

	private static String test1(Object arg1, Object arg2) {
		return "" + arg1 + arg2; // KO at run time, since it calls toString() on Object
	}

	private static String test2(Object arg1, Object arg2) {
		return "" + arg1 + arg2; // KO at run time, since it calls toString() on Object
	}
}