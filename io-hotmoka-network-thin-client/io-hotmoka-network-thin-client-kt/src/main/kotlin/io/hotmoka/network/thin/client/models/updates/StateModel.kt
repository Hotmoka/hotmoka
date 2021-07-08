package io.hotmoka.network.thin.client.models.updates

/**
 * The model of the state of an object: just the set of its updates.
 */
class StateModel(
        /**
         * The updates of this state model.
         */
        val updates: List<UpdateModel>
)