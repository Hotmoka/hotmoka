import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {TransactionRequestModel} from "./TransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {HotmokaException} from "../../internal/exception/HotmokaException";

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

        if (caller === null || caller === undefined) {
            throw new HotmokaException("caller cannot be null or undefined")
        }

        if (gasLimit === null || gasLimit === undefined) {
            throw new HotmokaException("gasLimit cannot be null")
        }

        if (Number(gasLimit) < 0) {
            throw new HotmokaException("gasLimit cannot be negative")
        }

        if (gasPrice === null || gasPrice === undefined) {
            throw new HotmokaException("gasPrice cannot be null")
        }

        if (Number(gasPrice) < 0) {
            throw new HotmokaException("gasPrice cannot be negative")
        }

        if (classpath === null || classpath === undefined) {
            throw new HotmokaException("classpath cannot be null or undefined")
        }

        if (nonce === null || nonce === undefined) {
            throw new HotmokaException("nonce cannot be null")
        }

        if (Number(nonce) < 0) {
            throw new HotmokaException("nonce cannot be negative")
        }

        this.caller = caller
        this.nonce = nonce
        this.classpath = classpath
        this.gasLimit = gasLimit
        this.gasPrice = gasPrice
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        StorageReferenceModel.intoWithoutSelector(context, this.caller)
        context.writeBigInteger(this.gasLimit)
        context.writeBigInteger(this.gasPrice)
        TransactionReferenceModel.into(context, this.classpath)
        context.writeBigInteger(this.nonce)
    }
}