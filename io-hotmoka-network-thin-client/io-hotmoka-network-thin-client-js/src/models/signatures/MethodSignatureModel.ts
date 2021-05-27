import {CodeSignatureModel} from "./CodeSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

/**
 * The model of the signature of a method of a class.
 */
export class MethodSignatureModel extends CodeSignatureModel {
    /**
     * The name of the method.
     */
    methodName: string

    /**
     * The return type of the method, if any.
     */
    returnType: string

    constructor(methodName: string, returnType: string, definingClass: string, formals: Array<string>) {
        super(definingClass, formals)
        this.methodName = methodName
        this.returnType = returnType
    }

    public into(context: MarshallingContext): void {
        super.into(context)
        context.writeString(this.methodName)
    }
}