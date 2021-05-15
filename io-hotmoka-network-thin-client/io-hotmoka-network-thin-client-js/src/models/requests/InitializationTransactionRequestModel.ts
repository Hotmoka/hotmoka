import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";

export class InitializationTransactionRequestModel extends InitialTransactionRequestModel {
    manifest: StorageReferenceModel
    classpath: TransactionReferenceModel

    constructor(manifest: StorageReferenceModel, classpath: TransactionReferenceModel) {
        super()
        this.manifest = manifest
        this.classpath = classpath
    }
}