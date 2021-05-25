package io.hotmoka.tests;

import io.hotmoka.beans.MarshallingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Marshallable {


    @Test
    @DisplayName("marshalling short")
    public void testShort() throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             MarshallingContext context = new MarshallingContext(baos)) {

            context.writeShort(22);
            context.flush();
            bytes = baos.toByteArray();
        }

        System.out.println(toBase64(bytes));
    }

    private static String toBase64(byte[] bytes) {
       return new String(Base64.getEncoder().encode(bytes));
    }
}
