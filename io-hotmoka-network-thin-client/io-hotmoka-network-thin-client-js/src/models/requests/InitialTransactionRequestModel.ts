import {TransactionRequestModel} from "./TransactionRequestModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export abstract class InitialTransactionRequestModel extends TransactionRequestModel {

    protected into(context: MarshallingContext): void {
        // empty
    }
}