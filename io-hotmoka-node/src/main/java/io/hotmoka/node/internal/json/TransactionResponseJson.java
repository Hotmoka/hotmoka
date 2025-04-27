/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.internal.json;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.crypto.Hex;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.NonVoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.internal.responses.TransactionResponseImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link TransactionResponse}.
 */
public abstract class TransactionResponseJson implements JsonRepresentation<TransactionResponse> {
	private final String type;
	private final Updates.Json[] updates;
	private final StorageValues.Json gamete;
	private final String instrumentedJar; // hex bytes
	private final TransactionReferences.Json[] dependencies;
	private final Long verificationToolVersion;
	private final BigInteger gasConsumedForCPU;
	private final BigInteger gasConsumedForRAM;
	private final BigInteger gasConsumedForStorage;
	private final BigInteger gasConsumedForPenalty;
	private final String classNameOfCause;
	private final String messageOfCause;
	private final StorageValues.Json[] events;
	private final String where;
	private final StorageValues.Json newObject;
	private final StorageValues.Json result;

	protected TransactionResponseJson(TransactionResponse response) {
		if (response instanceof GameteCreationTransactionResponse gctr) {
			this.type = GameteCreationTransactionResponse.class.getSimpleName();
			this.gamete = new StorageValues.Json(gctr.getGamete());
			this.updates = gctr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = null;
			this.gasConsumedForRAM = null;
			this.gasConsumedForStorage = null;
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = null;
			this.where = null;
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof InitializationTransactionResponse) {
			this.type = InitializationTransactionResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = null;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = null;
			this.gasConsumedForRAM = null;
			this.gasConsumedForStorage = null;
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = null;
			this.where = null;
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof JarStoreInitialTransactionResponse jsitr) {
			this.type = JarStoreInitialTransactionResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = null;
			this.instrumentedJar = Hex.toHexString(jsitr.getInstrumentedJar());
			this.dependencies = jsitr.getDependencies().map(TransactionReferences.Json::new).toArray(TransactionReferences.Json[]::new);
			this.verificationToolVersion = jsitr.getVerificationVersion();
			this.gasConsumedForCPU = null;
			this.gasConsumedForRAM = null;
			this.gasConsumedForStorage = null;
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = null;
			this.where = null;
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof JarStoreTransactionFailedResponse jstfr) {
			this.type = JarStoreTransactionFailedResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = jstfr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = jstfr.getGasConsumedForCPU();
			this.gasConsumedForRAM = jstfr.getGasConsumedForRAM();
			this.gasConsumedForStorage = jstfr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = jstfr.getGasConsumedForPenalty();
			this.classNameOfCause = jstfr.getClassNameOfCause();
			this.messageOfCause = jstfr.getMessageOfCause();
			this.events = null;
			this.where = null;
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof JarStoreTransactionSuccessfulResponse jstsr) {
			this.type = JarStoreTransactionSuccessfulResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = jstsr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = Hex.toHexString(jstsr.getInstrumentedJar());
			this.dependencies = jstsr.getDependencies().map(TransactionReferences.Json::new).toArray(TransactionReferences.Json[]::new);;
			this.verificationToolVersion = jstsr.getVerificationVersion();
			this.gasConsumedForCPU = jstsr.getGasConsumedForCPU();
			this.gasConsumedForRAM = jstsr.getGasConsumedForRAM();
			this.gasConsumedForStorage = jstsr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = null;
			this.where = null;
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof ConstructorCallTransactionExceptionResponse ccter) {
			this.type = ConstructorCallTransactionExceptionResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = ccter.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = ccter.getGasConsumedForCPU();
			this.gasConsumedForRAM = ccter.getGasConsumedForRAM();
			this.gasConsumedForStorage = ccter.getGasConsumedForStorage();
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = ccter.getClassNameOfCause();
			this.messageOfCause = ccter.getMessageOfCause();
			this.events = ccter.getEvents().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.where = ccter.getWhere();
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof ConstructorCallTransactionFailedResponse cctfr) {
			this.type = ConstructorCallTransactionFailedResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = cctfr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = cctfr.getGasConsumedForCPU();
			this.gasConsumedForRAM = cctfr.getGasConsumedForRAM();
			this.gasConsumedForStorage = cctfr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = cctfr.getGasConsumedForPenalty();
			this.classNameOfCause = cctfr.getClassNameOfCause();
			this.messageOfCause = cctfr.getMessageOfCause();
			this.events = null;
			this.where = cctfr.getWhere();
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof ConstructorCallTransactionSuccessfulResponse cctsr) {
			this.type = ConstructorCallTransactionSuccessfulResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = cctsr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = cctsr.getGasConsumedForCPU();
			this.gasConsumedForRAM = cctsr.getGasConsumedForRAM();
			this.gasConsumedForStorage = cctsr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = cctsr.getEvents().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.where = null;
			this.newObject = new StorageValues.Json(cctsr.getNewObject());
			this.result = null;
		}
		else if (response instanceof MethodCallTransactionExceptionResponse mcter) {
			this.type = MethodCallTransactionExceptionResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = mcter.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = mcter.getGasConsumedForCPU();
			this.gasConsumedForRAM = mcter.getGasConsumedForRAM();
			this.gasConsumedForStorage = mcter.getGasConsumedForStorage();
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = mcter.getClassNameOfCause();
			this.messageOfCause = mcter.getMessageOfCause();
			this.events = mcter.getEvents().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.where = mcter.getWhere();
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof MethodCallTransactionFailedResponse mctfr) {
			this.type = MethodCallTransactionFailedResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = mctfr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = mctfr.getGasConsumedForCPU();
			this.gasConsumedForRAM = mctfr.getGasConsumedForRAM();
			this.gasConsumedForStorage = mctfr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = mctfr.getGasConsumedForPenalty();
			this.classNameOfCause = mctfr.getClassNameOfCause();
			this.messageOfCause = mctfr.getMessageOfCause();
			this.events = null;
			this.where = mctfr.getWhere();
			this.newObject = null;
			this.result = null;
		}
		else if (response instanceof NonVoidMethodCallTransactionSuccessfulResponse mctsr) {
			this.type = NonVoidMethodCallTransactionSuccessfulResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = mctsr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = mctsr.getGasConsumedForCPU();
			this.gasConsumedForRAM = mctsr.getGasConsumedForRAM();
			this.gasConsumedForStorage = mctsr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = mctsr.getEvents().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.where = null;
			this.newObject = null;
			this.result = new StorageValues.Json(mctsr.getResult());
		}
		else if (response instanceof VoidMethodCallTransactionSuccessfulResponse vmctsr) {
			this.type = VoidMethodCallTransactionSuccessfulResponse.class.getSimpleName();
			this.gamete = null;
			this.updates = vmctsr.getUpdates().map(Updates.Json::new).toArray(Updates.Json[]::new);;
			this.instrumentedJar = null;
			this.dependencies = null;
			this.verificationToolVersion = null;
			this.gasConsumedForCPU = vmctsr.getGasConsumedForCPU();
			this.gasConsumedForRAM = vmctsr.getGasConsumedForRAM();
			this.gasConsumedForStorage = vmctsr.getGasConsumedForStorage();
			this.gasConsumedForPenalty = null;
			this.classNameOfCause = null;
			this.messageOfCause = null;
			this.events = vmctsr.getEvents().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.where = null;
			this.newObject = null;
			this.result = null;
		}
		else
			throw new IllegalArgumentException("Unexpected response of type " + response.getClass().getName());
	}

