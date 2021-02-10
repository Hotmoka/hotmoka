package io.hotmoka.examples.storagemap;

import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * A static method for creating an exported storage map, so that it can be used for calling its methods in tests.
 */
public class ExportedStorageMapMaker {

	public static <K,V> StorageMap<K,V> mkEmptyExportedStorageMap() {
		return new ExportedModifiableStorageMap<>(new StorageTreeMap<>());
	}
}