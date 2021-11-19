package io.hotmoka.tests;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.*;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Marshallable  {

    @Test
    @DisplayName("writeShort(22) = rO0ABXcCABY=")
    public void testShort() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeShort(22);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCABY=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeInt(32) = rO0ABXcCABY=")
    public void testInt() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeInt(32);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcEAAAAIA==", toBase64(bytes));
    }


    @Test
    @DisplayName("writeLong(92) = rO0ABXcIAAAAAAAAAFw=")
    public void testLong() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeLong(92);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIAAAAAAAAAFw=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeLong(1000129) = rO0ABXcIAAAAAAAPQsE=")
    public void testLong2() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeLong(1000129);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIAAAAAAAPQsE=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeLong(9007199254740991) = rO0ABXcIAB////////8=")
    public void testLong3() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeLong(9007199254740991L);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIAB////////8=", toBase64(bytes));
    }


    @Test
    @DisplayName("writeBigInteger(9007199254740991L) = rO0ABXcJAgAf////////")
    public void testBigInt() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeBigInteger(BigInteger.valueOf(9007199254740991L));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJAgAf////////", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(9) = rO0ABXcBDQ==")
    public void testBigInt2() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeBigInteger(BigInteger.valueOf(9));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBDQ==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(7654319) = rO0ABXcFAQB0y68=")
    public void testBigInt3() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeBigInteger(BigInteger.valueOf(7654319));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFAQB0y68=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBigInteger(9007199254740991765896) = rO0ABXcMAwoB6Ef//////G2I")
    public void testBigInt4() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeBigInteger(new BigInteger("9007199254740991765896"));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcYAxY5MDA3MTk5MjU0NzQwOTkxNzY1ODk2", toBase64(bytes));
    }

    @Test
    @DisplayName("writeDouble(33.8) = rO0ABXcIQEDmZmZmZmY=")
    public void testDouble() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeDouble(33.8);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcIQEDmZmZmZmY=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeFloat(33.8f) = rO0ABXcEQgczMw==")
    public void testFloat() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeFloat(33.8f);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcEQgczMw==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeBoolean(true) = rO0ABXcBAQ==")
    public void testBoolean() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeBoolean(true);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBAQ==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeChar('d') = rO0ABXcCAGQ=")
    public void testChar() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeChar('d');
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCAGQ=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeUTF(hello world) = rO0ABXcNAAtoZWxsbyB3b3JsZA==")
    public void testString() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeUTF("hello world");
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcNAAtoZWxsbyB3b3JsZA==", toBase64(bytes));
    }

    @Test
    @DisplayName("write(hello world.getBytes()) = rO0ABXcLaGVsbG8gd29ybGQ=")
    public void testBytes() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.write("hello world".getBytes());
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcLaGVsbG8gd29ybGQ=", toBase64(bytes));
    }


    @Test
    @DisplayName("writeCompactInt(30006) = rO0ABXcF/wAAdTY=")
    public void testCompactInt() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeCompactInt(30006);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcF/wAAdTY=", toBase64(bytes));
    }

    @Test
    @DisplayName("writeStringShared(Hotmoka) = rO0ABXcK/wAHSG90bW9rYQ==")
    public void testStringShared() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeStringShared("Hotmoka");
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcK/wAHSG90bW9rYQ==", toBase64(bytes));
    }

    @Test
    @DisplayName("writeFieldSignature(fieldSignature) = rO0ABXcM/xQAB2JhbGFuY2Ua")
    public void testFieldSignature() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            FieldSignature fieldSignature = new FieldSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

            context.writeFieldSignature(fieldSignature);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcM/xQAB2JhbGFuY2Ua", toBase64(bytes));
    }

    @Test
    @DisplayName("writeStorageReference(storageReference) = rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==")
    public void testStorageReference() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageReference storageReference = new StorageReference(
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    new BigInteger("19992")
            );

            context.writeStorageReference(storageReference);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==", toBase64(bytes));
    }

    @Test
    @DisplayName("testWriteTransactionReference(transactionReference) = rO0ABXch/9DklkaMJfylkXmIX6fF/09EDvvQ4MlsJCa3mXM2YZiC")
    public void testTransactionReference() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            TransactionReference transactionReference = new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882");
            context.writeTransactionReference(transactionReference);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXch/9DklkaMJfylkXmIX6fF/09EDvvQ4MlsJCa3mXM2YZiC", toBase64(bytes));
    }

    @Test
    @DisplayName("writeFieldSignature(fieldSignature) = rO0ABXcJ/yYABHNpemUE")
    public void testFieldSignatureBasicType() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            FieldSignature fieldSignature = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "size", BasicTypes.INT);

            context.writeFieldSignature(fieldSignature);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJ/yYABHNpemUE", toBase64(bytes));
    }

    @Test
    @DisplayName("new StringValue(\"hello\") = rO0ABXcICgAFaGVsbG8=")
    public void testStringValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new StringValue("hello");
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcICgAFaGVsbG8=", toBase64(bytes));
    }

    @Test
    @DisplayName("new IntValue(1993) = rO0ABXcFDgAAB8k=")
    public void testIntValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new IntValue(1993);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFDgAAB8k=", toBase64(bytes));
    }

    @Test
    @DisplayName("new BooleanValue(true) = rO0ABXcBAA==")
    public void testBooleanValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new BooleanValue(true);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBAA==", toBase64(bytes));
    }

    @Test
    @DisplayName("new ByteValue(32) = rO0ABXcCAiA=")
    public void testByteValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new ByteValue((byte) 32);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCAiA=", toBase64(bytes));
    }

    @Test
    @DisplayName("new CharValue('D') = rO0ABXcDAwBE")
    public void testCharValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new CharValue('D');
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDAwBE", toBase64(bytes));
    }

    @Test
    @DisplayName("new ShortValue(44) = rO0ABXcDCQAs")
    public void testShortValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new ShortValue((short) 44);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDCQAs", toBase64(bytes));
    }

    @Test
    @DisplayName("new LongValue(1238769181L) = rO0ABXcJBwAAAABJ1h4d")
    public void testLongValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new LongValue(1238769181L);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJBwAAAABJ1h4d", toBase64(bytes));
    }

    @Test
    @DisplayName("new DoubleValue(1238769181.9) = rO0ABXcJBEHSdYeHeZma")
    public void testDoubleValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new DoubleValue(1238769181.9);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJBEHSdYeHeZma", toBase64(bytes));
    }

    @Test
    @DisplayName("new FloatValue(23.7f) = rO0ABXcFBUG9mZo=")
    public void testFloatValue() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StorageValue storageValue = new FloatValue(23.7f);
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFBUG9mZo=", toBase64(bytes));
    }

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..) = rO0ABXdABAAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQALOwAAfQABQEGAAPnABMBGg==")
    public void testConstructorCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            ConstructorSignature constructorSignature = new ConstructorSignature(
                    ClassType.MANIFEST,
                    ClassType.BIG_INTEGER
            );

            ConstructorCallTransactionRequest constructorCall = new ConstructorCallTransactionRequest(
                   "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(11500),
                    BigInteger.valueOf(500),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    constructorSignature,
                    new BigIntegerValue(BigInteger.valueOf(999))
            );

            constructorCall.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdABAAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQALOwAAfQABQEGAAPnABMBGg==", toBase64(bytes));
    }


    @Test
    @DisplayName("new ConstructorSignature(..) = rO0ABXcEABQBGg==")
    public void testConstructorSignature() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            ConstructorSignature constructorSignature = new ConstructorSignature(
                    ClassType.MANIFEST,
                    ClassType.BIG_INTEGER
            );

            constructorSignature.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcEABMBGg==", toBase64(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) = rO0ABXdIBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQELAAEoARcAB2JhbGFuY2Ua")
    public void testStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            NonVoidMethodSignature nonVoidMethodSignature = new NonVoidMethodSignature(
                    ClassType.GAS_STATION,
                    "balance",
                    ClassType.BIG_INTEGER,
                    ClassType.STORAGE
            );

            StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = new StaticMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    nonVoidMethodSignature,
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
            );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdIBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQELAAEoARcAB2JhbGFuY2Ua", toBase64(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) VoidMethod = rO0ABXdKBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQEOAAABLAIbAQQAB3JlY2VpdmU=")
    public void testVoidStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            MethodSignature receiveInt = new VoidMethodSignature(
                    ClassType.PAYABLE_CONTRACT,
                    "receive",
                    BasicTypes.INT
            );

            StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = new StaticMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    receiveInt,
                    new IntValue(300)
                );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdKBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQEOAAABLAIbAQQAB3JlY2VpdmU=", toBase64(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) NonVoidMethod = rO0ABXdDBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEgAABW5vbmNlGg==")
    public void testNonVoidStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = new StaticMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    CodeSignature.NONCE
            );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdDBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEgAABW5vbmNlGg==", toBase64(bytes));
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) VoidMethod = rO0ABXc8BwAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQAAAAEs")
    public void testVoidInstanceMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            MethodSignature receiveInt = new VoidMethodSignature(
                    ClassType.PAYABLE_CONTRACT,
                    "receive",
                    BasicTypes.INT
            );

            InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    receiveInt,
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    new IntValue(300)
            );

            request.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXc8BwAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQAAAAEs", toBase64(bytes));
    }

    @Test
    @DisplayName("new InstanceMethodCallTransactionRequest(..) NonVoidMethod = rO0ABXdIBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEwAACWdldEdhbWV0ZRIA")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    CodeSignature.GET_GAMETE,
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
            );

            request.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdRBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQABEwAACWdldEdhbWV0ZQr/AAZHYW1ldGUA", toBase64(bytes));
    }

    @Test
    @Disabled
    @DisplayName("new JarStoreTransactionRequest(..)")
    public void testJarStoreTransactionRequest() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            JarStoreTransactionRequest jarStoreTransactionRequest = new JarStoreTransactionRequest(
                    "".getBytes(),
                    new StorageReference(new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    new LocalTransactionReference("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    bytesOf("lambdas.jar")
            );

            jarStoreTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("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"
                , toBase64(bytes));
    }


    protected static String toBase64(byte[] bytes) {
       return new String(Base64.getEncoder().encode(bytes));
    }

    public static byte[] bytesOf(String fileName) throws IOException {
        return Files.readAllBytes(pathOfExample(fileName));
    }

    public static Path pathOfExample(String fileName) {
        return Paths.get("../io-hotmoka-examples/target/io-hotmoka-examples-" + "1.0.1" + '-' + fileName);
    }
}
