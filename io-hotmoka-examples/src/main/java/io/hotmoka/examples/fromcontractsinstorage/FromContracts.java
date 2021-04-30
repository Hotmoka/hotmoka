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

package io.hotmoka.examples.fromcontractsinstorage;

import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class FromContracts extends Storage {

	// this returns the caller of entry1
	public @FromContract Contract entry1() {
		return getCaller();
	}

	// this returns this contract itself
	/*public @Entry Contract entry2() {
		Entries entries = this;
		return entries.getCaller();
	}*/

	// this returns this contract itself
	/*public @Entry Contract entry3() {
		Supplier<Contract> target = this::getCaller;
		return target.get();
	}*/

	// this returns this contract itself
	/*public @Entry Contract entry4() {
		Entries entries = this;
		Supplier<Contract> target = entries::getCaller;
		return target.get();
	}*/

	// this returns the caller of entry5
	public @FromContract Contract entry5() {
		Supplier<Contract> target = () -> this.getCaller();
		return target.get();
	}

	public @FromContract Contract getCaller() {
		return caller();
	}
}