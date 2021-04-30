/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.network.updates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.updates.Update;

/**
 * The model of the state of an object: just the set of its updates.
 */
public class StateModel {
    public List<UpdateModel> updates;

    /**
     * Builds the model of the given state of an object.
     * 
     * @param state the state
     */
    public StateModel(Stream<Update> state) {
    	this.updates = state.map(UpdateModel::new).collect(Collectors.toList());
    }

    public StateModel() {}

    /**
     * Yields the updates having this model.
     * 
     * @return the updates
     */
    public Stream<Update> toBean() {
    	return updates.stream().map(UpdateModel::toBean);
    }
}