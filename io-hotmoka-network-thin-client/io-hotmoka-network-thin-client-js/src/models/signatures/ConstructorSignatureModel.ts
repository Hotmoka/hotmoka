import {CodeSignatureModel} from "./CodeSignatureModel";

/**
 * The model of the signature of a constructor of a class.
 */
export class ConstructorSignatureModel extends CodeSignatureModel {

    constructor(definingClass: string, formals: Array<string>) {
        super(definingClass, formals)
    }
}