/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.local.api;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.api.Node;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 */
@ThreadSafe
public interface LocalNode<C extends LocalNodeConfig<?,?>> extends Node {
	C getLocalConfig();
	GasCostModel getGasCostModel();
	<T> Future<T> submit(Callable<T> task);
}