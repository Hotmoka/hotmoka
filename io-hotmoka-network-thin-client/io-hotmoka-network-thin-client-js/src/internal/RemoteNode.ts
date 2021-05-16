import {Node} from "./Node"
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";
import {StateModel} from "../models/updates/StateModel";
import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import axios, {AxiosResponse} from "axios";
import {ErrorModel} from "../models/errors/ErrorModel";

export class RemoteNode implements Node {
    readonly url: string

    /**
     * It constructs the instance of the remote node.
     * @param url the url of the remote node
     */
    constructor(url: string) {
        this.url = url
    }

    /**
     * It resolves the given error received from the HTTP call.
     * @param error the error
     * @private
     * @return ErrorModel the parsed error model
     */
    private static resolveError(error): ErrorModel {
        if (error && error.response && error.response.data && error.response.data.message)
            return  new ErrorModel(error.response.data.message, error.response.data.exceptionClassName || 'Exception')
        else
            return new ErrorModel('Internal Server Error', 'Exception')
    }

    /**
     * Performs a GET request and yields a Promise of entity T as response.
     * @param url the url
     * @private
     * @return a Promise of entity T
     */
    private static async get<T>(url: string): Promise<T> {
        try {
            const res: AxiosResponse = await axios.get<T>(url)
            return res.data
        } catch (error) {
            throw RemoteNode.resolveError(error)
        }
    }

    async getClassTag(request: StorageReferenceModel): Promise<ClassTagModel> {
        // TODO
        return null
    }

    async getManifest(): Promise<StorageReferenceModel> {
        // TODO
        return null
    }

    async getState(): Promise<StateModel> {
        // TODO
        return null
    }

    async getTakamakaCode(): Promise<TransactionReferenceModel> {
        return await RemoteNode.get<TransactionReferenceModel>(this.url + '/get/takamakaCode')
    }
}