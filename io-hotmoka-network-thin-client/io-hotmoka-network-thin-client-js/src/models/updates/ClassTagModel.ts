import {TransactionReferenceModel} from "../values/TransactionReferenceModel";

/**
 * The model of the class tag of an object.
 */
export class ClassTagModel {
    /**
     * The name of the class of the object.
     */
    className: string

    /**
     * The transaction that installed the jar from where the class has been loaded.
     */
    jar: TransactionReferenceModel

    constructor(className: string, jar: TransactionReferenceModel) {
        this.className = className
        this.jar = jar
    }

}