import {MethodSignatureModel} from "./MethodSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {Selectors} from "../../internal/marshalling/Selectors";
import {BasicType} from "../../internal/lang/BasicType";
import {ClassType} from "../../internal/lang/ClassType";
import {HotmokaException} from "../../internal/exception/HotmokaException";

export class NonVoidMethodSignatureModel extends MethodSignatureModel {
    /**
     * The return type of the method.
     */
    returnType: string

    constructor(methodName: string,
                definingClass: string,
                formals: Array<string>,
                returnType: string) {
        super(methodName, definingClass, formals)

        if (!returnType) {
            throw new HotmokaException("Invalid returnType " + returnType)
        }

        this.returnType = returnType
    }

    public into(context: MarshallingContext): void {
        context.writeByte(Selectors.SELECTOR_NON_VOID_METHOD)
        super.into(context)
        if (BasicType.isBasicType(this.returnType)) {
            new BasicType(this.returnType).into(context)
        } else {
            new ClassType(this.returnType).into(context)
        }
    }
}