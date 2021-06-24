import {SignatureModel} from "./SignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {BasicType} from "../../internal/lang/BasicType";
import {ClassType} from "../../internal/lang/ClassType";
import {HotmokaException} from "../../internal/exception/HotmokaException";


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

        if (formals === null || formals === undefined) {
            throw new HotmokaException("formals cannot be null or undefined")
        }

        for (const formal of formals) {
            if (formal === null || formal === undefined) {
                throw new HotmokaException("formals cannot hold null or undefined")
            }
        }

        this.formals = formals
    }

    protected equals(other: any): boolean {
        return (other as CodeSignatureModel).definingClass === this.definingClass &&
             CodeSignatureModel.arrayEquals((other as CodeSignatureModel).formals, this.formals)
    }

    protected into(context: MarshallingContext): void {
        super.into(context)
        context.writeCompactInt(this.formals.length)
        this.formals.forEach(formal => {
            if (BasicType.isBasicType(formal)) {
                new BasicType(formal).into(context)
            } else {
                new ClassType(formal).into(context)
            }
        })
    }

    /**
     * Checks if two string arrays are equal and in the same order.
     * @param arr1 the first array
     * @param arr2 the second array
     * @return true if the are equal, false otherwise
     */
    private static arrayEquals(arr1: Array<string>, arr2: Array<string>): boolean {
        if (arr1.length !== arr2.length) {
            return false
        }
        for (let i = 0; i < arr1.length; i++) {
            if (arr1[i] !== arr2[i]) {
                return false
            }
        }
        return true
    }
}