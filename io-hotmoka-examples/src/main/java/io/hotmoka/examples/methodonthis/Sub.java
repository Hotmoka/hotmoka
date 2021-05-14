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

package io.hotmoka.examples.methodonthis;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {

	@Override @FromContract @Payable
	public void foo(int amount) {
		super.foo(amount);
	}
}