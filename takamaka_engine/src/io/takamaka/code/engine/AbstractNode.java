package io.takamaka.code.engine;

import java.util.Optional;

import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.Node;

/**
 * A generic implementation of a HotMoka node. Specific implementations can subclass this class
 * and just implement the remaining missing abstract template methods.
 */
public abstract class AbstractNode implements Node {

	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	@Override
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}

	@Override
	public final String getClassNameOf(StorageReference object) {
		try {
			TransactionResponse response = getResponseAt(object.transaction);
			if (response instanceof TransactionResponseWithUpdates) {
				Optional<ClassTag> classTag = ((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst();

				if (classTag.isPresent())
					return classTag.get().className;
			}
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}

		throw new DeserializationError("no class tag found for " + object);
	}
}