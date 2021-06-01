/**
 * Class which wraps a type request model
 *
 * @param <T> the type request model
 */
export class TransactionRestRequestModel<T> {
    /**
     * The request model which should be an instance of {@link TransactionRequestModel}.
     */
    transactionRequestModel: T

    /**
     * The runtime type of the request model
     */
    type: string

    constructor(transactionRequestModel: T, type: string) {
        this.transactionRequestModel = transactionRequestModel
        this.type = type
    }
}