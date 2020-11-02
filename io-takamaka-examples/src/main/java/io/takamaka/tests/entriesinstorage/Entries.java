package io.takamaka.tests.entriesinstorage;

import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class Entries extends Storage {

	// this returns the caller of entry1
	public @Entry Contract entry1() {
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
	public @Entry Contract entry5() {
		Supplier<Contract> target = () -> this.getCaller();
		return target.get();
	}

	public @Entry Contract getCaller() {
		return caller();
	}
}