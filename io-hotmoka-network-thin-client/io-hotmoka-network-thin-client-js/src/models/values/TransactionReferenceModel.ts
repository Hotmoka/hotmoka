/**
 * The model of a transaction reference.
 */
import {Marshallable} from "../../internal/marshalling/Marshallable";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class TransactionReferenceModel extends Marshallable {
    /**
     * The type of transaction.
     */
    type: string
    /**
     * Used at least for local transactions.
     */
    hash: string

    constructor(type: string, hash: string) {
        super()
        this.type = type
        this.hash = hash
    }

    public into(context: MarshallingContext): void {
        context.writeTransactionReference(this)
    }

    protected intoWithoutSelector(context: MarshallingContext): void {
        // nothing
    }
}