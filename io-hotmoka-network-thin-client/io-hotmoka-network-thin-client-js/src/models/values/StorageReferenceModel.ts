import {TransactionReferenceModel} from "./TransactionReferenceModel";

export class StorageReferenceModel {
    transaction: TransactionReferenceModel
    progressive: string

    constructor(transaction: TransactionReferenceModel, progressive: string) {
        this.transaction = transaction
        this.progressive = progressive
    }
}