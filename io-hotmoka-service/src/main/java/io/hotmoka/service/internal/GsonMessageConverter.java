/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.service.internal;

import java.nio.charset.StandardCharsets;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.MimeType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A websocket message converter implementation class using the GSON library.
 */
public class GsonMessageConverter extends AbstractMessageConverter {
    private final Gson gson;

    public GsonMessageConverter() {
        super(new MimeType("application", "json"));
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected boolean canConvertTo(Object payload, MessageHeaders headers) {
        return true;
    }

    @Override
    protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
        return true;
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
        try {
            String json = new String((byte[]) message.getPayload());
            if (json.equals("null"))
                return NullObject.INSTANCE;
            else
            	return gson.fromJson(json, targetClass);
        }
        catch (RuntimeException e) {
            String exceptionMessage = e.getMessage() != null ? ": " + e.getMessage() : "";
            throw new RuntimeException("Error deserializing json message" + exceptionMessage);
        }
    }

    @Override
    protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
        try {
            if (payload == null)
                return "null".getBytes();
            else
            	return gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        }
        catch (RuntimeException e) {
            String exceptionMessage = e.getMessage() != null ? ": " + e.getMessage() : "";
            throw new RuntimeException("Error serializing Java object: " + exceptionMessage);
        }
    }

    /**
     * Special object to wrap a {@code null} reference
     */
    public static class NullObject {
    	public final static NullObject INSTANCE = new NullObject();
    	private NullObject() {}
    }
}