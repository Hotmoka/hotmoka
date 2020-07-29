package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.network.models.updates.UpdateModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Immutable
public class JarStoreTransactionSuccessfulResponseModel extends JarStoreTransactionResponseModel {

    /**
     * The jar to install, instrumented.
     */
    public final String instrumentedJar;

    /**
     * The dependencies of the jar, previously installed in blockchain.
     * This is a copy of the same information contained in the request.
     */
    public final List<TransactionReferenceModel> dependencies;

    public JarStoreTransactionSuccessfulResponseModel(JarStoreTransactionSuccessfulResponse response) {
        super(response);

        this.instrumentedJar = Base64.getEncoder().encodeToString(response.getInstrumentedJar());
        this.dependencies = response.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
    }

    public JarStoreTransactionSuccessfulResponse toBean() {
        return new JarStoreTransactionSuccessfulResponse(
                Base64.getDecoder().decode(this.instrumentedJar),
                dependencies.stream().map(TransactionReferenceModel::toBean).collect(Collectors.toList()).stream(),
                updates.stream().map(UpdateModel::toBean).collect(Collectors.toSet()).stream(),
                gasConsumedForCPU,
                gasConsumedForRAM,
                gasConsumedForStorage
        );
    }
}
