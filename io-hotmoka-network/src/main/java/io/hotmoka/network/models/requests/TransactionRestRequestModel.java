package io.hotmoka.network.models.requests;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.*;

/**
 * Class which wraps a type request model
 * @param <T> the type request model
 */
@Immutable
public class TransactionRestRequestModel<T> {
    /**
     * The request model which should be an instance of {@link io.hotmoka.network.models.requests.TransactionRequestModel}.
     */
    public final T transactionRequestModel;

    /**
     * The runtime type of the request model
     */
    private final String type;

    public TransactionRestRequestModel(T transactionRequestModel) {
        this.transactionRequestModel = transactionRequestModel;
        this.type = transactionRequestModel.getClass().getName();
    }

    public String getType() {
        return type;
    }


    /**
     * Builds the model of the given request.
     *
     * @param request the request
     * @return the corresponding model
     */
    public static TransactionRestRequestModel<?> from(TransactionRequest<?> request) {

        if (request == null)
            throw new InternalFailureException("unexpected null request");
        else if (request instanceof ConstructorCallTransactionRequest)
            return new TransactionRestRequestModel<>(new ConstructorCallTransactionRequestModel((ConstructorCallTransactionRequest) request));
        else if (request instanceof GameteCreationTransactionRequest)
            return new TransactionRestRequestModel<>(new GameteCreationTransactionRequestModel((GameteCreationTransactionRequest) request));
        else if (request instanceof InitializationTransactionRequest)
            return new TransactionRestRequestModel<>(new InitializationTransactionRequestModel((InitializationTransactionRequest) request));
        else if (request instanceof InstanceMethodCallTransactionRequest)
            return new TransactionRestRequestModel<>(new InstanceMethodCallTransactionRequestModel((InstanceMethodCallTransactionRequest) request));
        else if (request instanceof JarStoreInitialTransactionRequest)
            return new TransactionRestRequestModel<>(new JarStoreInitialTransactionRequestModel((JarStoreInitialTransactionRequest) request));
        else if (request instanceof JarStoreTransactionRequest)
            return new TransactionRestRequestModel<>(new JarStoreTransactionRequestModel((JarStoreTransactionRequest) request));
        else if (request instanceof RedGreenGameteCreationTransactionRequest)
            return new TransactionRestRequestModel<>(new RedGreenGameteCreationTransactionRequestModel((RedGreenGameteCreationTransactionRequest) request));
        else if (request instanceof StaticMethodCallTransactionRequest)
            return new TransactionRestRequestModel<>(new StaticMethodCallTransactionRequestModel((StaticMethodCallTransactionRequest) request));
        else
            throw new InternalFailureException("unexpected transaction request of class " + request.getClass().getName());
    }
}
