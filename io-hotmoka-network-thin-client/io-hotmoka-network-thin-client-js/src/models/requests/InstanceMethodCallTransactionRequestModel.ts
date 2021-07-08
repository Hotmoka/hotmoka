import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {CodeSignature} from "../../internal/lang/CodeSignature";
import {Selectors} from "../../internal/marshalling/Selectors";
import {AbstractInstanceMethodCallTransactionRequestModel} from "./AbstractInstanceMethodCallTransactionRequestModel";
import {Signer} from "../../internal/signature/Signer";
import {HotmokaException} from "../../internal/exception/HotmokaException";
import {Signature} from "../../internal/signature/Signature";

export class InstanceMethodCallTransactionRequestModel extends AbstractInstanceMethodCallTransactionRequestModel {
    /**
     * The chain identifier where this request can be executed, to forbid transaction replay across chains.
     */
    chainId: string

    /**
     * The signature of the request.
     */
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
        signature?: Signature
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals, receiver)

        if (chainId === null || chainId === undefined) {
            throw new HotmokaException("chainId cannot be null")
        }

        this.chainId = chainId
        this.signature = signature ? Signer.INSTANCE.sign(signature, this.marshall()) : ''
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
            StorageReferenceModel.intoWithoutSelector(context, this.caller)
            context.writeBigInteger(this.gasLimit)
            context.writeBigInteger(this.gasPrice)
            TransactionReferenceModel.into(context, this.classpath)
            context.writeBigInteger(this.nonce)
            StorageReferenceModel.intoWithoutSelector(context, this.receiver)

            if (this.actuals.length === 0) {
                throw new Error("Actuals required")
            }

            const howMuch = this.actuals[0]
            if (receiveInt) {
                context.writeInt(Number(howMuch.value))
            } else if (receiveLong) {
                context.writeLong(Number(howMuch.value))
            } else {
                context.writeBigInteger(howMuch.value ?? '0')
            }
        } else {
            super.intoWithoutSignature(context)
        }
    }
}