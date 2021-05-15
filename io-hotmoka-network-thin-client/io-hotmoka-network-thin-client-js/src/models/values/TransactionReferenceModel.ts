/**
 * The model of a transaction reference.
 */
export interface TransactionReferenceModel {
    /**
     * The type of transaction.
     */
    type: string
    /**
     * Used at least for local transactions.
     */
    hash: string
}