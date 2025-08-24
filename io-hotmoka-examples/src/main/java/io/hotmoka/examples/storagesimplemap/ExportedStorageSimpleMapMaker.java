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

package io.hotmoka.examples.storagesimplemap;

import io.takamaka.code.util.StorageSimpleMap;
import io.takamaka.code.util.StorageSimpleTreeMap;

/**
 * A static method for creating an exported storage map, so that it can be used for calling its methods in tests.
 */
public class ExportedStorageSimpleMapMaker {

	public static <K,V> StorageSimpleMap<K,V> mkEmptyExportedStorageSimpleMap() {
		return new ExportedModifiableStorageSimpleMap<>(new StorageSimpleTreeMap<>());
	}
}