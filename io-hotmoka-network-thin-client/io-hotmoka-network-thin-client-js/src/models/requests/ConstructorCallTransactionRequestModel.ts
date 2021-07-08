import {ConstructorSignatureModel} from "../signatures/ConstructorSignatureModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {CodeExecutionTransactionRequestModel} from "./CodeExecutionTransactionRequestModel";
import {Selectors} from "../../internal/marshalling/Selectors";
import {Signer} from "../../internal/signature/Signer";
import {HotmokaException} from "../../internal/exception/HotmokaException";
import {Signature} from "../../internal/signature/Signature";

export class ConstructorCallTransactionRequestModel extends CodeExecutionTransactionRequestModel {
    constructorSignature: ConstructorSignatureModel
    chainId: string
    signature: string


    constructor(caller: StorageReferenceModel,
                nonce: string,
                classpath: TransactionReferenceModel,
                gasLimit: string,
                gasPrice: string,
                constructorSignature: ConstructorSignatureModel,
                actuals: Array<StorageValueModel>,
                chainId: string,
                signature?: Signature
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, actuals)

        if (constructorSignature === null || constructorSignature === undefined) {
            throw new HotmokaException("constructor signature cannot be null or undefined")
        }

        const formalsLength = constructorSignature.formals ? constructorSignature.formals.length : 0
        const actualsLength = actuals ? actuals.length : 0

        if (formalsLength !== actualsLength) {
            throw new HotmokaException("argument count mismatch between formals and actuals")
        }

        if (chainId === null || chainId === undefined) {
            throw new HotmokaException("chainId cannot be null or undefined")
        }

        this.constructorSignature = constructorSignature
        this.chainId = chainId
        this.signature = signature ? Signer.INSTANCE.sign(signature, this.marshall()) : ''
    }

    public into(context: MarshallingContext): void {
        this.intoWithoutSignature(context)
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_CONSTRUCTOR_CALL)
        context.writeString(this.chainId)
        super.intoWithoutSignature(context)
        this.constructorSignature.into(context)
    }
}