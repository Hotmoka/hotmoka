package io.takamaka.code.blockchain.internal;

import java.math.BigInteger;

import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.requests.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.requests.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.requests.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.requests.NonInitialTransactionRequest;
import io.takamaka.code.blockchain.requests.StaticMethodCallTransactionRequest;
import io.takamaka.code.blockchain.responses.ConstructorCallTransactionExceptionResponse;
import io.takamaka.code.blockchain.responses.ConstructorCallTransactionFailedResponse;
import io.takamaka.code.blockchain.responses.ConstructorCallTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.responses.JarStoreTransactionFailedResponse;
import io.takamaka.code.blockchain.responses.JarStoreTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.responses.MethodCallTransactionExceptionResponse;
import io.takamaka.code.blockchain.responses.MethodCallTransactionFailedResponse;
import io.takamaka.code.blockchain.responses.MethodCallTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.responses.NonInitialTransactionResponse;
import io.takamaka.code.blockchain.responses.TransactionResponseFailed;
import io.takamaka.code.blockchain.responses.TransactionResponseWithEvents;
import io.takamaka.code.blockchain.responses.TransactionResponseWithGas;
import io.takamaka.code.blockchain.responses.TransactionResponseWithUpdates;
import io.takamaka.code.blockchain.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.signatures.CodeSignature;
import io.takamaka.code.blockchain.signatures.ConstructorSignature;
import io.takamaka.code.blockchain.signatures.FieldSignature;
import io.takamaka.code.blockchain.signatures.NonVoidMethodSignature;
import io.takamaka.code.blockchain.signatures.VoidMethodSignature;
import io.takamaka.code.blockchain.types.BasicTypes;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.types.StorageType;
import io.takamaka.code.blockchain.values.BigIntegerValue;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.ByteValue;
import io.takamaka.code.blockchain.values.CharValue;
import io.takamaka.code.blockchain.values.DoubleValue;
import io.takamaka.code.blockchain.values.EnumValue;
import io.takamaka.code.blockchain.values.FloatValue;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.LongValue;
import io.takamaka.code.blockchain.values.NullValue;
import io.takamaka.code.blockchain.values.ShortValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;
import io.takamaka.code.blockchain.values.StringValue;

/**
 * An object that knows about the size of objects when stored in blockchain.
 */
public class SizeCalculator {
	private final GasCostModel gasCostModel;

	/**
	 * Builds the size calculator.
	 * 
	 * @param gasCostModel the gas model to use for the calculations
	 */
	public SizeCalculator(GasCostModel gasCostModel) {
		this.gasCostModel = gasCostModel;
	}

