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

package io.hotmoka.node.service.api;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.Node;

/**
 * A network service that exposes a REST API to a Hotmoka node.
 */
@ThreadSafe
public interface NodeService extends AutoCloseable {

	/**
	 * The network endpoint path where {@link Node#getNodeInfo()} is published.
	 */
	String GET_NODE_INFO_ENDPOINT = "/get_node_info";

	/**
	 * The network endpoint path where {@link Node#getTakamakaCode()} is published.
	 */
	String GET_TAKAMAKA_CODE_ENDPOINT = "/get_takamaka_code";

	/**
	 * The network endpoint path where {@link Node#getManifest()} is published.
	 */
	String GET_MANIFEST_ENDPOINT = "/get_manifest";

	/**
	 * The network endpoint path where {@link Node#getClassTag(StorageReference)} is published.
	 */
	String GET_CLASS_TAG_ENDPOINT = "/get_class_tag";

	/**
	 * Stops the service and releases its resources.
	 */
	@Override
	void close();
}