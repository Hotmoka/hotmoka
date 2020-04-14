package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.Method;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.AbstractNode;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code
 * annotated as {@linkplain io.hotmoka.code.lang.View}.
 */
public class StaticViewMethodCallResponseBuilder extends StaticMethodCallResponseBuilder {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public StaticViewMethodCallResponseBuilder(StaticMethodCallTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);
	}

	@Override
	protected void validateCallee(Method methodJVM) throws NoSuchMethodException {
		super.validateCallee(methodJVM);

		if (!hasAnnotation(methodJVM, Constants.VIEW_NAME))
			throw new NoSuchMethodException("cannot call a method not annotated as @View");
	}

	@Override
	protected final void callerAndRequestMustAgreeOnNonce() {
		// we disable the check, since the nonce is not checked in view transactions
	}

	@Override
	protected final void increaseNonceOfCaller() {
		// we disable the nonce increment for view transactions
	}
}