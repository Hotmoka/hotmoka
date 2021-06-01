import {MethodCallTransactionRequestModel} from "./MethodCallTransactionRequestModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {CodeSignature} from "../../internal/lang/CodeSignature";
import {Selectors} from "../../internal/marshalling/Selectors";

export class InstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
    receiver: StorageReferenceModel
    chainId: string
    signature: string

    constructor(
        caller: StorageReferenceModel,
        nonce: string,
        classpath: TransactionReferenceModel,
        gasLimit: string,
        gasPrice: string,
        method: MethodSignatureModel,
        actuals: Array<StorageValueModel>,
        receiver: StorageReferenceModel,
        chainId: string,
        signature: string
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals)
        this.receiver = receiver
        this.chainId = chainId
        this.signature = signature
    }

    public into(context: MarshallingContext): void {
       this.intoWithoutSignature(context)
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        const receiveInt = CodeSignature.RECEIVE_INT.equals(this.method)
        const receiveLong = CodeSignature.RECEIVE_LONG.equals(this.method)
        const receiveBigInteger = CodeSignature.RECEIVE_BIG_INTEGER.equals(this.method)

        if (receiveInt) {
            context.writeByte(Selectors.SELECTOR_TRANSFER_INT)
        } else if (receiveLong) {
            context.writeByte(Selectors.SELECTOR_TRANSFER_LONG)
        } else if (receiveBigInteger) {
            context.writeByte(Selectors.SELECTOR_TRANSFER_BIG_INTEGER)
        } else {
            context.writeByte(Selectors.SELECTOR_INSTANCE_METHOD_CALL)
        }

        context.writeString(this.chainId)

        if (receiveInt || receiveLong || receiveBigInteger) {
            this.caller.intoWithoutSelector(context)
            context.writeBigInteger(Number(this.gasLimit))
            context.writeBigInteger(Number(this.gasPrice))
            this.classpath.into(context)
            context.writeBigInteger(Number(this.nonce))
            this.receiver.intoWithoutSelector(context)

            if (this.actuals.length === 0) {
                throw new Error("Actuals required")
            }

            const howMuch = this.actuals[0]
            if (receiveInt) {
                context.writeInt(Number(howMuch.value))
            } else if (receiveLong) {
                context.writeLong(Number(howMuch.value))
            } else {
                context.writeBigInteger(Number(howMuch.value))
            }
        } else {
            super.intoWithoutSignature(context)
        }
    }
}