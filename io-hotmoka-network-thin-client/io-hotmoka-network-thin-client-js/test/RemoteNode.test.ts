import {RemoteNode, Signature} from "../src"
import {expect} from 'chai';
import {TransactionReferenceModel} from "../src";
import {StateModel} from "../src";
import {StorageReferenceModel} from "../src";
import {TransactionRestRequestModel} from "../src/models/requests/TransactionRestRequestModel";
import {TransactionRestResponseModel} from "../src";
import * as fs from "fs";
import * as path from "path"
import {JarStoreTransactionRequestModel} from "../src";
import {InstanceMethodCallTransactionRequestModel} from "../src";
import {SignatureAlgorithmResponseModel} from "../src";
import {ConstructorCallTransactionRequestModel} from "../src";
import {ConstructorSignatureModel} from "../src";
import {StorageValueModel} from "../src";
import {JarStoreInitialTransactionResponseModel} from "../src";
import {NonVoidMethodSignatureModel} from "../src";
import {CodeSignature} from "../src";
import {BasicType} from "../src";
import {Algorithm} from "../src";
import {StaticMethodCallTransactionRequestModel} from "../src";
import {ConstructorCallTransactionSuccessfulResponseModel} from "../src";
import {MethodCallTransactionSuccessfulResponseModel} from "../src";
import {InfoModel} from "../src";
import assert = require("assert");

const getPrivateKey = (pathFile: string): string => {
    return fs.readFileSync(path.resolve(pathFile), "utf8");
}


const HOTMOKA_VERSION = "1.0.1"
const CHAIN_ID = "chain-btmZzq"
const REMOTE_NODE_URL = "http://panarea.hotmoka.io"
const basicJarClasspath = new TransactionReferenceModel("local", "35ec7fdea4fdf8a25ab562256d3f1b8cf489d411148633658631179ed23c81a5")
const SIGNATURE = new Signature(Algorithm.ED25519, getPrivateKey("./test/keys/eoa.pri"))
const EOA = new StorageReferenceModel(new TransactionReferenceModel("local", "60d7d1db2559b628a3e9904f589ed681ca8dbf770995558375e3349c21a516de"), "0")
const gasLimit = "500000"


describe('Testing the GET methods of a remote hotmoka node', () => {

    it('getTakamakaCode - it should respond with a valid takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const result: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        
        expect(result.hash).to.be.not.null
        expect(result.hash).to.be.have.length.above(10)
    })

    it('getManifest - it should respond with a valid manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const result: StorageReferenceModel = await remoteNode.getManifest()

        expect(result.transaction).to.be.not.null
        expect(result.transaction.hash).to.be.not.null
        expect(result.transaction.hash).to.be.have.length.above(10)
    })

    it('getState - it should respond with a valid state model of the manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest: StorageReferenceModel = await remoteNode.getManifest()
        const result: StateModel = await remoteNode.getState(manifest)

        expect(result.updates).to.be.not.null
        expect(result.updates.length).to.be.above(1)

        const manifestUpdate = result.updates.filter(update => update.className === 'io.takamaka.code.governance.Manifest')
        expect(manifestUpdate).to.not.be.empty
        expect(manifestUpdate[0].object.transaction.hash).to.eql(manifest.transaction.hash)
    })

    it('getRequestAt - it should respond with a valid request for takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestRequestModel<unknown> = await remoteNode.getRequestAt(takamakaCode)
        const jarStoreTransaction = result.transactionRequestModel as JarStoreTransactionRequestModel

        expect(result.transactionRequestModel).to.be.not.null
        expect(jarStoreTransaction.jar).to.be.not.null
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.requests.JarStoreInitialTransactionRequestModel')
    })

    it('getResponseAt - it should respond with a valid response for takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getResponseAt(takamakaCode)
        const jarStoreInitialTransaction = result.transactionResponseModel as JarStoreInitialTransactionResponseModel

        expect(result.transactionResponseModel).to.be.not.null
        expect(jarStoreInitialTransaction.instrumentedJar).to.be.not.null
        expect(jarStoreInitialTransaction.dependencies.length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    it('getPolledResponseAt - it should respond with a valid polledResponse for takamakaCode', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode: TransactionReferenceModel = await remoteNode.getTakamakaCode()
        const result: TransactionRestResponseModel<unknown> = await remoteNode.getPolledResponseAt(takamakaCode)
        const jarStoreInitialTransaction = result.transactionResponseModel as JarStoreInitialTransactionResponseModel

        expect(result.transactionResponseModel).to.be.not.null
        expect(jarStoreInitialTransaction.instrumentedJar).to.be.not.null
        expect(jarStoreInitialTransaction.dependencies.length).to.be.eql(0)
        expect(result.type).to.be.not.null
        expect(result.type).to.be.eql('io.hotmoka.network.responses.JarStoreInitialTransactionResponseModel')
    })

    it('getNameOfSignatureAlgorithmForRequests - it should respond with a signature algorithm', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const result: SignatureAlgorithmResponseModel = await remoteNode.getNameOfSignatureAlgorithmForRequests()

        expect(result).to.be.not.null
        expect(result.algorithm).to.be.not.null
        expect(result.algorithm).to.be.not.empty
        expect(result.algorithm).to.be.eql(Algorithm[SIGNATURE.algorithm].toLocaleLowerCase())
    })
})


