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

package io.hotmoka.beans.internal.gson;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.TransactionResponses;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.api.responses.InitializationTransactionResponse;
import io.hotmoka.beans.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
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
		else if (response instanceof MethodCallTransactionSuccessfulResponse mctsr) {
			this.type = MethodCallTransactionSuccessfulResponse.class.getSimpleName();
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

	@Override
	public TransactionResponse unmap() throws IllegalArgumentException, HexConversionException {
		if (GameteCreationTransactionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.gameteCreation(convertedUpdates(), (StorageReference) gamete.unmap());
		else if (InitializationTransactionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.initialization();
		else if (JarStoreInitialTransactionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.jarStoreInitial(Hex.fromHexString(instrumentedJar), convertedDependencies(), verificationToolVersion);
		else if (JarStoreTransactionFailedResponse.class.getSimpleName().equals(type))
			return TransactionResponses.jarStoreFailed(classNameOfCause, messageOfCause, convertedUpdates(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		else if (JarStoreTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.jarStoreSuccessful(Hex.fromHexString(instrumentedJar), convertedDependencies(), verificationToolVersion, convertedUpdates(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		else if (ConstructorCallTransactionExceptionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.constructorCallException(classNameOfCause, messageOfCause, where, convertedUpdates(), convertedEvents(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		else if (ConstructorCallTransactionFailedResponse.class.getSimpleName().equals(type))
			return TransactionResponses.constructorCallFailed(classNameOfCause, messageOfCause, where, convertedUpdates(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		else if (ConstructorCallTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.constructorCallSuccessful((StorageReference) newObject.unmap(), convertedUpdates(), convertedEvents(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		else if (MethodCallTransactionExceptionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.methodCallException(classNameOfCause, messageOfCause, where, convertedUpdates(), convertedEvents(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		else if (MethodCallTransactionFailedResponse.class.getSimpleName().equals(type))
			return TransactionResponses.methodCallFailed(classNameOfCause, messageOfCause, where, convertedUpdates(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		else if (MethodCallTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.methodCallSuccessful(result.unmap(), convertedUpdates(), convertedEvents(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		else if (VoidMethodCallTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.voidMethodCallSuccessful(convertedUpdates(), convertedEvents(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		else
			throw new IllegalArgumentException("Unexpected response type " + type);
	}

	private Stream<Update> convertedUpdates() throws HexConversionException {
		return Stream.of(CheckSupplier.check(HexConversionException.class,
			() -> Stream.of(updates).map(UncheckFunction.uncheck(Updates.Json::unmap)).toArray(Update[]::new)
		));
	}

	private Stream<TransactionReference> convertedDependencies() throws HexConversionException {
		return Stream.of(CheckSupplier.check(HexConversionException.class,
			() -> Stream.of(dependencies).map(UncheckFunction.uncheck(TransactionReferences.Json::unmap)).toArray(TransactionReference[]::new)
		));
	}

	private Stream<StorageReference> convertedEvents() throws HexConversionException {
		return Stream.of(CheckSupplier.check(HexConversionException.class,
			() -> Stream.of(events).map(UncheckFunction.uncheck(StorageValues.Json::unmap)).map(value -> (StorageReference) value).toArray(StorageReference[]::new)
		));
	}
}