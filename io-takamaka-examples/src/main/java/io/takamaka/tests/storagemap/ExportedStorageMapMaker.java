package io.takamaka.tests.storagemap;

import io.takamaka.code.util.ModifiableStorageMap;

/**
 * A static method for creating an exported storage map, so that it can be used for calling its methods in tests.
 */
public class ExportedStorageMapMaker {

	public static <K,V> ModifiableStorageMap<K,V> mkEmptyExportedStorageMap() {
		return ModifiableStorageMap.viewOf(ModifiableStorageMap.empty());
	}
}