import {SignatureModel} from "./SignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";

/**
 * The model of the signature of a field, method or constructor.
 */
export abstract class CodeSignatureModel extends SignatureModel {
    /**
     * The name of the class defining the field, method or constructor.
     */
    formals: Array<string>

    protected constructor(definingClass: string, formals: Array<string>) {
        super(definingClass)
        this.formals = formals
    }

    protected into(context: MarshallingContext): void {
        super.into(context)
        context.writeCompactInt(this.formals.length)
        this.formals.forEach(formal => new BasicType(formal).into(context))
    }
}