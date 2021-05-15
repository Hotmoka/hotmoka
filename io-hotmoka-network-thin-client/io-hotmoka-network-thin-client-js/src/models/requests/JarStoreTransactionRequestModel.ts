import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";

/**
 * The model of a jar store transaction request.
 */
export class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    jar: string
    dependencies: Array<TransactionReferenceModel>
    chainId: string
    signature: string

    constructor(caller: StorageReferenceModel,
                nonce: string,
                classpath: TransactionReferenceModel,
                gasLimit: string,
                gasPrice: string,
                jar: string,
                dependencies: Array<TransactionReferenceModel>,
                chainId: string,
                signature: string) {
        super(caller, nonce, classpath, gasLimit, gasPrice)
        this.jar = jar
        this.dependencies = dependencies
        this.chainId = chainId
        this.signature = signature
    }
}