import {Validator} from "./Validator";
import {StorageReferenceModel} from "../values/StorageReferenceModel";

export class Validators {
    validatorsReference?: StorageReferenceModel
    validators: Array<Validator> = []
    numOfValidators?: string
    height?: string
    numberOfTransactions?: string
    ticketForNewPoll?: string
    numberOfPolls?: string

    constructor(
        validatorsReference?: StorageReferenceModel,
        numOfValidators?: string,
        height?: string,
        numberOfTransactions?: string,
        ticketForNewPoll?: string,
        numberOfPolls?: string
    ) {
        this.validatorsReference = validatorsReference
        this.numOfValidators = numOfValidators ?? '0'
        this.height = height ?? '0'
        this.numberOfTransactions = numberOfTransactions ?? '0'
        this.ticketForNewPoll = ticketForNewPoll ?? '0'
        this.numberOfPolls = numberOfPolls ?? '0'
    }
}