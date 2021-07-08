import {MethodCallTransactionRequestModel} from "./MethodCallTransactionRequestModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {HotmokaException} from "../../internal/exception/HotmokaException";

export abstract class AbstractInstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {

    /**
     * The receiver of the call.
     */
    receiver: StorageReferenceModel

    protected constructor(
        caller: StorageReferenceModel,
        nonce: string,
        classpath: TransactionReferenceModel,
        gasLimit: string,
        gasPrice: string,
        method: MethodSignatureModel,
        actuals: Array<StorageValueModel>,
        receiver: StorageReferenceModel
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals)

        if (receiver === null || receiver === undefined) {
            throw new HotmokaException("receiver cannot be null or undefined")
        }

        this.receiver = receiver
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        super.intoWithoutSignature(context)
        StorageReferenceModel.intoWithoutSelector(context, this.receiver)
    }
}