/**
 * The model of the signature of a field, method or constructor.
 */
export abstract class SignatureModel {
    /**
     * The name of the class defining the field, method or constructor.
     */
    definingClass: string

    protected constructor(definingClass: string) {
        this.definingClass = definingClass
    }
}