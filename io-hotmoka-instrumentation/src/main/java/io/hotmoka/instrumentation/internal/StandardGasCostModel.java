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

package io.hotmoka.instrumentation.internal;

import java.math.BigInteger;

import io.hotmoka.instrumentation.api.GasCostModel;

/**
 * The standard specification of the cost of gas.
 */
public class StandardGasCostModel implements GasCostModel {

	@Override
	public int ramCostOfObject() {
		return 8;
	}

	@Override
	public int ramCostOfField() {
		return 4;
	}

	@Override
	public int ramCostOfArraySlot() {
		return 4;
	}

	@Override
	public int ramCostOfArray() {
		return 12;
	}

	@Override
	public int ramCostOfActivationRecord() {
		return 32;
	}

	@Override
	public int ramCostOfActivationSlot() {
		return 8;
	}

	@Override
	public BigInteger ramCostForInstallingJar(int numBytes) {
		// higher than the subsequent one, since code verification applies
		return BigInteger.valueOf(numBytes / 20);
	}

	@Override
	public BigInteger ramCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 80);
	}

	@Override
	public int cpuCostOfArithmeticInstruction() {
		return 2;
	}

	@Override
	public int cpuCostOfArrayAccessInstruction() {
		return 2;
	}

	@Override
	public int cpuCostOfFieldAccessInstruction() {
		return 2;
	}

	@Override
	public int cpuCostOfInvokeInstruction() {
		return 8;
	}

	@Override
	public int cpuCostOfSelectInstruction() {
		return 2;
	}

	@Override
	public int cpuCostOfMemoryAllocationInstruction() {
		return 16;
	}

	@Override
	public int cpuCostOfInstruction() {
		return 1;
	}

	@Override
	public BigInteger cpuCostForInstallingJar(int numBytes) {
		// higher than the subsequent one, since code verification applies
		return BigInteger.valueOf(numBytes / 10);
	}

	@Override
	public BigInteger cpuCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 40);
	}

	@Override
	public BigInteger cpuBaseTransactionCost() {
		return BigInteger.valueOf(5_000);
	}

	@Override
	public BigInteger storageCostOfByte() {
		return BigInteger.valueOf(200);
	}
}