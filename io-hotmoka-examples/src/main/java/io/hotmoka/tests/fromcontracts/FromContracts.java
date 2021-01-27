package io.hotmoka.tests.fromcontracts;

import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class FromContracts extends Contract {

	// this returns the caller of entry1
	public @FromContract Contract entry1() {
		return getCaller();
	}

	// this returns this contract itself
	public @FromContract Contract entry2() {
		FromContracts entries = this;
		return entries.getCaller();
	}

	// this returns this contract itself
	public @FromContract Contract entry3() {
		Supplier<Contract> target = this::getCaller;
		return target.get();
	}

	// this returns this contract itself
	public @FromContract Contract entry4() {
		FromContracts entries = this;
		Supplier<Contract> target = entries::getCaller;
		return target.get();
	}

	// this returns the caller of entry5
	public @FromContract Contract entry5() {
		Supplier<Contract> target = () -> this.getCaller();
		return target.get();
	}

	public @FromContract Contract getCaller() {
		return caller();
	}
}