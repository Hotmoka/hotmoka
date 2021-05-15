import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";

export abstract class NonInitialTransactionRequestModel {
    caller: StorageReferenceModel
    nonce: string
    classpath: TransactionReferenceModel
    gasLimit: string
    gasPrice: string

    protected constructor(caller: StorageReferenceModel,
                          nonce: string,
                          classpath: TransactionReferenceModel,
                          gasLimit: string,
                          gasPrice: string) {
        this.caller = caller
        this.nonce = nonce
        this.classpath = classpath
        this.gasLimit = gasLimit
        this.gasPrice = gasPrice
    }
}