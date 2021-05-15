import {TransactionReferenceModel} from "./TransactionReferenceModel";

export interface StorageReferenceModel {
    transaction: TransactionReferenceModel
    progressive: string
}