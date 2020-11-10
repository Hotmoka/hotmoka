package io.takamaka.tests.storagemap;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.util.StorageMap;

/**
 * An exported storage map, so that it can be used for calling its methods in tests.
 */
@Exported
public class ExportedStorageMap<K,V> extends StorageMap<K,V> {
}