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

package io.hotmoka.remote.internal.websockets.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.hotmoka.network.errors.ErrorModel;

/**
 * Abstract class to handle the result published by a topic.
 * @param <T> the result type
 */
abstract class ResultHandler<T> {
    private final Gson gson;
    private final Class<T> resultTypeClass;

    ResultHandler(Class<T> resultTypeClass) {
        this.gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
        this.resultTypeClass = resultTypeClass;
    }

    public Class<T> getResultTypeClass() {
        return resultTypeClass;
    }

    /**
     * Yields the model <T> of the STOMP message from the json representation.
     *
     * @param payload the json
     * @return the model <T>
     */
    protected T toModel(String payload) {
        try {
            return gson.fromJson(payload, resultTypeClass);
        }
        catch (Exception e) {
            throw new RuntimeException("Error while deserializing model of type " + resultTypeClass.getName());
        }
    }

    /**
     * It delivers the result of a parsed STOMP message response.
     * @param result the result as JSON.
     */
    public abstract void deliverResult(String result);

    /**
     * It delivers an {@link ErrorModel} which wraps an error.
     * @param errorModel the error model
     */
    public abstract void deliverError(ErrorModel errorModel);

    /**
     * Special method to deliver a NOP.
     */
    public abstract void deliverNothing();
}