describe('Testing the RUN methods of a remote hotmoka node', () => {

    it('runInstanceMethodCallTransaction - getGasStation of manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)

        expect(gasStation).to.be.not.null
        expect(gasStation.reference).to.be.not.null
        expect(gasStation.reference?.transaction).to.be.not.null
        expect(gasStation.reference?.transaction.hash).to.be.not.null
    })

    it('runInstanceMethodCallTransaction - getGasPrice of manifest', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)

        expect(gasPrice.value).to.be.not.null
        expect(Number(gasPrice.value)).to.be.gte(1)
    })

    it('runInstanceMethodCallTransaction - getGamete', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)

        expect(gamete).to.be.not.null
        expect(gamete.reference).to.be.not.null
        expect(gamete.reference?.transaction).to.be.not.null
        expect(gamete.reference?.transaction.hash).to.be.not.null
    })

    it('runInstanceMethodCallTransaction - getNonceOf gamete', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)
        const takamakaCode = await remoteNode.getTakamakaCode()
        const manifest = await remoteNode.getManifest()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        expect(nonceOfGamete).to.be.not.null
        expect(nonceOfGamete.value).to.be.not.null
        expect(Number(nonceOfGamete.value)).to.be.gt(1)
    })
})

describe.skip('Testing the io-hotmoka-examples-1.0.1-lambdas.jar methods of a remote hotmoka node', () => {
    let lambdasJarClasspath: TransactionReferenceModel
    let lambdasStorageReference: StorageReferenceModel

    it.skip('addJarStoreTransaction - it should add a valid jar transaction', async () => {
        const jar = getLocalJar("lambdas.jar")
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        const request = new JarStoreTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            takamakaCode,
            "500000",
            "203377",
            jar.toString("base64"),
            [takamakaCode],
            "test"
        )

        lambdasJarClasspath = await remoteNode.addJarStoreTransaction(request)
        expect(lambdasJarClasspath.hash).to.be.not.null
        expect(lambdasJarClasspath.hash).to.be.have.length.above(10)
    })

    it.skip('addConstructorCallTransaction - it should invoke new Lambdas()', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        const constructorSignature = new ConstructorSignatureModel(
            "io.hotmoka.examples.lambdas.Lambdas",
            ["java.math.BigInteger", "java.lang.String"]
        )
        const actuals = [StorageValueModel.newStorageValue("100000", "java.math.BigInteger"), StorageValueModel.newStorageValue("", "java.lang.String")]

        const request = new ConstructorCallTransactionRequestModel(
            gamete.reference!,
            nonceOfGamete.value!,
            lambdasJarClasspath,
            "500000",
            "1",
            constructorSignature,
            actuals,
            CHAIN_ID
        )

        lambdasStorageReference = await remoteNode.addConstructorCallTransaction(request)

        expect(lambdasStorageReference.transaction).to.be.not.null
        expect(lambdasStorageReference.transaction.hash).to.be.have.length.above(10)
    })


    it.skip('addInstanceMethodCallTransaction - it should invoke new Lambdas().whiteListChecks(13,1,1973) == 7', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gamete = await getGamete(manifest, takamakaCode)
        const nonceOfGamete = await getNonceOf(gamete.reference!, takamakaCode)

        const result: StorageValueModel = await remoteNode.addInstanceMethodCallTransaction(
            new InstanceMethodCallTransactionRequestModel(
                gamete.reference!,
                nonceOfGamete.value!,
                lambdasJarClasspath,
                "500000",
                "1",
                new NonVoidMethodSignatureModel(
                    "whiteListChecks",
                    "io.hotmoka.examples.lambdas.Lambdas",
                    ["java.lang.Object", "java.lang.Object", "java.lang.Object"],
                    "int"
                ),
                [StorageValueModel.newStorageValue("13", "java.math.BigInteger"), StorageValueModel.newStorageValue("1", "java.math.BigInteger"), StorageValueModel.newStorageValue("1973", "java.math.BigInteger")],
                lambdasStorageReference,
                CHAIN_ID
            )
        )

        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql("7")
    })

})


