/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.json;

import java.math.BigInteger;

import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.internal.GasCostImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the gas cost of a transaction execution.
 */
public abstract class GasCostJson implements JsonRepresentation<GasCost> {
	private final BigInteger forCPU;
	private final BigInteger forRAM;
	private final BigInteger forStorage;
	private final BigInteger forPenalty;
	private final BigInteger price;

	protected GasCostJson(GasCost cost) {
		this.forCPU = cost.getForCPU();
		this.forRAM = cost.getForRAM();
		this.forStorage = cost.getForStorage();
		this.forPenalty = cost.getForPenalty();
		this.price = cost.getPrice();
	}

	public BigInteger getForCPU() {
		return forCPU;
	}

	public BigInteger getForRAM() {
		return forRAM;
	}

	public BigInteger getForStorage() {
		return forStorage;
	}

	public BigInteger getForPenalty() {
		return forPenalty;
	}

	public BigInteger getPrice() {
		return price;
	}

	@Override
	public GasCost unmap() throws InconsistentJsonException {
		return new GasCostImpl(this);
	}
}