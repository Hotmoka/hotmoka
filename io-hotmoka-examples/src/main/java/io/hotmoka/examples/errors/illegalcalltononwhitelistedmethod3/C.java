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

package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod3;

import io.takamaka.code.lang.StringSupport;

public class C {

	public static String foo() {
		return test(new Object(), new Object());
	}

	private static String test(Object... args) {
		for (var o: args)
			StringSupport.concat(o); // this will be KO but only at run time

		return "done";
	}
}