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

package io.hotmoka.node.api;

import io.hotmoka.node.api.values.StorageReference;

/**
 * The information to control an account of a Hotmoka node.
 * It contains the entropy from which the key pair can be reconstructed and
 * the storage reference of the account in the store of the node.
 */
public interface Account extends io.hotmoka.crypto.api.Account<StorageReference> {
}