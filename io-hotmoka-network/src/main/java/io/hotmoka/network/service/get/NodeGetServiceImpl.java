package io.hotmoka.network.service.get;

import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.model.State;
import io.hotmoka.network.model.update.ClassUpdate;
import io.hotmoka.network.model.update.FieldUpdate;
import io.hotmoka.network.model.update.Update;
import io.hotmoka.network.service.NetworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NodeGetServiceImpl extends NetworkService implements NodeGetService {

    @Override
    public ResponseEntity<Object> getTakamakaCode() {
        return this.map(node -> responseOf(node.getTakamakaCode()));
    }

    @Override
    public ResponseEntity<Object> getManifest() {
        return this.map(node -> responseOf(node.getManifest()));
    }

    @Override
    public ResponseEntity<Object> getState() {

        return this.map(node -> {
            StorageReference manifest = node.getManifest();
            List<Update> updatesJson = node.getState(manifest)
                    .map(NodeGetServiceImpl::buildUpdateModel)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            State stateJson = new State();
            stateJson.setTransaction(manifest.transaction.getHash());
            stateJson.setProgressive(manifest.progressive);
            stateJson.setUpdates(updatesJson);

            return responseOf(stateJson);
        });
    }

    @Override
    public ResponseEntity<Object> getClassTag() {
        return this.map(node -> responseOf(node.getClassTag(node.getManifest())));
    }


    /**
     * Build a json update model from an update item {@link io.hotmoka.beans.updates.Update} of a {@link io.hotmoka.nodes.Node} instance
     * @param updateItem the update from which to build a json model
     * @return a json model of an update instance {@link io.hotmoka.beans.updates.Update}  of a {@link io.hotmoka.nodes.Node}
     */
    private static Update buildUpdateModel(io.hotmoka.beans.updates.Update updateItem) {
        Update updateJson = null;

        if (updateItem instanceof UpdateOfField) {
            updateJson = new FieldUpdate();
            ((FieldUpdate) updateJson).setUpdateType(updateItem.getClass().getName());
            ((FieldUpdate) updateJson).setValue(((UpdateOfField) updateItem).getValue().toString());
            ((FieldUpdate) updateJson).setDefiningClass(((UpdateOfField) updateItem).getField().definingClass.name);
            ((FieldUpdate) updateJson).setType(((UpdateOfField) updateItem).getField().type.toString());
            ((FieldUpdate) updateJson).setName(((UpdateOfField) updateItem).getField().name);
        }

        if (updateItem instanceof ClassTag) {
            updateJson = new ClassUpdate();
            ((ClassUpdate) updateJson).setClassName(((ClassTag) updateItem).className);
            ((ClassUpdate) updateJson).setJar(((ClassTag) updateItem).jar.getHash());
        }

        return updateJson;
    }
}
