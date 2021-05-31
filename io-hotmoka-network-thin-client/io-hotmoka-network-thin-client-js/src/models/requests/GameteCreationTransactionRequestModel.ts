import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {InitialTransactionRequestModel} from "./InitialTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

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

    protected into(context: MarshallingContext): void {
        //TODO
    }
}