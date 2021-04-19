package io.hotmoka.remote.internal.websockets.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.network.errors.ErrorModel;

/**
 * Abstract class to handle the result published by a topic.
 * @param <T> the result type
 */
public abstract class ResultHandler<T> {
    private final Gson gson;
    private final Class<T> resultTypeClass;

    public ResultHandler(Class<T> resultTypeClass) {
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
            throw new InternalFailureException("Error deserializing model of type " + resultTypeClass.getName());
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
