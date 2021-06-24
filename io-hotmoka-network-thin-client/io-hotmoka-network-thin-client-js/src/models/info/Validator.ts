import {StorageReferenceModel} from "../values/StorageReferenceModel";

export class Validator {
    validator?: StorageReferenceModel
    id?: string
    balanceOfValidator?: string
    power?: string
    num?: number

    constructor(
        validator?: StorageReferenceModel,
        id?: string,
        balanceOfValidator?: string,
        power?: string,
        num?: number
    ) {
        this.validator = validator
        this.id = id
        this.balanceOfValidator = balanceOfValidator
        this.power = power
        this.num = num
    }
}