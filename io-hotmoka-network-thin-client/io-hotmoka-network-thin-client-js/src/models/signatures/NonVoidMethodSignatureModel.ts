import {MethodSignatureModel} from "./MethodSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

export class NonVoidMethodSignatureModel extends MethodSignatureModel {
    /**
     * The return type of the method, if any.
     */
    returnType: string

    constructor(methodName: string,
                definingClass: string,
                formals: Array<string>,
                returnType: string) {
        super(methodName, definingClass, formals)
        this.returnType = returnType
    }

    public into(context: MarshallingContext) {
        // TODO
    }
}