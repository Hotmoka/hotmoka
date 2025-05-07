/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.json;

import java.net.URI;

import io.hotmoka.moka.api.NodeResumeOutput;

/**
 * The JSON representation of the output of a command that resumes a Hotmoka node.
 */
public abstract class NodeResumeOutputJson {
	private final URI uri;

	protected NodeResumeOutputJson(NodeResumeOutput output) {
		this.uri = output.getURI();
	}

	public URI getURI() {
		return uri;
	}
}