describe('Testing the io-hotmoka-examples-1.0.1-basic.jar of a remote hotmoka node [ADD version]', () => {
   let simpleStorageReference: StorageReferenceModel

    it('addConstructorCallTransaction - it should invoke new Simple(13)', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const takamakaCode = await remoteNode.getTakamakaCode()
        const manifest = await remoteNode.getManifest()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)

        // constructor call
        const requestConstructorCall = new ConstructorCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            gasLimit,
            gasPrice.value!,
            new ConstructorSignatureModel(
                "io.hotmoka.examples.basic.Simple",
                [BasicType.INT.name]
            ),
            [StorageValueModel.newStorageValue("13", BasicType.INT.name)],
            CHAIN_ID
        )

        simpleStorageReference = await remoteNode.addConstructorCallTransaction(requestConstructorCall)
        expect(simpleStorageReference).to.be.not.null
        expect(simpleStorageReference.transaction).to.be.not.null
        expect(simpleStorageReference.transaction.hash).to.be.not.null
    })


    it('runInstanceMethodCallTransaction - it should invoke simple.foo3() == 13', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)

        // method call
        const requestInstanceMethodCall = new InstanceMethodCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            gasLimit,
            gasPrice.value!,
            new NonVoidMethodSignatureModel(
                "foo3",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            simpleStorageReference,
            CHAIN_ID
        )

        const result = await remoteNode.runInstanceMethodCallTransaction(requestInstanceMethodCall)
        expect(result).to.be.not.null
        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql('13')
    })


    it('addStaticMethodCallTransaction - it should invoke Simple.foo5() == 14', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)

        // method call
        const requestInstanceMethodCall = new StaticMethodCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            gasLimit,
            gasPrice.value!,
            new NonVoidMethodSignatureModel(
                "foo5",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            CHAIN_ID
        )

        const result = await remoteNode.addStaticMethodCallTransaction(requestInstanceMethodCall)
        expect(result).to.be.not.null
        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql('14')
    })

    it('runStaticMethodCallTransaction - it should invoke Simple.foo5() == 14', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)

        // method call
        const requestInstanceMethodCall = new StaticMethodCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            gasLimit,
            gasPrice.value!,
            new NonVoidMethodSignatureModel(
                "foo5",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            CHAIN_ID
        )

        const result = await remoteNode.runStaticMethodCallTransaction(requestInstanceMethodCall)
        expect(result).to.be.not.null
        expect(result.value).to.be.not.null
        expect(result.value).to.be.eql('14')
    })

})



