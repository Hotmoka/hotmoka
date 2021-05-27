import {SignatureModel} from "./SignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";
import {ConstructorSignatureModel} from "./ConstructorSignatureModel";

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

    protected equals(other: any): boolean {
        return other instanceof ConstructorSignatureModel &&
            (other as ConstructorSignatureModel).definingClass === this.definingClass &&
             ConstructorSignatureModel.arrayEquals((other as ConstructorSignatureModel).formals, this.formals)
    }

    private static arrayEquals(arr1: Array<string>, arr2: Array<string>) {
        if (arr1.length !== arr2.length) {
            return false
        }
        // Check if all items exist and are in the same order
        for (let i = 0; i < arr1.length; i++) {
            if (arr1[i] !== arr2[i]) {
                return false
            }
        }
        return true
    }

    protected into(context: MarshallingContext): void {
        super.into(context)
        context.writeCompactInt(this.formals.length)
        this.formals.forEach(formal => new BasicType(formal).into(context))
    }
}