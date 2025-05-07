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

package io.hotmoka.moka.api;

import java.net.URI;

import io.hotmoka.annotations.Immutable;

/**
 * The output of a command that resumes an already initialized Hotmoka node.
 * The difference with the initialization or start of a node is that
 * the resume just continues executing the node from its current store,
 * as saved on disk.
 */
@Immutable
public interface NodeResumeOutput {

	/**
	 * Yields the URI of the published node service.
	 * 
	 * @return the URI of the published node service
	 */
	URI getURI();
}