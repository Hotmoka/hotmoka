package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.engine.AbstractNode;

/**
 * The creator of a response for a transaction that executes a method of Takamaka code.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class MethodCallResponseBuilder<Request extends MethodCallTransactionRequest> extends CodeCallResponseBuilder<Request, MethodCallTransactionResponse> {

	/**
	 * Builds the creator of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected MethodCallResponseBuilder(Request request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);
	}

	@Override
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = gas();

		return sizeCalculator.sizeOfResponse(new MethodCallTransactionFailedResponse
			("placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where",
			Stream.empty(), gas, gas, gas, gas));
	}

	protected abstract class ResponseCreator extends CodeCallResponseBuilder<Request, MethodCallTransactionResponse>.ResponseCreator {

		protected ResponseCreator() throws TransactionRejectedException {
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
			MethodSignature method = request.method;
			Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
			Class<?>[] argTypes = formalsAsClass();

			return getClassLoader().resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
				.orElseThrow(() -> new NoSuchMethodException(method.toString()));
		}

		/**
		 * Determines if the execution only affected the balance or nonce of the caller contract.
		 * 
		 * @param result the returned value for method calls or created object for constructor calls, if any
		 * @return true if and only if that condition holds
		 */
		protected final boolean onlyAffectedBalanceOrNonceOfCaller(Object result) {
			StorageReference caller = getClassLoader().getStorageReferenceOf(getDeserializedCaller());

			return updates(result).allMatch
				(update -> update.object.equals(caller)
							&& update instanceof UpdateOfField
							&& (((UpdateOfField) update).getField().equals(FieldSignature.BALANCE_FIELD)
								|| ((UpdateOfField) update).getField().equals(FieldSignature.EOA_NONCE_FIELD)
								|| ((UpdateOfField) update).getField().equals(FieldSignature.RGEOA_NONCE_FIELD)));
		}

		/**
		 * Checks that the method called by this transaction
		 * is white-listed and its white-listing proof-obligations hold.
		 * 
		 * @param executable the method
		 * @param actuals the actual arguments passed to {@code executable}, including the receiver for instance methods
		 * @throws ClassNotFoundException if some class could not be found during the check
		 */
		protected void ensureWhiteListingOf(Method executable, Object[] actuals) throws ClassNotFoundException {
			Optional<Method> model = getClassLoader().getWhiteListingWizard().whiteListingModelOf(executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed method " + request.method.definingClass.name + "." + request.method.methodName);

			Annotation[][] anns = model.get().getParameterAnnotations();
			String methodName = model.get().getName();

			for (int pos = 0; pos < actuals.length; pos++)
				checkWhiteListingProofObligations(methodName, actuals[pos], anns[pos]);
		}
	}
}