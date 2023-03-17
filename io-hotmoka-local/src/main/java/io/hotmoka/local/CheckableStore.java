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

package io.hotmoka.local;

/**
 * A store that can be checked out, that is, its view of the world can be moved
 * back in time. Different moments of the store are identifies by hashes, that
 * can be checked out when needed.
 */
public interface CheckableStore extends Store {

	/**
	 * Resets the store to the view of the world expressed by the given hash.
	 * This assumes that no commit after the one that created the given hash has
	 * been garbage-collected, since otherwise some data might be missing for the given hash,
	 * which might result in missing objects.
	 * 
	 * @param hash the hash to reset to
	 */
	void checkout(byte[] hash);
}