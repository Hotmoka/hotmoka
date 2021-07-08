import {expect} from "chai";
import {Signer} from "../src/internal/signature/Signer"
import {InstanceMethodCallTransactionRequestModel} from "../src";
import {StorageReferenceModel} from "../src";
import {TransactionReferenceModel} from "../src";
import {CodeSignature} from "../src";
import {VoidMethodSignatureModel} from "../src";
import {ClassType} from "../src";
import {BasicType} from "../src";
import {StorageValueModel} from "../src";
import {JarStoreTransactionRequestModel} from "../src";
import * as fs from "fs";
import * as path from "path";
import {StaticMethodCallTransactionRequestModel} from "../src";
import {NonVoidMethodSignatureModel} from "../src";
import {ConstructorSignatureModel} from "../src";
import {ConstructorCallTransactionRequestModel} from "../src";
import {Algorithm} from "../src";
import {Signature} from "../src";

const getPrivateKey = (pathFile: string): string => {
    return fs.readFileSync(path.resolve(pathFile), "utf8");
}
const HOTMOKA_VERSION = "1.0.1"
const SIGNATURE = new Signature(Algorithm.ED25519, getPrivateKey("./test/keys/gameteED25519.pri"))

describe('Testing the signed requests of the Hotmoka JS objects', () => {

    it('Signed string', async () => {

        const result = Signer.INSTANCE.sign(SIGNATURE, Buffer.from("hello"))
        expect(result).to.be.eq("mn+Rt4DL1EVH/kBtVm8l9y/7l5S7kJRz4XpqT6vf9ohOQFm2RSkqP8ucTh03KaOBKQclxfaOugfkeCYI9Dt7BA==")
    })

    it('new ConstructorCallTransactionRequestModel(..)', async () => {

        const constructorSignature = new ConstructorSignatureModel(
            ClassType.MANIFEST.name,
            [ClassType.BIG_INTEGER.name]
        )

        const request = new ConstructorCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "11500",
            "500",
            constructorSignature,
            [StorageValueModel.newStorageValue("999", ClassType.BIG_INTEGER.name)],
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('P9PDt2/BL/pBVcFVwf9LvsTyb65O0SRzNC8ZeAe9Zbmn4AqTYJcFdltrWBYOFSej2I/TU3ejQyqKpPfCfp/vDA==')
    })

    it('new InstanceMethodCallTransactionRequestModel(..) NonVoidMethod', async () => {

        const request = new InstanceMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            CodeSignature.GET_GAMETE,
            [],
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('vqn3qcfi3MMgJiSmLKcAtCJuX3bna3Qa+rgIku0owEhT3GaA7WwojtthtcmRKVuFj1wV+fVdgweqjNTH+FxDAg==')
    })

    it('new InstanceMethodCallTransactionRequestModel(..) VoidMethod', async () => {

        const RECEIVE_INT = new VoidMethodSignatureModel(
            "receive",
            ClassType.PAYABLE_CONTRACT.name,
            [BasicType.INT.name]
        )

        const request = new InstanceMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            RECEIVE_INT,
            [StorageValueModel.newStorageValue("300", BasicType.INT.name)],
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('8G7sgR0yhpRyS4dZc0sDiMRZZIkCh8m1eoFChSxWo5lL8SPtuxtoBLw4gwbN9dGLCUfqk3DpqUf5S0bwtVdMAA==')
    })


    it('new StaticMethodCallTransactionRequestModel(..) NonVoidMethod', async () => {

        const request = new StaticMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            CodeSignature.NONCE,
            [],
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('Q4oCMaptE+bLL5p5p+Uei6uINJ3TuB4/k3miqwjviKQ5ki0/oJ6hJI3xulbhaAhT5AV15P6Zy1XI2SjF9pPbDg==')
    })

    it('new StaticMethodCallTransactionRequestModel(..) VoidMethod', async () => {

        const RECEIVE_INT = new VoidMethodSignatureModel(
            "receive",
            ClassType.PAYABLE_CONTRACT.name,
            [BasicType.INT.name]
        )

        const request = new StaticMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            RECEIVE_INT,
            [StorageValueModel.newStorageValue("300", BasicType.INT.name)],
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('0NwNhHIZCf3TFj5OQupJruLlRGsiR91uPhUsHTpxADgTYgIGJTULSoUAYRak1WNBUBMDG8Icx2xz3gnzhgDzBA==')
    })

    it('new StaticMethodCallTransactionRequestModel(..) NonVoidMethod gas station', async () => {

        const nonVoidMethodSignature = new NonVoidMethodSignatureModel(
            "balance",
            ClassType.GAS_STATION.name,
            [ClassType.STORAGE.name],
            ClassType.BIG_INTEGER.name
        )

        const request = new StaticMethodCallTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            nonVoidMethodSignature,
            [StorageValueModel.newReference(new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ))],
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('gNJAD15DKlQeMqqgPAKQv2jx2V7loNfkLIvJhpvUeYIxrImlyk6OmdLaAb49bHrNYY2MJoI/ujd9SBDvbK7HAA==')
    })

    it.skip('new JarStoreTransactionRequestModel(..)', async () => {

        const request = new JarStoreTransactionRequestModel(
            new StorageReferenceModel(new TransactionReferenceModel(
                "local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"
                ), "0"
            ),
            "1",
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "5000",
            "4000",
            getLocalJar('lambdas.jar').toString('base64'),
            [],
            "chaintest",
            SIGNATURE
        )

        expect(request.signature).to.be.eq('n5KOY/VWbm5fcUP7qYnJogfaxanj2997EJSpREKBDXOG+PC2FXllXttYl0pHtlDUJ41JzqEJ9KkKsBVTC7kZAA==')
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