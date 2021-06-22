import {MethodCallTransactionRequestModel} from "./MethodCallTransactionRequestModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";
import {Signer} from "../../internal/signature/Signer";
import {HotmokaException} from "../../internal/HotmokaException";

export class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
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
        chainId: string
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals)

        if (chainId === null || chainId === undefined) {
            throw new HotmokaException("chainId cannot be null")
        }

        this.chainId = chainId
        this.signature = Signer.INSTANCE.sign(this.marshall())
    }

    public into(context: MarshallingContext): void {
        this.intoWithoutSignature(context)
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_STATIC_METHOD_CALL)
        context.writeString(this.chainId)
        super.intoWithoutSignature(context)
    }
}