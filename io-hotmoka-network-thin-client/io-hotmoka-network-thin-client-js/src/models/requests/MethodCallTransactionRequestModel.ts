import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {CodeExecutionTransactionRequestModel} from "./CodeExecutionTransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {HotmokaException} from "../../internal/exception/HotmokaException";

/**
 * The model of a method call transaction request.
 */
export abstract class MethodCallTransactionRequestModel extends CodeExecutionTransactionRequestModel {
    method: MethodSignatureModel

    protected constructor(
        caller: StorageReferenceModel,
        nonce: string,
        classpath: TransactionReferenceModel,
        gasLimit: string,
        gasPrice: string,
        method: MethodSignatureModel,
        actuals: Array<StorageValueModel>) {

        super(caller, nonce, classpath, gasLimit, gasPrice, actuals)

        if (method === null || method === undefined) {
            throw new HotmokaException("method signature cannot be null or undefined")
        }

        const formalsLength = method.formals ? method.formals.length : 0
        const actualsLength = actuals ? actuals.length : 0

        if (formalsLength !== actualsLength) {
            throw new HotmokaException("argument count mismatch between formals and actuals")
        }

        this.method = method
    }

    protected intoWithoutSignature(context: MarshallingContext): void {
        super.intoWithoutSignature(context)
        this.method.into(context)
    }
}