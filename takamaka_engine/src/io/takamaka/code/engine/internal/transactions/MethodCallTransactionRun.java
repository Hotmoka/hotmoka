package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.Method;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.nodes.Node;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class MethodCallTransactionRun<Request extends MethodCallTransactionRequest> extends CodeCallTransactionRun<Request, MethodCallTransactionResponse> {

	/**
	 * The method that is being called.
	 */
	protected final MethodSignature method;

	/**
	 * True if the method has been called correctly and it is declared as {@code void},
	 */
	protected boolean isVoidMethod;

	/**
	 * True if the method has been called correctly and it is annotated as {@link io.takamaka.code.lang.View}.
	 */
	protected boolean isViewMethod;

	protected MethodCallTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		this.method = request.method;
	}

	/**
	 * Resolves the method that must be called.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws SecurityException if the method could not be accessed
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	protected final Method getMethod() throws ClassNotFoundException, NoSuchMethodException {
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClass();

		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	/**
	 * Determines if the execution only affected the balance of the caller contract.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean onlyAffectedBalanceOf() {
		return updates().allMatch
			(update -> update.object.equals(classLoader.getStorageReferenceOf(deserializedCaller))
						&& update instanceof UpdateOfField
						&& ((UpdateOfField) update).getField().equals(FieldSignature.BALANCE_FIELD));
	}

	@Override
	protected final MethodSignature getMethodOrConstructor() {
		return method;
	}
}