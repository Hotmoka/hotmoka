import {CodeSignatureModel} from "./CodeSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";
import {ClassType} from "../../internal/lang/ClassType";
import {Selectors} from "../../internal/marshalling/Selectors";

/**
 * The model of the signature of a constructor of a class.
 */
export class ConstructorSignatureModel extends CodeSignatureModel {
    /**
     * The constructor ExternallyOwnedAccount(BigInteger, String).
     */
    public static readonly EOA_CONSTRUCTOR = new ConstructorSignatureModel(ClassType.EOA.name, [ClassType.BIG_INTEGER.name, ClassType.STRING.name]);


    constructor(definingClass: string, formals: Array<string>) {
        super(definingClass, formals)
    }

    public equals(other: any): boolean {
        return other instanceof ConstructorSignatureModel && super.equals(other)
    }

    public into(context: MarshallingContext): void {
        if (this.equals(ConstructorSignatureModel.EOA_CONSTRUCTOR)) {
            context.writeByte(Selectors.SELECTOR_CONSTRUCTOR_EOA)
        } else {
            context.writeByte(Selectors.SELECTOR_CONSTRUCTOR)
            super.into(context)
        }
    }
}