	/**
	 * Yields the size of the given request, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @param request the request
	 * @return the size
	 */
	public BigInteger sizeOf(NonInitialTransactionRequest request) {
		if (request instanceof ConstructorCallTransactionRequest)
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOf(request.caller))
				.add(gasCostModel.storageCostOf(request.gas)).add(request.classpath.size(gasCostModel))
				.add(((ConstructorCallTransactionRequest) request).actuals().map(this::sizeOf).reduce(BigInteger.ZERO, BigInteger::add));
		else if (request instanceof InstanceMethodCallTransactionRequest) {
			InstanceMethodCallTransactionRequest instanceMethodCallTransactionRequest = (InstanceMethodCallTransactionRequest) request;
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOf(request.caller))
				.add(gasCostModel.storageCostOf(request.gas)).add(request.classpath.size(gasCostModel))
				.add(sizeOf(instanceMethodCallTransactionRequest.method))
				.add(sizeOf(instanceMethodCallTransactionRequest.receiver))
				.add(instanceMethodCallTransactionRequest.getActuals().map(this::sizeOf).reduce(BigInteger.ZERO, BigInteger::add));
		}
		else if (request instanceof StaticMethodCallTransactionRequest) {
			StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = (StaticMethodCallTransactionRequest) request;
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOf(request.caller))
				.add(gasCostModel.storageCostOf(request.gas)).add(request.classpath.size(gasCostModel))
				.add(sizeOf(staticMethodCallTransactionRequest.method))
				.add(staticMethodCallTransactionRequest.getActuals().map(this::sizeOf).reduce(BigInteger.ZERO, BigInteger::add));
		}
		else if (request instanceof JarStoreTransactionRequest) {
			JarStoreTransactionRequest jarStoreTransactionRequest = (JarStoreTransactionRequest) request;
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOf(request.caller)).add(gasCostModel.storageCostOf(request.gas)).add(request.classpath.size(gasCostModel))
				.add(jarStoreTransactionRequest.getDependencies().map(classpath -> classpath.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add))
				.add(gasCostModel.storageCostOfJar(jarStoreTransactionRequest.getJarLength()));
		}
		else
			throw new IllegalArgumentException("unexpected transaction request");
	}

	/**
	 * Yields the size of the given response, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @param response the response
	 * @return the size
	 */
	public BigInteger sizeOf(NonInitialTransactionResponse response) {
		BigInteger size = BigInteger.valueOf(gasCostModel.storageCostPerSlot());

		if (response instanceof TransactionResponseWithGas) {
			TransactionResponseWithGas responseAsWithGas = (TransactionResponseWithGas) response;

			size = size.add(gasCostModel.storageCostOf(responseAsWithGas.gasConsumedForCPU()))
				.add(gasCostModel.storageCostOf(responseAsWithGas.gasConsumedForRAM()))
				.add(gasCostModel.storageCostOf(responseAsWithGas.gasConsumedForStorage()));
		}

		if (response instanceof TransactionResponseWithUpdates)
			size = size.add(((TransactionResponseWithUpdates) response).getUpdates().map(update -> update.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));

		if (response instanceof TransactionResponseFailed)
			size = size.add(gasCostModel.storageCostOf(((TransactionResponseFailed) response).gasConsumedForPenalty()));

		if (response instanceof TransactionResponseWithEvents)
			size = size.add(((TransactionResponseWithEvents) response).getEvents().map(this::sizeOf).reduce(BigInteger.ZERO, BigInteger::add));

		if (response instanceof ConstructorCallTransactionSuccessfulResponse)
			return size.add(sizeOf(((ConstructorCallTransactionSuccessfulResponse) response).newObject));
		else if (response instanceof MethodCallTransactionSuccessfulResponse)
			return size.add(sizeOf(((MethodCallTransactionSuccessfulResponse) response).result));
		else if (response instanceof JarStoreTransactionSuccessfulResponse)
			return size.add(gasCostModel.storageCostOfJar(((JarStoreTransactionSuccessfulResponse) response).getInstrumentedJarLength()));
		else if (response instanceof ConstructorCallTransactionExceptionResponse ||
				response instanceof ConstructorCallTransactionFailedResponse ||
				response instanceof JarStoreTransactionFailedResponse ||
				response instanceof MethodCallTransactionExceptionResponse ||
				response instanceof MethodCallTransactionFailedResponse ||
				response instanceof VoidMethodCallTransactionSuccessfulResponse)
			return size;
		else
			throw new IllegalArgumentException("unexpected transaction response");
	}

	/**
	 * Yields the size of the given field, in terms of storage consumed if the field is
	 * stored in blockchain.
	 * 
	 * @param field the field
	 * @return the size
	 */
	public BigInteger sizeOf(FieldSignature field) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(sizeOf(field.definingClass))
			.add(gasCostModel.storageCostOf(field.name)).add(sizeOf(field.type));
	}

	/**
	 * Yields the size of the given method or constructor, in terms of storage gas units consumed
	 * if it is stored in blockchain.
	 * 
	 * @param methodOrConstructor the method or constructor
	 * @return the size
	 */
	public BigInteger sizeOf(CodeSignature methodOrConstructor) {
		BigInteger size = BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(sizeOf(methodOrConstructor.definingClass))
			.add(methodOrConstructor.formals().map(this::sizeOf).reduce(BigInteger.ZERO, BigInteger::add));

		if (methodOrConstructor instanceof ConstructorSignature)
			return size;
		else if (methodOrConstructor instanceof VoidMethodSignature)
			return size.add(gasCostModel.storageCostOf(((VoidMethodSignature) methodOrConstructor).methodName));
		else if (methodOrConstructor instanceof NonVoidMethodSignature) {
			NonVoidMethodSignature method = (NonVoidMethodSignature) methodOrConstructor;
			return size.add(gasCostModel.storageCostOf(method.methodName)).add(sizeOf(method.returnType));
		}
		else
			throw new IllegalArgumentException("unexpected code signature");
	}

	/**
	 * Yields the size of the given type, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @param type the type
	 * @return the size
	 */
	public BigInteger sizeOf(StorageType type) {
		if (type instanceof BasicTypes)
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot());
		else if (type instanceof ClassType)
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
				.add(gasCostModel.storageCostOf(((ClassType) type).name));
		else
			throw new IllegalArgumentException("unexpected storage type");
	}


	/**
	 * Yields the size of the given value, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @param value the value
	 * @return the size
	 */
	public BigInteger sizeOf(StorageValue value) {
		BigInteger size = BigInteger.valueOf(gasCostModel.storageCostPerSlot());
		if (value instanceof BigIntegerValue)
			return size.add(gasCostModel.storageCostOf(((BigIntegerValue) value).value));
		else if (value instanceof StringValue)
			return size.add(gasCostModel.storageCostOf(((StringValue) value).value));
		else if (value instanceof StorageReference) {
			StorageReference storageReference = (StorageReference) value;
			return size.add(gasCostModel.storageCostOf(storageReference.progressive))
				.add(storageReference.transaction.size(gasCostModel));
		}
		else if (value instanceof EnumValue) {
			EnumValue enumValue = (EnumValue) value;
			return size.add(gasCostModel.storageCostOf(enumValue.enumClassName))
					.add(gasCostModel.storageCostOf(enumValue.name));
		}
		else if (value instanceof BooleanValue || value instanceof IntValue || value instanceof CharValue
				|| value instanceof DoubleValue || value instanceof FloatValue || value instanceof NullValue
				|| value instanceof LongValue || value instanceof ByteValue || value instanceof ShortValue)
			return size;
		else
			throw new IllegalArgumentException("unexpected storage value");
	}
}