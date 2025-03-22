/*
Copyright 2024 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.testing.AbstractLoggedTests;

public class TransactionResponseTests extends AbstractLoggedTests {
	private final static TransactionReference classpath = TransactionReferences.of("cafebabe01234567cafebabe01234567cafebabe01234567cafebabe01234567");
	private final static TransactionReference reference = TransactionReferences.of("01234567cafebabe01234567cafebabe01234567cafebabe01234567cafebabe");
	private final static TransactionReference reference2 = TransactionReferences.of("a1234567cafebabe01234567cafebabe01234567cafebabe01234567cafebabe");
	private final static TransactionReference reference3 = TransactionReferences.of("b1234567cafebabe01234567cafebabe01234567cafebabe01234567cafebabe");
	private final static StorageReference gamete = StorageValues.reference(reference, BigInteger.TWO);
	private final static StorageReference result = StorageValues.reference(reference, BigInteger.valueOf(17));
	private final static Update update1 = Updates.classTag(gamete, StorageTypes.classNamed("io.hotmoka.Gamete"), classpath);
	private final static Update update2 = Updates.ofBigInteger(gamete, FieldSignatures.of(StorageTypes.classNamed("io.hotmoka.Gamete"), "f", StorageTypes.BIG_INTEGER), BigInteger.valueOf(1234));
	private final static Update update3 = Updates.ofInt(gamete, FieldSignatures.of(StorageTypes.classNamed("io.hotmoka.Gamete"), "i", StorageTypes.INT), 42);
	private final static StorageReference event1 = StorageValues.reference(reference2, BigInteger.TWO);
	private final static StorageReference event2 = StorageValues.reference(reference3, BigInteger.TEN);
	private final static StorageReference event3 = StorageValues.reference(reference3, BigInteger.ONE);
	private final static byte[] instrumentedJar = "Imagine this to be a very beautiful jar file".getBytes();
	private final static BigInteger gasConsumedForCPU = BigInteger.valueOf(1317);
	private final static BigInteger gasConsumedForRAM = BigInteger.valueOf(1987);
	private final static BigInteger gasConsumedForStorage = BigInteger.valueOf(200);
	private final static BigInteger gasConsumedForPenalty = BigInteger.valueOf(4200);
	private final static String classNameOfCause = "io.hotmoka.Exception";
	private final static String messageName = "bad exception!";
	private final static String where = "somewhere in your code";

	@Test
	@DisplayName("gamete creation transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGameteCreationTransactionResponse() throws Exception {
		var response1 = TransactionResponses.gameteCreation(Stream.of(update1, update2, update3), gamete);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("initialization transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForInitializationTransactionResponse() throws Exception {
		var response1 = TransactionResponses.initialization();
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("jar store initial transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForJarStoreInitialTransactionResponse() throws Exception {
		var response1 = TransactionResponses.jarStoreInitial(instrumentedJar, Stream.of(reference, reference2, reference3), 13);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("jar store successful transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForJarStoreTransactionSuccessfulResponse() throws Exception {
		var response1 = TransactionResponses.jarStoreSuccessful(instrumentedJar, Stream.of(reference, reference2, reference3), 12, Stream.of(update1, update2, update3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("jar store failed transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForJarStoreTransactionFailedResponse() throws Exception {
		var response1 = TransactionResponses.jarStoreFailed(Stream.of(update1, update2, update3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty, classNameOfCause, messageName);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("constructor call failed transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForConstructorCallTransactionFailedResponse() throws Exception {
		var response1 = TransactionResponses.constructorCallFailed(Stream.of(update1, update2, update3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty, classNameOfCause, messageName, where);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("constructor call exception transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForConstructorCallTransactionExceptionResponse() throws Exception {
		var response1 = TransactionResponses.constructorCallException(Stream.of(update1, update2, update3), Stream.of(event1, event2, event3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, classNameOfCause, messageName, where);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("constructor call successful transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForConstructorCallTransactionSuccessfulResponse() throws Exception {
		var response1 = TransactionResponses.constructorCallSuccessful(result, Stream.of(update1, update2, update3), Stream.of(event1, event2, event3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("method call exception transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForMethodCallTransactionExceptionResponse() throws Exception {
		var response1 = TransactionResponses.methodCallException(Stream.of(update1, update2, update3), Stream.of(event1, event2, event3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, classNameOfCause, messageName, where);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("method call failed transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForMethodCallTransactionFailedResponse() throws Exception {
		var response1 = TransactionResponses.methodCallFailed(Stream.of(update1, update2, update3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty, classNameOfCause, messageName, where);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("method call successful transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForMethodCallTransactionSuccessfulResponse() throws Exception {
		var response1 = TransactionResponses.nonVoidMethodCallSuccessful(result, Stream.of(update1, update2, update3), Stream.of(event1, event2, event3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("void method call successful transaction responses are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForVoidMethodCallTransactionSuccessfulResponse() throws Exception {
		var response1 = TransactionResponses.voidMethodCallSuccessful(Stream.of(update1, update2, update3), Stream.of(event1, event2, event3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		String encoded = new TransactionResponses.Encoder().encode(response1);
		var response2 = new TransactionResponses.Decoder().decode(encoded);
		assertEquals(response1, response2);
	}

	@Test
	@DisplayName("void method call successful transaction responses are correctly marshalled and unmarshalled")
	public void marshalUnmarshalWorksForVoidMethodCallSuccessfulResponse() throws Exception {
		var response1 = TransactionResponses.voidMethodCallSuccessful(Stream.of(update1, update2, update3), Stream.of(event1, event2, event3), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		byte[] bytes;

		try (var baos = new ByteArrayOutputStream(); var context = NodeMarshallingContexts.of(baos)) {
			response1.into(context);
			context.flush();
			bytes = baos.toByteArray();
		}

		TransactionResponse response2;
		try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))) {
			response2 = TransactionResponses.from(context);
		}

		assertEquals(response1, response2);
	}
}