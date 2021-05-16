/**
 * Class which wraps a type response model
 *
 * @param <T> the type response model
 */
export class TransactionRestResponseModel<T> {

    /**
     * The response model which should be an instance of {@link TransactionResponseModel}.
     */
    transactionResponseModel: T

    /**
     * The runtime type of the response model
     */
    type: string

    constructor(transactionResponseModel: T, type: string) {
        this.transactionResponseModel = transactionResponseModel
        this.type = type
    }
}