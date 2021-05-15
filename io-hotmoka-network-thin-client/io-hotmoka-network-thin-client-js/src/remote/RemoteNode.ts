import {Node} from "./Node"
import {StorageReferenceModel} from "../models/values/StorageReferenceModel";
import {ClassTagModel} from "../models/updates/ClassTagModel";
import {StateModel} from "../models/updates/StateModel";
import {TransactionReferenceModel} from "../models/values/TransactionReferenceModel";
import axios, {AxiosResponse} from "axios";

export class RemoteNode implements Node {
    readonly url: string

    constructor(url: string) {
        this.url = url
    }


    private static invoke<T>(callback: () => Promise<T>) {
        try {
          return callback()
        } catch (error) {
            console.log(error)
            throw error
        }
    }

    private static invokeGet<T>(url: string) {
        return RemoteNode.invoke(async () => {
            const res: AxiosResponse = await axios.get<T>(url)
            return res.data
        })
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
        return await RemoteNode.invokeGet<TransactionReferenceModel>(this.url + '/get/takamakaCode')
    }
}