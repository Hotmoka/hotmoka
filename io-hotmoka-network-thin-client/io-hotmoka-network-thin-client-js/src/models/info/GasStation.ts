import {StorageReferenceModel} from "../values/StorageReferenceModel";

export class GasStation {
    gasStation?: StorageReferenceModel
    gasPrice?: string
    maxGasPerTransaction?: string
    ignoresGasPrice?: boolean
    targetGasAtReward?: string
    inflation?: string
    oblivion?: string
    inflationInfo: string
    oblivionInfo: string

    constructor(
        gasStation?: StorageReferenceModel,
        gasPrice?: string,
        maxGasPerTransaction?: string,
        ignoresGasPrice?: string,
        targetGasAtReward?: string,
        inflation?: string,
        oblivion?: string
        ) {
        this.gasStation = gasStation
        this.gasPrice = gasPrice ?? '0'
        this.maxGasPerTransaction = maxGasPerTransaction ?? '0'
        this.ignoresGasPrice = ignoresGasPrice ? 'true' === ignoresGasPrice : false
        this.targetGasAtReward = targetGasAtReward ?? '0'
        this.inflation = inflation ?? '0'
        this.oblivion = oblivion ?? '0'
        this.inflationInfo = this.inflation !== '0' ? (Number(this.inflation) / 100000.0).toFixed(2) + '%' : ''
        this.oblivionInfo = this.oblivion !== '0' ? (100.0 * Number(this.oblivion) / 1000000).toFixed(2) + '%' : ''
    }


}