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

package io.hotmoka.instrumentation;

import java.math.BigInteger;

/**
 * A specification of the cost of gas.
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
		return 4;
	}

	@Override
	public int ramCostOfActivationSlot() {
		return 4;
	}

	@Override
	public BigInteger ramCostForInstallingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 40);
	}

	@Override
	public BigInteger ramCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 200);
	}

	@Override
	public int cpuCostOfArithmeticInstruction() {
		return 2;
	}

	@Override
	public int cpuCostOfArrayAccessInstruction() {
		return 3;
	}

	@Override
	public int cpuCostOfFieldAccessInstruction() {
		return 3;
	}

	@Override
	public int cpuCostOfInvokeInstruction() {
		return 5;
	}

	@Override
	public int cpuCostOfSelectInstruction() {
		return 4;
	}

	@Override
	public int cpuCostOfMemoryAllocationInstruction() {
		return 10;
	}

	@Override
	public int cpuCostOfInstruction() {
		return 1;
	}

	@Override
	public BigInteger cpuCostForInstallingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 400);
	}

	@Override
	public BigInteger cpuCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 1000);
	}

	@Override
	public BigInteger cpuBaseTransactionCost() {
		return BigInteger.valueOf(10);
	}
}