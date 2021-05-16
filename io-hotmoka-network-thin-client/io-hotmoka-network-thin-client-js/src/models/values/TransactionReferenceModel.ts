/**
 * The model of a transaction reference.
 */
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
}