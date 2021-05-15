import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";

export class GameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
    initialAmount: string
    redInitialAmount: string;
    publicKey: string;
    classpath: TransactionReferenceModel

    constructor(initialAmount: string,
                redInitialAmount: string,
                publicKey: string,
                classpath: TransactionReferenceModel) {
        super()
        this.initialAmount = initialAmount
        this.redInitialAmount = redInitialAmount
        this.publicKey = publicKey
        this.classpath = classpath
    }
}