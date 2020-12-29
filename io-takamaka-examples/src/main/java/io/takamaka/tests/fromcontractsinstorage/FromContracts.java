package io.takamaka.tests.fromcontractsinstorage;

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