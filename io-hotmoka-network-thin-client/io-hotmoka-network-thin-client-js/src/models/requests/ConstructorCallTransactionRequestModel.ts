import {ConstructorSignatureModel} from "../signatures/ConstructorSignatureModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {CodeExecutionTransactionRequestModel} from "./CodeExecutionTransactionRequestModel";
import {Selectors} from "../../internal/marshalling/Selectors";

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
                signature: string) {
        super(caller, nonce, classpath, gasLimit, gasPrice, actuals)
        this.constructorSignature = constructorSignature
        this.chainId = chainId
        this.signature = signature
    }

    protected into(context: MarshallingContext): void {
        this.intoWithoutSignature(context)

        // TODO
        // we add the signature
      /*  byte[] signature = getSignature();
        context.writeCompactInt(signature.length);
        context.write(signature);*/
    }

    protected intoWithoutSignature(context: MarshallingContext) {
        context.writeByte(Selectors.SELECTOR_CONSTRUCTOR_CALL)
        context.writeString(this.chainId)
        super.intoWithoutSignature(context)
        this.constructorSignature.into(context)
    }
}