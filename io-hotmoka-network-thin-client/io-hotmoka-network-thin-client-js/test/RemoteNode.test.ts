import {RemoteNode} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";
import {StateModel} from "../src/models/updates/StateModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";
import {TransactionRestRequestModel} from "../src/models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../src/models/responses/TransactionRestResponseModel";
import * as fs from "fs";
import * as path from "path"
import {JarStoreTransactionRequestModel} from "../src/models/requests/JarStoreTransactionRequestModel";
import {InstanceMethodCallTransactionRequestModel} from "../src/models/requests/InstanceMethodCallTransactionRequestModel";
import {MethodSignatureModel} from "../src/models/signatures/MethodSignatureModel";


const HOTMOKA_VERSION = "1.0.0"
const CHAIN_ID = "test"

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

describe('Testing the ADD methods of a remote hotmoka node', () => {

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    it('addJarStoreTransaction - it should add a valid jar transaction', async () => {
        const jar = getLocalJar("lambdas.jar")
        const remoteNode = new RemoteNode("http://localhost:8080")

        const manifest = await remoteNode.getManifest()
        const takamakacode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakacode)
        const nonceOfGamete = await getNonceOfGamete(gamete.reference, takamakacode)

        const request = new JarStoreTransactionRequestModel(
            gamete.reference,
            nonceOfGamete.value,
            takamakacode,
            "500000",
            "203377",
            jar.toString("base64"),
            [takamakacode],
            "test",
            ""
        )

        const result: TransactionReferenceModel = await remoteNode.addJarStoreTransaction(request)
        expect(result.hash).to.be.not.null
        expect(result.hash).to.be.have.length.above(10)
    })

})

const getLocalJar = (jarName: string): Buffer => {
    return fs.readFileSync(
        path.join(
            __dirname,
            "../../../io-hotmoka-examples/target/io-hotmoka-examples-" + HOTMOKA_VERSION + "-" + jarName
        )
    )
}

const getGamete = (manifest: StorageReferenceModel, takamakacode: TransactionReferenceModel) => {
    const remoteNode = new RemoteNode("http://localhost:8080")

    const methodSignature = new MethodSignatureModel(
        "getGamete",
        "io.takamaka.code.lang.Account",
        "io.takamaka.code.governance.Manifest",
        []
    )

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakacode,
        "15000",
        "0",
        methodSignature,
        [],
        manifest,
        CHAIN_ID,
        ""
    ))
}

const getNonceOfGamete = (gamete: StorageReferenceModel, takamakacode: TransactionReferenceModel) => {
    const remoteNode = new RemoteNode("http://localhost:8080")

    const methodSignature = new MethodSignatureModel(
        "nonce",
        "java.math.BigInteger",
        "io.takamaka.code.lang.Account",
        []
    )

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        gamete,
        "0",
        takamakacode,
        "15000",
        "0",
        methodSignature,
        [],
        gamete,
        CHAIN_ID,
        ""
    ))
}