describe('Testing the io-hotmoka-examples-1.0.0-basic.jar of a remote hotmoka node [POST version]', () => {

    it('postConstructorCallTransaction - it should invoke new Simple(13)', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)

        // constructor call
        const requestConstructorCall = new ConstructorCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            gasLimit,
            gasPrice.value!,
            new ConstructorSignatureModel(
                "io.hotmoka.examples.basic.Simple",
                [BasicType.INT.name]
            ),
            [StorageValueModel.newStorageValue("13", BasicType.INT.name)],
            CHAIN_ID
        )

        const promiseResult = await remoteNode.postConstructorCallTransaction(requestConstructorCall)
        const result = await remoteNode.getResponseAt(promiseResult)
        const successfulTransaction = result.transactionResponseModel as ConstructorCallTransactionSuccessfulResponseModel

        expect(successfulTransaction).to.be.not.null
        const simpleStorageReference = successfulTransaction.newObject
        expect(simpleStorageReference).to.be.not.null
        expect(simpleStorageReference.transaction).to.be.not.null
        expect(simpleStorageReference.transaction.hash).to.be.not.null
    }).timeout(3000)


    it('postStaticMethodCallTransaction - it should invoke Simple.foo5() == 14', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

        const manifest = await remoteNode.getManifest()
        const takamakaCode = await remoteNode.getTakamakaCode()
        const gasStation = await getGasStation(manifest, takamakaCode)
        const gasPrice = await getGasPrice(manifest, takamakaCode, gasStation)
        const nonceOfEOA = await getNonceOf(EOA, takamakaCode)

        // method call
        const requestInstanceMethodCall = new StaticMethodCallTransactionRequestModel(
            EOA,
            nonceOfEOA.value!,
            basicJarClasspath,
            gasLimit,
            gasPrice.value!,
            new NonVoidMethodSignatureModel(
                "foo5",
                "io.hotmoka.examples.basic.Simple",
                [],
                BasicType.INT.name
            ),
            [],
            CHAIN_ID
        )

        const promiseResult = await remoteNode.postStaticMethodCallTransaction(requestInstanceMethodCall)
        const result = await remoteNode.getPolledResponseAt(promiseResult)
        const successfulTransaction = result.transactionResponseModel as MethodCallTransactionSuccessfulResponseModel

        expect(successfulTransaction).to.be.not.null
        expect(successfulTransaction.result).to.be.not.null
        expect(successfulTransaction.result.value).to.be.not.null
        expect(successfulTransaction.result.value).to.be.eql('14')
    }).timeout(3000)

})

