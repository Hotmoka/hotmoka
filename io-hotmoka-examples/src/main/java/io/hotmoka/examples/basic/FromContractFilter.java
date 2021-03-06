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

package io.hotmoka.examples.basic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.ThrowsExceptions;
import io.takamaka.code.lang.View;

public class FromContractFilter extends Contract {
	public @FromContract @View void foo1() {}
	public @FromContract(Contract.class) @View void foo2() {}
	public @FromContract(PayableContract.class) @View void foo3() {}
	public @FromContract(FromContractFilter.class) @View void foo4() {}
	public @ThrowsExceptions @View void foo5() throws MyCheckedException {
		throw new MyCheckedException();
	}
	public @View void foo6() throws MyCheckedException {
		throw new MyCheckedException();
	}
}