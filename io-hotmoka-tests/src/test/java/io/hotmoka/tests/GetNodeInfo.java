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

package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.constants.Constants;

/**
 * A test for {@link io.hotmoka.node.api.Node#getNodeInfo()}.
 */
class GetNodeInfo extends HotmokaTest {

	@Test @DisplayName("getInfoworks")
	void getNodeInfo() throws Exception {
		assertEquals(Constants.HOTMOKA_VERSION, node.getInfo().getVersion());
	}
}