	public String getType() {
		return type;
	}

	public Stream<Updates.Json> getUpdates() {
		return updates == null ? Stream.empty() : Stream.of(updates);
	}

	public StorageValues.Json getGamete() {
		return gamete;
	}

	public String getInstrumentedJar() {
		return instrumentedJar;
	}

	public Stream<TransactionReferences.Json> getDependencies() {
		return dependencies == null ? Stream.empty() : Stream.of(dependencies);
	}

	public Long getVerificationToolVersion() {
		return verificationToolVersion;
	}

	public BigInteger getGasConsumedForCPU() {
		return gasConsumedForCPU;
	}

	public BigInteger getGasConsumedForRAM() {
		return gasConsumedForRAM;
	}

	public BigInteger getGasConsumedForStorage() {
		return gasConsumedForStorage;
	}

	public BigInteger getGasConsumedForPenalty() {
		return gasConsumedForPenalty;
	}

	public String getClassNameOfCause() {
		return classNameOfCause;
	}

	public String getMessageOfCause() {
		return messageOfCause;
	}

	public Stream<StorageValues.Json> getEvents() {
		return events == null ? Stream.empty() : Stream.of(events);
	}

	public String getWhere() {
		return where;
	}

	public StorageValues.Json getNewObject() {
		return newObject;
	}

	public StorageValues.Json getResult() {
		return result;
	}

	@Override
	public TransactionResponse unmap() throws InconsistentJsonException {
		return TransactionResponseImpl.from(this);
	}
}