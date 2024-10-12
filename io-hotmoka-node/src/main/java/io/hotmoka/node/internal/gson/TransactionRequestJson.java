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

package io.hotmoka.node.internal.gson;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link TransactionRequest}.
 */
public abstract class TransactionRequestJson implements JsonRepresentation<TransactionRequest<?>> {
	private final String type;
	private final TransactionReferences.Json classpath;
	private final BigInteger initialAmount;
	private final BigInteger redInitialAmount;
	private final String publicKey;
	private final StorageValues.Json manifest;
	private final String jar; // Base64-encoded bytes
	private final TransactionReferences.Json[] dependencies;
	private final StorageValues.Json caller;
	private final BigInteger gasLimit;
	private final BigInteger gasPrice;
	private final BigInteger nonce;
	private final String chainId;
	private final String signature; // hex bytes
	private final StorageValues.Json[] actuals;
	private final ConstructorSignatures.Json constructor;
	private final MethodSignatures.Json method;
	private final StorageValues.Json receiver;

	protected TransactionRequestJson(TransactionRequest<?> request) {
		if (request instanceof GameteCreationTransactionRequest gctr) {
			this.type = GameteCreationTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(gctr.getClasspath());
			this.initialAmount = gctr.getInitialAmount();
			this.redInitialAmount = gctr.getRedInitialAmount();
			this.publicKey = gctr.getPublicKey();
			this.manifest = null;
			this.jar = null;
			this.dependencies = null;
			this.caller = null;
			this.gasLimit = null;
			this.gasPrice = null;
			this.nonce = null;
			this.chainId = null;
			this.signature = null;
			this.actuals = null;
			this.constructor = null;
			this.method = null;
			this.receiver = null;
		}
		else if (request instanceof InitializationTransactionRequest itr) {
			this.type = InitializationTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(itr.getClasspath());
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = new StorageValues.Json(itr.getManifest());
			this.jar = null;
			this.dependencies = null;
			this.caller = null;
			this.gasLimit = null;
			this.gasPrice = null;
			this.nonce = null;
			this.chainId = null;
			this.signature = null;
			this.actuals = null;
			this.constructor = null;
			this.method = null;
			this.receiver = null;
		}
		else if (request instanceof JarStoreInitialTransactionRequest jitr) {
			this.type = JarStoreInitialTransactionRequest.class.getSimpleName();
			this.classpath = null;
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = null;
			this.jar = Base64.toBase64String(jitr.getJar());
			this.dependencies = jitr.getDependencies().map(TransactionReferences.Json::new).toArray(TransactionReferences.Json[]::new);
			this.caller = null;
			this.gasLimit = null;
			this.gasPrice = null;
			this.nonce = null;
			this.chainId = null;
			this.signature = null;
			this.actuals = null;
			this.constructor = null;
			this.method = null;
			this.receiver = null;
		}
		else if (request instanceof JarStoreTransactionRequest jtr) {
			this.type = JarStoreTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(jtr.getClasspath());
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = null;
			this.jar = Base64.toBase64String(jtr.getJar());
			this.dependencies = jtr.getDependencies().map(TransactionReferences.Json::new).toArray(TransactionReferences.Json[]::new);
			this.caller = new StorageValues.Json(jtr.getCaller());
			this.gasLimit = jtr.getGasLimit();
			this.gasPrice = jtr.getGasPrice();
			this.nonce = jtr.getNonce();
			this.chainId = jtr.getChainId();
			this.signature = Hex.toHexString(jtr.getSignature());
			this.actuals = null;
			this.constructor = null;
			this.method = null;
			this.receiver = null;
		}
		else if (request instanceof ConstructorCallTransactionRequest cctr) {
			this.type = ConstructorCallTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(cctr.getClasspath());
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = null;
			this.jar = null;
			this.dependencies = null;
			this.caller = new StorageValues.Json(cctr.getCaller());
			this.gasLimit = cctr.getGasLimit();
			this.gasPrice = cctr.getGasPrice();
			this.nonce = cctr.getNonce();
			this.chainId = cctr.getChainId();
			this.signature = Hex.toHexString(cctr.getSignature());
			this.actuals = cctr.actuals().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.constructor = new ConstructorSignatures.Json(cctr.getStaticTarget());
			this.method = null;
			this.receiver = null;
		}
		else if (request instanceof StaticMethodCallTransactionRequest smctr) {
			this.type = StaticMethodCallTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(smctr.getClasspath());
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = null;
			this.jar = null;
			this.dependencies = null;
			this.caller = new StorageValues.Json(smctr.getCaller());
			this.gasLimit = smctr.getGasLimit();
			this.gasPrice = smctr.getGasPrice();
			this.nonce = smctr.getNonce();
			this.chainId = smctr.getChainId();
			this.signature = Hex.toHexString(smctr.getSignature());
			this.actuals = smctr.actuals().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.constructor = null;
			this.method = new MethodSignatures.Json(smctr.getStaticTarget());
			this.receiver = null;
		}
		else if (request instanceof InstanceMethodCallTransactionRequest imctr) {
			this.type = InstanceMethodCallTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(imctr.getClasspath());
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = null;
			this.jar = null;
			this.dependencies = null;
			this.caller = new StorageValues.Json(imctr.getCaller());
			this.gasLimit = imctr.getGasLimit();
			this.gasPrice = imctr.getGasPrice();
			this.nonce = imctr.getNonce();
			this.chainId = imctr.getChainId();
			this.signature = Hex.toHexString(imctr.getSignature());
			this.actuals = imctr.actuals().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.constructor = null;
			this.method = new MethodSignatures.Json(imctr.getStaticTarget());
			this.receiver = new StorageValues.Json(imctr.getReceiver());
		}
		else if (request instanceof InstanceSystemMethodCallTransactionRequest ismctr) {
			this.type = InstanceSystemMethodCallTransactionRequest.class.getSimpleName();
			this.classpath = new TransactionReferences.Json(ismctr.getClasspath());
			this.initialAmount = null;
			this.redInitialAmount = null;
			this.publicKey = null;
			this.manifest = null;
			this.jar = null;
			this.dependencies = null;
			this.caller = new StorageValues.Json(ismctr.getCaller());
			this.gasLimit = ismctr.getGasLimit();
			this.gasPrice = null;
			this.nonce = ismctr.getNonce();
			this.chainId = null;
			this.signature = null;
			this.actuals = ismctr.actuals().map(StorageValues.Json::new).toArray(StorageValues.Json[]::new);
			this.constructor = null;
			this.method = new MethodSignatures.Json(ismctr.getStaticTarget());
			this.receiver = new StorageValues.Json(ismctr.getReceiver());
		}
		else
			throw new IllegalArgumentException("Unexpected request of type " + request.getClass().getName());
	}

