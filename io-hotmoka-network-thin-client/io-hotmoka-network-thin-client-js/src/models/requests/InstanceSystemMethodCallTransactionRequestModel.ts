import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {MethodCallTransactionRequestModel} from "./MethodCallTransactionRequestModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";

export class InstanceSystemMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
    receiver: StorageReferenceModel

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
        super(caller, nonce, classpath, gasLimit, gasPrice, method, actuals)
        this.receiver = receiver
    }
}