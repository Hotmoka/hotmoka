package io.hotmoka.network.internal.services;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.internal.models.updates.ClassTagModel;
import io.hotmoka.network.internal.models.updates.ClassUpdateModel;
import io.hotmoka.network.internal.models.updates.FieldUpdateModel;
import io.hotmoka.network.internal.models.updates.StateModel;
import io.hotmoka.network.internal.models.updates.UpdateModel;
import io.hotmoka.network.internal.models.values.StorageReferenceModel;
import io.hotmoka.network.internal.models.values.TransactionReferenceModel;
import io.hotmoka.nodes.Node;

@Service
public class GetServiceImpl extends AbstractService implements GetService {

    @Override
    public TransactionReferenceModel getTakamakaCode() {
        return wrapExceptions(() -> new TransactionReferenceModel(getNode().getTakamakaCode()));
    }

    @Override
    public StorageReferenceModel getManifest() {
        return wrapExceptions(() -> new StorageReferenceModel(getNode().getManifest()));
    }

    @Override
    public StateModel getState(StorageReferenceModel request) {
        return wrapExceptions(() -> {
            Node node = getNode();
            StorageReference storageReference = request.toBean();
            List<UpdateModel> updatesJson = node.getState(storageReference)
                    .map(GetServiceImpl::buildUpdateModel)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            StateModel stateJson = new StateModel();
            stateJson.setTransaction(storageReference.transaction.getHash());
            stateJson.setProgressive(storageReference.progressive);
            stateJson.setUpdates(updatesJson);

            return stateJson;
        });
    }

    @Override
    public ClassTagModel getClassTag(StorageReferenceModel request) {
        return wrapExceptions(() -> new ClassTagModel(getNode().getClassTag(request.toBean())));
    }

    /**
     * Build a json update model from an update item {@link io.hotmoka.beans.updates.Update} of a {@link io.hotmoka.nodes.Node} instance
     * @param updateItem the update from which to build a json model
     * @return a json model of an update instance {@link io.hotmoka.beans.updates.Update}  of a {@link io.hotmoka.nodes.Node}
     */
    private static UpdateModel buildUpdateModel(io.hotmoka.beans.updates.Update updateItem) {
        UpdateModel updateJson = null;

        if (updateItem instanceof UpdateOfField) {
            updateJson = new FieldUpdateModel();
            ((FieldUpdateModel) updateJson).setUpdateType(updateItem.getClass().getName());
            ((FieldUpdateModel) updateJson).setValue(((UpdateOfField) updateItem).getValue().toString());
            ((FieldUpdateModel) updateJson).setDefiningClass(((UpdateOfField) updateItem).getField().definingClass.name);
            ((FieldUpdateModel) updateJson).setType(((UpdateOfField) updateItem).getField().type.toString());
            ((FieldUpdateModel) updateJson).setName(((UpdateOfField) updateItem).getField().name);
        }

        if (updateItem instanceof io.hotmoka.beans.updates.ClassTag) {
            updateJson = new ClassUpdateModel();
            ((ClassUpdateModel) updateJson).setClassName(((io.hotmoka.beans.updates.ClassTag) updateItem).className);
            ((ClassUpdateModel) updateJson).setJar(((io.hotmoka.beans.updates.ClassTag) updateItem).jar.getHash());
        }

        return updateJson;
    }
}