import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {TransactionRequestModel} from "./TransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export abstract class NonInitialTransactionRequestModel extends TransactionRequestModel {
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
        super()
        this.caller = caller
        this.nonce = nonce
        this.classpath = classpath
        this.gasLimit = gasLimit
        this.gasPrice = gasPrice
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        StorageReferenceModel.intoWithoutSelector(context, this.caller)
        context.writeBigInteger(Number(this.gasLimit))
        context.writeBigInteger(Number(this.gasPrice))
        TransactionReferenceModel.into(context, this.classpath)
        context.writeBigInteger(Number(this.nonce))
    }
}