describe('Testing the Info of a remote hotmoka node', () => {

    it('info - it should respond with all the info of the remote node', async () => {
        const remoteNode = new RemoteNode(REMOTE_NODE_URL)
        const info: InfoModel = await remoteNode.info()
        const gameteInfo = info.gameteInfo
        const gasStation = info.gasStation
        const validators = info.validators

        if (!gameteInfo) {
            assert.fail('missing gamete info')
        }

        if (!gasStation) {
            assert.fail('missing gasStation info')
        }

        if (!validators) {
            assert.fail('missing validators info')
        }

        expect(info.takamakaCode!.hash).to.be.eql('bbdc415227b228422093d6248cc590d3ea7c2d74bddb65664cb59a393953fa0e')
        expect(info.chainId).to.be.eql(CHAIN_ID)
        expect(info.maxErrorLength).to.be.eql(300)
        expect(info.maxCumulativeSizeOfDependencies).to.be.eql(10000000)
        expect(info.maxDependencies).to.be.eql(20)
        expect(info.allowsSelfCharged).to.be.eql(false)
        expect(info.allowsUnsignedFaucet).to.be.eql(true)
        expect(info.skipsVerification).to.be.eql(false)
        expect(info.signature).to.be.eql('ed25519')
        expect(info.verificationVersion).to.be.eql('0')
        expect(info.versions!.transaction.hash).to.be.eql('1a73e76ebd37e7a20d17388616f3cceebd640a5fe4341fe58555fbaf8d940e52')

        // gamete
        expect(gameteInfo.gamete).to.be.not.undefined
        expect(gameteInfo.balanceOfGamete).to.be.not.undefined
        expect(gameteInfo.redBalance).to.be.eql('0')
        expect(gameteInfo.maxFaucet).to.be.eql('100000000000000000000000')
        expect(gameteInfo.maxRedFaucet).to.be.eql('0')

        // gasStation
        expect(gasStation.gasStation).to.be.not.undefined
        expect(gasStation.gasPrice).to.be.eql('1')
        expect(gasStation.maxGasPerTransaction).to.be.eql('1000000000')
        expect(gasStation.ignoresGasPrice).to.be.eql(false)
        expect(gasStation.targetGasAtReward).to.be.eql('1000000')
        expect(gasStation.inflation).to.be.eql('10000')
        expect(gasStation.oblivion).to.be.eql('250000')

        // validators
        expect(validators.validatorsReference).to.be.not.undefined
        expect(Number(validators.numOfValidators)).to.be.eql(1)
        expect(Number(validators.height)).to.be.gte(1)
        expect(Number(validators.numberOfTransactions)).to.be.gte(1)
        expect(Number(validators.ticketForNewPoll)).to.be.gte(100)
        expect(Number(validators.numberOfPolls)).to.be.gte(0)
        expect(validators.validators.length).to.be.gt(0)

        const validator = validators.validators[0]
        expect(validator.validator).to.be.not.undefined
        expect(validator.validator!.transaction.hash).to.be.eql('1a73e76ebd37e7a20d17388616f3cceebd640a5fe4341fe58555fbaf8d940e52')
        expect(validator.id).to.be.not.undefined
        expect(validator.id).to.be.eql('016C31DDFCD28FC55653279891B1A57EAB2824F2')
        expect(Number(validator.balanceOfValidator)).to.be.gt(100000)
        expect(Number(validator.power)).to.be.gte(1)

    }).timeout(5000)


})

const getLocalJar = (jarName: string): Buffer => {
    return fs.readFileSync(
        path.join(
            __dirname,
            "../../../io-hotmoka-examples/target/io-hotmoka-examples-" + HOTMOKA_VERSION + "-" + jarName
        )
    )
}

const getGamete = async (manifest: StorageReferenceModel, takamakaCode: TransactionReferenceModel): Promise<StorageValueModel> => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.GET_GAMETE,
        [],
        manifest,
        CHAIN_ID
    ))
}

const getNonceOf = async (eoa: StorageReferenceModel, takamakaCode: TransactionReferenceModel): Promise<StorageValueModel> => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        eoa,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.NONCE,
        [],
        eoa,
        CHAIN_ID
    ))
}


const getGasStation = async (manifest: StorageReferenceModel, takamakaCode: TransactionReferenceModel): Promise<StorageValueModel> => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.GET_GAS_STATION,
        [],
        manifest,
        CHAIN_ID
    ))
}


const getGasPrice = async (manifest: StorageReferenceModel, takamakaCode: TransactionReferenceModel, gasStation: StorageValueModel): Promise<StorageValueModel> => {
    const remoteNode = new RemoteNode(REMOTE_NODE_URL, SIGNATURE)

    const ignoresGasPrice = await remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.IGNORES_GAS_PRICE,
        [],
        gasStation.reference!,
        CHAIN_ID
    ))

    if (ignoresGasPrice.value && ignoresGasPrice.value === 'true') {
        return Promise.resolve(StorageValueModel.newStorageValue("1", "int"))
    }

    return remoteNode.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequestModel(
        manifest,
        "0",
        takamakaCode,
        "100000",
        "0",
        CodeSignature.GET_GAS_PRICE,
        [],
        gasStation.reference!,
        CHAIN_ID
    ))
}



