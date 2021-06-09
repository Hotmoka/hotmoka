/**
 * The model of a transaction reference.
 */
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class TransactionReferenceModel {
    /**
     * The type of transaction.
     */
    type: string
    /**
     * Used at least for local transactions.
     */
    hash: string

    constructor(type: string, hash: string) {
        this.type = type
        this.hash = hash
    }

    public static into(context: MarshallingContext, transactionReference: TransactionReferenceModel): void {
        context.writeTransactionReference(transactionReference)
    }

    /**
     * Marshals an array of transaction references into a given stream.
     * @param transactionReferences the array of transaction references
     * @param context the context holding the stream
     */
    public static intoArray(transactionReferences: Array<TransactionReferenceModel>, context: MarshallingContext): void {
        context.writeCompactInt(transactionReferences.length)
        transactionReferences.forEach(transactionReference => TransactionReferenceModel.into(context, transactionReference))
    }
}