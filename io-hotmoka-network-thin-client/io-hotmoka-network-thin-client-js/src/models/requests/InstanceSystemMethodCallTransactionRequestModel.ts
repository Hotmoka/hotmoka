import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {AbstractInstanceMethodCallTransactionRequestModel} from "./AbstractInstanceMethodCallTransactionRequestModel";

export class InstanceSystemMethodCallTransactionRequestModel extends AbstractInstanceMethodCallTransactionRequestModel {

    constructor(
        caller: StorageReferenceModel,
        nonce: string,
        classpath: TransactionReferenceModel,
        gasLimit: string,
        gasPrice: string,
        method: MethodSignatureModel,
        actuals: Array<StorageValueModel>,
        receiver: StorageReferenceModel
    ) {
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals, receiver)
    }

    protected into(context: MarshallingContext): void {
       // nothing
    }
}