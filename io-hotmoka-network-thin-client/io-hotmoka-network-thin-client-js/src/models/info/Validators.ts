import {Validator} from "./Validator";

export class Validators {
    validators: Array<Validator> = []
    numOfValidators?: string
    height?: string
    numberOfTransactions?: string
    ticketForNewPoll?: string

    constructor(
        numOfValidators?: string,
        height?: string,
        numberOfTransactions?: string,
        ticketForNewPoll?: string
    ) {
        this.numOfValidators = numOfValidators ?? '0'
        this.height = height ?? '0'
        this.numberOfTransactions = numberOfTransactions ?? '0'
        this.ticketForNewPoll = ticketForNewPoll ?? '0'
    }
}