	@Override
	public TransactionRequest<?> unmap() throws InconsistentJsonException {
		try {
			if (GameteCreationTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.gameteCreation(classpath.unmap(), initialAmount, redInitialAmount, publicKey);
			else if (InitializationTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.initialization(classpath.unmap(), unmapIntoStorageReference(manifest));
			else if (JarStoreInitialTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.jarStoreInitial(Base64.fromBase64String(jar), convertedDependencies());
			else if (JarStoreTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.jarStore(Hex.fromHexString(signature), unmapIntoStorageReference(caller), nonce, chainId, gasLimit, gasPrice, classpath.unmap(), Base64.fromBase64String(jar), convertedDependencies());
			else if (ConstructorCallTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.constructorCall(Hex.fromHexString(signature), unmapIntoStorageReference(caller), nonce, chainId, gasLimit, gasPrice, classpath.unmap(), constructor.unmap(), convertedActuals());
			else if (StaticMethodCallTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.staticMethodCall(Hex.fromHexString(signature), unmapIntoStorageReference(caller), nonce, chainId, gasLimit, gasPrice, classpath.unmap(), method.unmap(), convertedActuals());
			else if (InstanceMethodCallTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.instanceMethodCall(Hex.fromHexString(signature), unmapIntoStorageReference(caller), nonce, chainId, gasLimit, gasPrice, classpath.unmap(), method.unmap(), unmapIntoStorageReference(receiver), convertedActuals());
			else if (InstanceSystemMethodCallTransactionRequest.class.getSimpleName().equals(type))
				return TransactionRequests.instanceSystemMethodCall(unmapIntoStorageReference(caller), nonce, gasLimit, classpath.unmap(), method.unmap(), unmapIntoStorageReference(receiver), convertedActuals());
			else
				throw new InconsistentJsonException("Unexpected request type " + type);
		}
		catch (HexConversionException | Base64ConversionException e) {
			throw new InconsistentJsonException(e);
		}
	}

	private static StorageReference unmapIntoStorageReference(StorageValues.Json json) throws InconsistentJsonException {
		if (json.unmap() instanceof StorageReference sr)
			return sr;
		else
			throw new InconsistentJsonException("Unexpected storage value");
	}

	private TransactionReference[] convertedDependencies() throws HexConversionException {
		return CheckSupplier.check(HexConversionException.class,
			() -> Stream.of(dependencies).map(UncheckFunction.uncheck(TransactionReferences.Json::unmap)).toArray(TransactionReference[]::new)
		);
	}

	private StorageValue[] convertedActuals() throws HexConversionException {
		return CheckSupplier.check(HexConversionException.class,
			() -> Stream.of(actuals).map(UncheckFunction.uncheck(StorageValues.Json::unmap)).toArray(StorageValue[]::new)
		);
	}
}