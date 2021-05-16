import {RemoteNode} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";
import {StateModel} from "../src/models/updates/StateModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";
import {TransactionRestRequestModel} from "../src/models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../src/models/responses/TransactionRestResponseModel";

describe('Testing the GET methods of a remote hotmoka node', () => {

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid takamakacode', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const result: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        expect(result.hash).to.be.not.null
        expect(result.hash).to.be.have.length.above(10)
    })


    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid manifest', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const result: StorageReferenceModel = await remoteNode.getManifest()

        expect(result.transaction).to.be.not.null
        expect(result.transaction.hash).to.be.not.null
        expect(result.transaction.hash).to.be.have.length.above(10)
    })

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid state model', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const manifest: StorageReferenceModel = await remoteNode.getManifest()
        const result: StateModel = await remoteNode.getState(manifest)

        expect(result.updates).to.be.not.null
        expect(result.updates.length).to.be.above(1)

        const update = result.updates[0]
        expect(update.field.definingClass).to.eql('io.takamaka.code.governance.Manifest')
        expect(update.object.transaction.hash).to.eql(manifest.transaction.hash)
    })

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid request for takamakacode', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const takamakacode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestRequestModel<unknown> = await remoteNode.getRequestAt(takamakacode)

        expect(result.transactionRequestModel).to.be.not.null
        expect(result.transactionRequestModel['jar']).to.be.not.null
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel')
    })

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid response for takamakacode', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const takamakacode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getResponseAt(takamakacode)

        expect(result.transactionResponseModel).to.be.not.null
        expect(result.transactionResponseModel['instrumentedJar']).to.be.not.null
        expect(result.transactionResponseModel['dependencies'].length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('it should respond with a valid polledResponse for takamakacode', async () => {
        const remoteNode = new RemoteNode("http://localhost:8080")

        const takamakacode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getPolledResponseAt(takamakacode)

        expect(result.transactionResponseModel).to.be.not.null
        expect(result.transactionResponseModel['instrumentedJar']).to.be.not.null
        expect(result.transactionResponseModel['dependencies'].length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })
})

