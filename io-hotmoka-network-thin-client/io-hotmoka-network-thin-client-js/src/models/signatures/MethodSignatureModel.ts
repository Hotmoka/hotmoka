import {CodeSignatureModel} from "./CodeSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {HotmokaException} from "../../internal/exception/HotmokaException";

/**
 * The model of the signature of a method of a class.
 */
export abstract class MethodSignatureModel extends CodeSignatureModel {
    /**
     * The name of the method.
     */
    methodName: string


    constructor(methodName: string,
                definingClass: string,
                formals: Array<string>) {
        super(definingClass, formals)

        if (!methodName) {
            throw new HotmokaException("Invalid methodName " + methodName)
        }

        this.methodName = methodName
    }

    public equals(other: any): boolean {
        return other instanceof MethodSignatureModel &&
            (other as MethodSignatureModel).methodName === this.methodName &&
            super.equals(other)
    }

    public into(context: MarshallingContext): void {
        super.into(context)
        context.writeString(this.methodName)
    }
}