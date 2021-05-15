/**
 * The model of the state of an object: just the set of its updates.
 */
import {UpdateModel} from "./UpdateModel";

export class StateModel {
    update: Array<UpdateModel>

    constructor(update: Array<UpdateModel>) {
        this.update = update
    }
}