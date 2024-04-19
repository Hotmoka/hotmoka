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

package io.hotmoka.node.api;

import java.util.Optional;

import io.hotmoka.node.api.values.StorageValue;

/**
 * The future of a transaction that executes a method in a node.
 * It yields an optional value, since void method return non value.
 */
public interface MethodFuture extends CodeFuture<Optional<StorageValue>> {
}