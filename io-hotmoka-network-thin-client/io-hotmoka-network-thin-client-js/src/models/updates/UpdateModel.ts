/**
 * The model of an update of an object.
 */
import {FieldSignatureModel} from "../signatures/FieldSignatureModel";
import {StorageValueModel} from "../values/StorageValueModel";
import {TransactionReferenceModel} from "../values/TransactionReferenceModel";
import {StorageReferenceModel} from "../values/StorageReferenceModel";

export class UpdateModel {
    /**
     * The field that is updated. This is {@code null} for class tags.
     */
    field: FieldSignatureModel

    /**
     * The value assigned to the updated field. This is {@code null} for class tags.
     */
    value: StorageValueModel

    /**
     * The name of the class of the object. This is non-{@code null} for class tags only.
     */
    className: string

    /**
     * The transaction that installed the jar from where the class has been loaded.
     * This is non-{@code null} for class tags only.
     */
    jar: TransactionReferenceModel

    /**
     * The object whose field is modified.
     */
    object: StorageReferenceModel

    constructor(
        field: FieldSignatureModel,
        value: StorageValueModel,
        className: string,
        jar: TransactionReferenceModel,
        object: StorageReferenceModel
    ) {
        this.field = field
        this.value = value
        this.className = className
        this.jar = jar
        this.object = object
    }

}