/**
 * The model of the state of an object: just the set of its updates.
 */
import {UpdateModel} from "./UpdateModel";

export class StateModel {
    updates: Array<UpdateModel>

    constructor(updates: Array<UpdateModel>) {
        this.updates = updates
    }
}