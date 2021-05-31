import {StorageReferenceModel} from "../values/StorageReferenceModel";
import {MethodCallTransactionRequestModel} from "./MethodCallTransactionRequestModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {MethodSignatureModel} from "../signatures/MethodSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";
import {Marshallable} from "../../internal/marshalling/Marshallable";

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

    protected into(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_INSTANCE_SYSTEM_METHOD_CALL)
        this.caller.intoWithoutSelector(context)
        context.writeBigInteger(Number(this.gasLimit))
        this.classpath.into(context)
        context.writeBigInteger(Number(this.nonce))
        Marshallable.intoArray(this.actuals, context)
        this.method.into(context)
        this.receiver.intoWithoutSelector(context)
    }
}