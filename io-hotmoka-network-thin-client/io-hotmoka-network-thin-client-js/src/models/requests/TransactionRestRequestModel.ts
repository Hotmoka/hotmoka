/**
 * Class which wraps a type request model
 *
 * @param <T> the type request model
 */
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class TransactionRestRequestModel<T> extends Marshallable {
    /**
     * The request model which should be an instance of {@link TransactionRequestModel}.
     */
    transactionRequestModel: T

    /**
     * The runtime type of the request model
     */
    type: string

    constructor(transactionRequestModel: T, type: string) {
        super()
        this.transactionRequestModel = transactionRequestModel
        this.type = type
    }

    protected into(context: MarshallingContext): void {
        return
    }
}