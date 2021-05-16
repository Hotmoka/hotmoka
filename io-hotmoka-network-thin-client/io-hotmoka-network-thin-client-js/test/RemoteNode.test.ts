import {RemoteNode} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";
import {StateModel} from "../src/models/updates/StateModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";

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
})

