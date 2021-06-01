package io.hotmoka.tests;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;

public class Marshallable {


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
    public void testBigIntShort() throws IOException {
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
    public void testBigIntInt() throws IOException {
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
    public void testBigIntBig() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeBigInteger(new BigInteger("9007199254740991765896"));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcMAwoB6Ef//////G2I", toBase64(bytes));
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
    public void testWriteStringShared() throws IOException {
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
    public void testWriteFieldSignatureClassType() throws IOException {
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
    public void testWriteStorageReference() throws IOException {
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
    public void testWriteTransactionReference() throws IOException {
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
    public void testWriteFieldSignatureBasicType() throws IOException {
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
    public void testStaticMethodCallTransaction() throws IOException {
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

    private static String toBase64(byte[] bytes) {
       return new String(Base64.getEncoder().encode(bytes));
    }
}
