import {expect} from "chai";
import {MarshallingContext} from "../src/internal/marshalling/MarshallingContext";
import {StorageReferenceModel} from "../src";
import {TransactionReferenceModel} from "../src";
import {ClassType} from "../src";
import {BasicType} from "../src";
import {StorageValueModel} from "../src";
import {ConstructorSignatureModel} from "../src";
import {FieldSignatureModel} from "../src";
import {ConstructorCallTransactionRequestModel} from "../src";
import {NonVoidMethodSignatureModel} from "../src";
import {StaticMethodCallTransactionRequestModel} from "../src";
import {VoidMethodSignatureModel} from "../src";
import {JarStoreTransactionRequestModel} from "../src";
import * as fs from "fs";
import * as path from "path"
import {InstanceMethodCallTransactionRequestModel} from "../src";
import {CodeSignature} from "../src";

const HOTMOKA_VERSION = "1.0.1"

describe('Testing the marshalling of the JS objects', () => {

    it('writeShort(22) = rO0ABXcCABY=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeShort(22)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCABY=')
    })

    it('writeInt(32) = rO0ABXcEAAAAIA==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeInt(32)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEAAAAIA==')
    })

    it('writeLong(92) = rO0ABXcIAAAAAAAAAFw=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(92)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAAAAAAAAAFw=')
    })

    it('writeLong(1000129) = rO0ABXcIAAAAAAAPQsE=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(1000129)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAAAAAAAPQsE=')
    })

    it('writeLong(9007199254740991) = rO0ABXcIAB////////8=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(9007199254740991)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAB////////8=')
    })


    it('writeBigInteger(9007199254740991) = rO0ABXcJAgAf////////', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger('9007199254740991')
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcJAgAf////////')
    })

    it('writeBigInteger(9) = rO0ABXcBDQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger('9')
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBDQ==')
    })

    it('writeBigInteger(7654319) = rO0ABXcFAQB0y68=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger('7654319')
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcFAQB0y68=')
    })

    it('writeBigInteger(9007199254740991765896) = rO0ABXcMAwoB6Ef//////G2I', async () => {
        // TODO: implement big bigInteger
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger('9007199254740991765896')
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcYAxY5MDA3MTk5MjU0NzQwOTkxNzY1ODk2')
    })

    it('writeFloat(33.8) = rO0ABXcEQgczMw==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeFloat(33.8)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEQgczMw==')
    })

    it('writeBoolean(true) = rO0ABXcBAQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBoolean(true)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBAQ==')
    })

    it('writeChar("d") = rO0ABXcCAGQ=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeChar("d")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCAGQ=')
    })

    it('writeString("hello world") = rO0ABXcNAAtoZWxsbyB3b3JsZA==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeString("hello world")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcNAAtoZWxsbyB3b3JsZA==')
    })

    it('writeBuffer(Buffer.from("hello world")) = rO0ABXcLaGVsbG8gd29ybGQ=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBuffer(Buffer.from("hello world"))
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcLaGVsbG8gd29ybGQ=')
    })

    it('writeCompactInt(30006) = rO0ABXcF/wAAdTY=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeCompactInt(30006)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcF/wAAdTY=')
    })

    it('writeStringShared(Hotmoka") = rO0ABXcK/wAHSG90bW9rYQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeStringShared("Hotmoka")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcK/wAHSG90bW9rYQ==')
    })


    it('classType - writeFieldSignature(fieldSignature") = rO0ABXcM/xQAB2JhbGFuY2Ua', async () => {
        const fieldSignature = new FieldSignatureModel("balance", ClassType.BIG_INTEGER.name, ClassType.CONTRACT.name)
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeFieldSignature(fieldSignature)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcM/xQAB2JhbGFuY2Ua')
    })

    it('writeStorageReference(storageReference") = rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==', async () => {
        const storageReference = new StorageReferenceModel(
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "19992"
        )
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeStorageReference(storageReference)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==')
    })

    it('writeTransactionReference(transactionReference") = rO0ABXch/9DklkaMJfylkXmIX6fF/09EDvvQ4MlsJCa3mXM2YZiC', async () => {
        const transactionReference = new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882");
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeTransactionReference(transactionReference)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXch/9DklkaMJfylkXmIX6fF/09EDvvQ4MlsJCa3mXM2YZiC')
    })

    it('basicType - writeFieldSignature(fieldSignature") = rO0ABXcJ/yYABHNpemUE', async () => {
        const fieldSignature = new FieldSignatureModel("size", BasicType.INT.name, ClassType.STORAGE_TREE_INTMAP_NODE.name)
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeFieldSignature(fieldSignature)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcJ/yYABHNpemUE')
    })

    it('new StringValue("hello") = rO0ABXcICgAFaGVsbG8=', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("hello", ClassType.STRING.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcICgAFaGVsbG8=')
    })

    it('new IntValue("1993") = rO0ABXcFDgAAB8k=', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("1993", BasicType.INT.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcFDgAAB8k=')
    })

    it('new BooleanValue("true") = rO0ABXcBAA==', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("true", BasicType.BOOLEAN.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBAA==')
    })

    it('new ByteValue("32") = rO0ABXcCAiA=', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("32", BasicType.BYTE.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCAiA=')
    })

    it('new CharValue("32") = rO0ABXcDAwBE', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("D", BasicType.CHAR.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcDAwBE')
    })

    it('new ShortValue("44") = rO0ABXcDCQAs', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("44", BasicType.SHORT.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcDCQAs')
    })

    it('new LongValue("1238769181") = rO0ABXcJBwAAAABJ1h4d', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("1238769181", BasicType.LONG.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcJBwAAAABJ1h4d')
    })

    it('new DoubleValue("1238769181") = rO0ABXcJBEHSdYeHeZma', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("1238769181.9", BasicType.DOUBLE.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcJBEHSdYeHeZma')
    })

    it('new FloatValue("23.7") = rO0ABXcFBUG9mZo=', async () => {
        const marshallingContext = new MarshallingContext()
        const storageValue = StorageValueModel.newStorageValue("23.7", BasicType.FLOAT.name)
        StorageValueModel.into(marshallingContext, storageValue)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcFBUG9mZo=')
    })

    it('new ConstructorCallTransactionRequestModel(..) = rO0ABXdABAAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQALOwAAfQABQEGAAPnABMBGg==', async () => {
        const marshallingContext = new MarshallingContext()

        const constructorSignature = new ConstructorSignatureModel(
            ClassType.MANIFEST.name,
            [ClassType.BIG_INTEGER.name]
        )

        const constructorCall = new ConstructorCallTransactionRequestModel(
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
            "chaintest"
        )

        constructorCall.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXdABAAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQALOwAAfQABQEGAAPnABMBGg==')
    })

    it('new ConstructorSignatureModel(..) = rO0ABXcEABMBGg==', async () => {
        const marshallingContext = new MarshallingContext()

        const constructorSignature = new ConstructorSignatureModel(
            ClassType.MANIFEST.name,
            [ClassType.BIG_INTEGER.name]
        )

        constructorSignature.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEABMBGg==')
    })

    it('new StaticMethodCallTransactionRequestModel(..) NonVoidMethod gas station = rO0ABXdIBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQELAAEoARcAB2JhbGFuY2Ua', async () => {
        const marshallingContext = new MarshallingContext()

        const nonVoidMethodSignature = new NonVoidMethodSignatureModel(
            "balance",
            ClassType.GAS_STATION.name,
            [ClassType.STORAGE.name],
            ClassType.BIG_INTEGER.name
        )

        const staticMethodCall = new StaticMethodCallTransactionRequestModel(
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
            "chaintest"
        )

        staticMethodCall.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXdIBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQELAAEoARcAB2JhbGFuY2Ua')
    })

    it('new StaticMethodCallTransactionRequestModel(..) VoidMethod = rO0ABXdKBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQEOAAABLAIbAQQAB3JlY2VpdmU=', async () => {
        const marshallingContext = new MarshallingContext()

        const RECEIVE_INT = new VoidMethodSignatureModel(
            "receive",
            ClassType.PAYABLE_CONTRACT.name,
            [BasicType.INT.name]
        )

        const staticMethodCall = new StaticMethodCallTransactionRequestModel(
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
            "chaintest"
        )

        staticMethodCall.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXdKBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQEOAAABLAIbAQQAB3JlY2VpdmU=')
    })

    it('new StaticMethodCallTransactionRequestModel(..) NonVoidMethod = rO0ABXdDBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEgAABW5vbmNlGg==', async () => {
        const marshallingContext = new MarshallingContext()

        const staticMethodCall = new StaticMethodCallTransactionRequestModel(
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
            "chaintest"
        )

        staticMethodCall.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXdDBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEgAABW5vbmNlGg==')
    })

    it('new InstanceMethodCallTransactionRequestModel(..) VoidMethod = rO0ABXc8BwAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQAAAAEs', async () => {
        const marshallingContext = new MarshallingContext()

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
            "chaintest"
        )

        request.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXc8BwAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQAAAAEs')
    })

    it('new InstanceMethodCallTransactionRequestModel(..) NonVoidMethod = rO0ABXdIBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEwAACWdldEdhbWV0ZRIA', async () => {
        const marshallingContext = new MarshallingContext()

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
            "chaintest"
        )

        request.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXdIBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEwAACWdldEdhbWV0ZRIA')
    })


    it.skip('new JarStoreTransactionRequestModel(..)', async () => {
        const marshallingContext = new MarshallingContext()

        const jarStoreTransaction = new JarStoreTransactionRequestModel(
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
            "chaintest"
        )

        jarStoreTransaction.into(marshallingContext)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('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')
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


