import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {NonInitialTransactionRequestModel} from "./NonInitialTransactionRequestModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";

/**
 * The model of a method call transaction request.
 */
export abstract class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    method: MethodSignatureModel
    actuals: Array<StorageValueModel>

    constructor(
        caller: StorageReferenceModel,
        nonce: string,
        classpath: TransactionReferenceModel,
        gasLimit: string,
        gasPrice: string,
        method: MethodSignatureModel,
        actuals: Array<StorageValueModel>) {
        super(caller, nonce, classpath, gasLimit, gasPrice)
        this.method = method
        this.actuals = actuals
    }
}