package io.hotmoka.network.service.get;

import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.model.State;
import io.hotmoka.network.model.update.ClassUpdate;
import io.hotmoka.network.model.update.FieldUpdate;
import io.hotmoka.network.model.update.Update;
import io.hotmoka.network.service.NetworkService;
import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NodeGetServiceImpl extends NetworkService implements NodeGetService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeGetServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    public ResponseEntity<Object> getTakamakaCode() {

        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

            return responseOf(node.getTakamakaCode());

        }catch (Exception e) {
            LOGGER.error("getTakamakaCode", e);
            return exceptionResponseOf(e);
        }
    }

    @Override
    public ResponseEntity<Object> getManifest() {
        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

            return responseOf(node.getManifest());

        } catch (Exception e) {
            LOGGER.error("getManifest", e);
            return exceptionResponseOf(e);
        }
    }

    @Override
    public ResponseEntity<Object> getState() {
        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

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

        } catch (Exception e) {
            LOGGER.error("getState", e);
            return exceptionResponseOf(e);
        }
    }


    /**
     * Build a json update model from an update item {@link io.hotmoka.beans.updates.Update} of a node instance {@link io.hotmoka.nodes.Node}
     * @param updateItem the update from which to build a json model
     * @return a json model of an update instance {@link io.hotmoka.beans.updates.Update}  of a node {@link io.hotmoka.nodes.Node}
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
