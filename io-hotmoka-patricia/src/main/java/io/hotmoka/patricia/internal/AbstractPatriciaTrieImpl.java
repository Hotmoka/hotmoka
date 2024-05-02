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

package io.hotmoka.patricia.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Optional;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.patricia.FromBytes;
import io.hotmoka.patricia.ToBytes;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.KeyValueStoreException;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * Implementation of a Merkle-Patricia trie.
 *
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 */
public abstract class AbstractPatriciaTrieImpl<Key, Value, T extends PatriciaTrie<Key, Value, T>> implements PatriciaTrie<Key, Value, T> {

	/**
	 * The store that supports this trie.
	 */
	private final KeyValueStore store;

	/**
	 * The hasher for the keys of the trie.
	 */
	private final Hasher<? super Key> hasherForKeys;

	/**
	 * The hasher for the nodes of the trie.
	 */
	private final Hasher<AbstractNode> hasherForNodes;

	/**
	 * A function that marshals values into their byte representation.
	 */
	private final ToBytes<? super Value> valueToBytes;

	/**
	 * A function that unmarshals bytes into the represented value.
	 */
	private final FromBytes<? extends Value> bytesToValue;

	/**
	 * The current number of commits already executed on the store; this trie
	 * will record which data must be garbage collected (eventually)
	 * as result of the store updates performed during that commit. This might
	 * be -1L if the trie is only used for reading.
	 */
	private final long numberOfCommits;

	/**
	 * The hash of the empty node.
	 */
	private final byte[] hashOfEmpty;

	/**
	 * The root of the trie.
	 */
	private final byte[] root;

	/**
	 * Creates a new Merkle-Patricia trie supported by the given underlying store,
	 * using the given hashing algorithms to hash nodes and values.
	 * 
	 * @param store the store used to store the nodes of the tree, as a mapping from nodes' hashes
	 *              to the marshalled representation of the nodes
	 * @param root the root of the trie; pass it empty to create an empty trie
	 * @param hasherForKeys the hasher for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueToBytes a function that marshals values into their byte representation
	 * @param bytesToValue a function that unmarshals bytes into the represented value
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data can be garbage collected (eventually)
	 *                        because they become unreachable as result of the store updates
	 *                        performed during commit {@code numerOfCommits}; this value could
	 *                        be -1L if the trie is only used for reading, so that there is no need
	 *                        to keep track of keys that can be garbage-collected
	 */
	public AbstractPatriciaTrieImpl(KeyValueStore store, Optional<byte[]> root,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes,
			ToBytes<? super Value> valueToBytes, FromBytes<? extends Value> bytesToValue, long numberOfCommits) {

		this.store = store;
		this.hasherForKeys = hasherForKeys;
		this.hasherForNodes = hashingForNodes.getHasher(AbstractNode::toByteArray);
		this.bytesToValue = bytesToValue;
		this.valueToBytes = valueToBytes;
		this.hashOfEmpty = hasherForNodes.hash(new Empty());
		this.numberOfCommits = numberOfCommits;
		this.root = root.orElse(hashOfEmpty);
	}

	/**
	 * Clones the given trie, but for its root, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param root the root to use in the cloned trie
	 */
	protected AbstractPatriciaTrieImpl(AbstractPatriciaTrieImpl<Key, Value, T> cloned, byte[] root) {
		this.store = cloned.store;
		this.hasherForKeys = cloned.hasherForKeys;
		this.hasherForNodes = cloned.hasherForNodes;
		this.bytesToValue = cloned.bytesToValue;
		this.valueToBytes = cloned.valueToBytes;
		this.hashOfEmpty = cloned.hashOfEmpty;
		this.numberOfCommits = cloned.numberOfCommits;
		this.root = root.clone();
	}

	/**
	 * Clones the given trie, but for its supporting store, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param store the store to use in the cloned trie
	 */
	protected AbstractPatriciaTrieImpl(AbstractPatriciaTrieImpl<Key, Value, T> cloned, KeyValueStore store) {
		this.store = store;
		this.hasherForKeys = cloned.hasherForKeys;
		this.hasherForNodes = cloned.hasherForNodes;
		this.bytesToValue = cloned.bytesToValue;
		this.valueToBytes = cloned.valueToBytes;
		this.hashOfEmpty = cloned.hashOfEmpty;
		this.numberOfCommits = cloned.numberOfCommits;
		this.root = cloned.root;
	}

	@Override
	public Optional<Value> get(Key key) throws TrieException {
		try {
			byte[] hashedKey = hasherForKeys.hash(key);
			byte[] nibblesOfHashedKey = toNibbles(hashedKey);
			AbstractNode rootNode = getNodeFromHash(root, 0);
			return Optional.of(rootNode.get(nibblesOfHashedKey, 0));
		}
		catch (UnknownKeyException e) {
			return Optional.empty();
		}
	}

	@Override
	public T put(Key key, Value value) throws TrieException {
		byte[] hashedKey = hasherForKeys.hash(key);
		byte[] nibblesOfHashedKey = toNibbles(hashedKey);
		AbstractNode oldRoot = getNodeFromHash(root, 0);
		AbstractNode newRoot = oldRoot.put(nibblesOfHashedKey, 0, value);
		oldRoot.markAsGarbageCollectable(root);
		return checkoutAt(hasherForNodes.hash(newRoot));
	}

	@Override
	public final byte[] getRoot() {
		return root.clone();
	}

	/**
	 * Yields the key/value store that supports this trie.
	 * 
	 * @return the key/value store that supports this trie
	 */
	protected final KeyValueStore getStore() {
		return store;
	}

	/**
	 * Factory method that unmarshals a node from the given stream.
	 * 
	 * @param ois the stream
	 * @param cursor the number of nibbles in the path from the root of the trie to the node;
	 *               this is needed in order to foresee the size of the leaves
	 * @return the node
	 * @throws IOException if the node could not be unmarshalled
	 */
	private AbstractNode from(ObjectInputStream ois, int cursor) throws IOException {
		byte kind = ois.readByte();

		if (kind == 0x00 || (kind & 0xf0) == 0x10) {
			int nodeHashSize = hasherForNodes.length();
			int sharedBytesLength = ois.available() - nodeHashSize + 1;
			var sharedBytes = new byte[sharedBytesLength];
			sharedBytes[0] = kind;
			if (sharedBytesLength - 1 != ois.readNBytes(sharedBytes, 1, sharedBytesLength - 1))
				throw new IOException("Nibbles length mismatch in an extension node of a Patricia trie");

			byte[] sharedNibbles = expandBytesIntoNibbles(sharedBytes, (byte) 0x00);
			byte[] next = ois.readAllBytes();

			return new Extension(sharedNibbles, next);
		}
		else if (kind == 0x04) {
			short selector = ois.readShort();
			int nodeHashSize = hasherForNodes.length();
			var children = new byte[16][];
			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if ((selector & bit) != 0) {
					children[pos] = new byte[nodeHashSize];
					if (nodeHashSize != ois.readNBytes(children[pos], 0, nodeHashSize))
						throw new IOException("Hash length mismatch in Patricia node");
				}

			return new Branch(children);
		}
		else if (kind == 0x02 || (kind & 0xf0) == 0x30) {
			int expected;
			if (cursor % 2 == 0)
				expected = hasherForKeys.length() - cursor / 2 + 1;
			else
				expected = hasherForKeys.length() - cursor / 2;

			var nibbles = new byte[expected];
			nibbles[0] = kind;
			if (expected - 1 != ois.readNBytes(nibbles, 1, expected - 1))
				throw new IOException("keyEnd length mismatch in a leaf node of a Patricia trie");

			byte[] keyEnd = expandBytesIntoNibbles(nibbles, (byte) 0x02);
			byte[] value = ois.readAllBytes();

			return new Leaf(keyEnd, value);
		}
		else if (kind == 0x05)
			return new Empty();
		else
			throw new IOException("Unexpected Patricia node kind: " + kind);
	}

	/**
	 * Yields the node whose hash is the given one.
	 * 
	 * @param hash the hash of the node to look up; this must exist in this trie
	 * @param cursor the number of nibbles in the path from the root of the trie to the node;
	 *               this is needed in order to foresee the size of the leaves
	 * @return the node
	 * @throws TrieException if this trie is not able to complete the operation correctly
	 */
	private AbstractNode getNodeFromHash(byte[] hash, int cursor) throws TrieException {
		if (Arrays.equals(hash, hashOfEmpty))
			return new Empty();

		try (var ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(store.get(hash))))) {
			return from(ois, cursor);
		}
		catch (UnknownKeyException e) {
			throw new TrieException("This trie refers to a node that cannot be found in the trie itself", e);
		}
		catch (KeyValueStoreException | IOException e) {
			throw new TrieException(e);
		}
	}

	/**
	 * Splits each byte into two nibbles and yields the resulting array of nibbles.
	 * 
	 * @param original the original array of bytes; each byte will be split into two nibbles
	 * @return the resulting array of nibbles; each element of this array contains
	 *         a nibble in the least significant 4 bits; the most significant 4 bits
	 *         are constantly set to 0
	 */
	private static byte[] toNibbles(byte[] original) {
		int length = original.length;
		var split = new byte[length * 2];
		int pos = 0;
		for (byte b: original) {
			split[pos++] = (byte) ((b & 0xf0) >> 4);
			split[pos++] = (byte) (b & 0x0f);
		}
	
		return split;
	}

	/**
	 * Compacts the given nibbles into bytes, using the given selector
	 * as first nibble if the array has odd length.
	 * 
	 * @param nibbles the nibbles
	 * @param evenSelector the selector byte prefixed to even arrays of nibbles
	 * @param oddSelector the selector nibble prefixed to odd arrays of nibbles
	 * @return the resulting bytes
	 */
	private static byte[] compactNibblesIntoBytes(byte[] nibbles, byte evenSelector, byte oddSelector) {
		int length = nibbles.length;
		var result = new byte[1 + length / 2];

		if (length % 2 == 0) {
			result[0] = evenSelector;
			for (int pos = 0; pos < length; pos += 2)
				result[1 + pos / 2] = (byte) ((nibbles[pos] << 4) | nibbles[pos + 1]);
		}
		else {
			result[0] = (byte) ((oddSelector << 4) | nibbles[0]);
			for (int pos = 1; pos < length; pos += 2)
				result[1 + pos / 2] = (byte) ((nibbles[pos] << 4) | nibbles[pos + 1]);
		}

		return result;
	}

	private static byte[] expandBytesIntoNibbles(byte[] bytes, byte evenSelector) {
		byte[] nibbles;

		if (bytes[0] == evenSelector) {
			nibbles = new byte[(bytes.length - 1) * 2];
			for (int pos = 1; pos < bytes.length; pos++) {
				nibbles[(pos - 1) * 2] = (byte) ((bytes[pos] & 0xf0) >> 4);
				nibbles[pos * 2 - 1] = (byte) (bytes[pos] & 0x0f);
			}
		}
		else {
			nibbles = new byte[bytes.length * 2 - 1];
			nibbles[0] = (byte) (bytes[0] & 0x0f);

			for (int pos = 1; pos < bytes.length; pos++) {
				nibbles[pos * 2 - 1] = (byte) ((bytes[pos] & 0xf0) >> 4);
				nibbles[pos * 2] = (byte) (bytes[pos] & 0x0f);
			}
		}

		return nibbles;
	}

	/**
	 * A node of a Merkle-Patricia tree.
	 */
	private abstract class AbstractNode extends AbstractMarshallable {

		/**
		 * Yields the value bound to the given key.
		 * It considers only the portion of the key starting at the {@code cursor}th nibble.
		 * 
		 * @param nibblesOfHashedKey the nibbles of the key to look up; only the 4 least significant bits
		 *                           of each element are significant; the 4 most significant bits must be
		 *                           constantly 0
		 * @param cursor the starting point of the significant portion of {@code nibblesOfHashedKey}
		 * @return the value
		 * @throws UnknownKeyException if there is not such value
		 * @throws TrieException if the trie is not able to complete the operation correctly
		 */
		protected abstract Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException, TrieException;

		/**
		 * Binds the given value to the given key.
		 * It considers only the portion of the key starting at the {@code cursor}th nibble.
		 * 
		 * @param nibblesOfHashedKey the nibbles of the key to look up; only the 4 least significant bits
		 *                           of each element are relevant; the 4 most significant bits must be 0
		 * @param cursor the starting point of the significant portion of {@code nibblesOfHashedKey}
		 * @param value the value
		 * @return the new node that replaced this in the trie; if the key was already bound to the same
		 *         value, then this node will coincide with this, that is, they have the same hash
		 * @throws TrieException if the trie is not able to complete the operation correctly
		 */
		protected abstract AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws TrieException;

		/**
		 * Persist this node in the store of the trie.
		 * 
		 * @return this same node
		 * @throws TrieException if the trie is not able to complete the operation correctly
		 */
		protected final AbstractNode putInStore() throws TrieException {
			try {
				store.put(hasherForNodes.hash(this), toByteArray()); // we bind it to its hash in the store
				return this;
			}
			catch (KeyValueStoreException e) {
				throw new TrieException(e);
			}
		}

		/**
		 * Takes note that this node, having the given key (hash), became garbage during an update
		 * occurred during the current commit.
		 * 
		 * @param key the key that became garbage
		 * @throws TrieException if this trie is not able to complete the operation correctly
		 */
		protected void markAsGarbageCollectable(byte[] key) throws TrieException {
			//long numberOfGarbageKeys = getNumberOfGarbageKeys(numberOfCommits);
			//setGarbageKey(numberOfCommits, numberOfGarbageKeys, key);
			//setNumberOfGarbageKeys(numberOfCommits, numberOfGarbageKeys + 1);
		}
	}

	/**
	 * A branch node of a Patricia trie.
	 */
	private class Branch extends AbstractNode {

		/**
		 * The hashes of the branching children of the node. If the nth child is missing,
		 * the array will hold {@code hashOfEmpty} for it.
		 */
		private final byte[][] children;

		/**
		 * Builds a branch node of a Patricia trie.
		 * 
		 * @param children the hashes of the branching children of the node.
		 *                 If the nth child is missing the array can hold null for it,
		 *                 which will be replaced with {@code hashOfEmpty}
		 */
		private Branch(byte[][] children) {
			this.children = children;

			for (int pos = 0; pos < children.length; pos++)
				if (this.children[pos] == null)
					this.children[pos] = hashOfEmpty.clone();
		}

		/**
		 * Yields a bitmap that describes which children exist.
		 * 
		 * @return the bitmap
		 */
		private short selector() {
			short result = 0;

			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if (!Arrays.equals(children[pos], hashOfEmpty))
					result |= bit;

			return result;
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeByte(0x04);
			context.writeShort(selector());

			for (byte[] child: children)
				// useless to write the empty nodes, since the selector keeps the same information
				if (!Arrays.equals(child, hashOfEmpty))
					context.writeBytes(child);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, final int cursor) throws UnknownKeyException, TrieException {
			if (cursor >= nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie nibblesOfHashedKey.length = " + nibblesOfHashedKey.length + ", cursor = " + cursor);

			byte selection = nibblesOfHashedKey[cursor];
			if (Arrays.equals(children[selection], hashOfEmpty))
				throw new UnknownKeyException("Key not found in Patricia trie");

			return getNodeFromHash(children[selection], cursor + 1).get(nibblesOfHashedKey, cursor + 1);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, final int cursor, Value value) throws TrieException {
			if (cursor >= nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie");

			byte selection = nibblesOfHashedKey[cursor];
			AbstractNode oldChild = getNodeFromHash(children[selection], cursor + 1); // we recur
			AbstractNode newChild = oldChild.put(nibblesOfHashedKey, cursor + 1, value);
			oldChild.markAsGarbageCollectable(children[selection]);
			byte[][] childrenCopy = children.clone();
			childrenCopy[selection] = hasherForNodes.hash(newChild);

			return new Branch(childrenCopy).putInStore();
		}
	}

	/**
	 * An extension node of a Patricia trie.
	 */
	private class Extension extends AbstractNode {

		/**
		 * The prefix nibbles shared among all paths passing through this node.
		 * Each byte uses only its least significant 4 bits (a nibble).
		 * Its 4 most significant bits are constantly set to 0.
		 * This array is never empty.
		 */
		private final byte[] sharedNibbles;

		/**
		 * The hash of the next node, the only child of this node.
		 */
		private final byte[] next;

		/**
		 * Builds an extension node of a Patricia trie.
		 * 
		 * @param sharedNibbles the prefix nibbles shared among all paths passing through this node.
		 *                      Each byte uses only its least significant 4 bits (a nibble).
		 *                      It 4 most significant bits are constantly set to 0.
		 *                      This array is never empty
		 * @param next the hash of the next node, the only child of the extension node
		 */
		private Extension(byte[] sharedNibbles, byte[] next) {
			this.sharedNibbles = sharedNibbles;
			this.next = next;
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeBytes(compactNibblesIntoBytes(sharedNibbles, (byte) 0x00, (byte) 0x01));
			context.writeBytes(next);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException, TrieException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < sharedNibbles.length; cursor1++, cursor++)
				if (sharedNibbles[cursor1] != nibblesOfHashedKey[cursor])
					throw new UnknownKeyException("Key not found in Patricia trie");

			if (cursor1 != sharedNibbles.length || cursor >= nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie");

			return getNodeFromHash(next, cursor).get(nibblesOfHashedKey, cursor);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, final int cursor, Value value) throws TrieException {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < sharedNibbles.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == sharedNibbles[lengthOfSharedPortion])
				 lengthOfSharedPortion++;

			int lengthOfDistinctPortion = sharedNibbles.length - lengthOfSharedPortion;

			if (lengthOfDistinctPortion == 0) {
				AbstractNode oldNext = getNodeFromHash(next, sharedNibbles.length + cursor);
				AbstractNode newNext = oldNext.put(nibblesOfHashedKey, sharedNibbles.length + cursor, value); // we recur
				oldNext.markAsGarbageCollectable(next);

				return new Extension(sharedNibbles, hasherForNodes.hash(newNext)).putInStore();
			}
			else {
				var sharedNibbles1 = new byte[sharedNibbles.length - lengthOfSharedPortion - 1];
				System.arraycopy(sharedNibbles, lengthOfSharedPortion + 1, sharedNibbles1, 0, sharedNibbles1.length);
				var keyEnd2 = new byte[nibblesOfHashedKey.length - cursor - lengthOfSharedPortion - 1];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = sharedNibbles[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				var children = new byte[16][];
				byte[] hashOfChild1 = (sharedNibbles1.length == 0) ? next : hasherForNodes.hash(new Extension(sharedNibbles1, next).putInStore());
				AbstractNode child2;

				try {
					child2 = new Leaf(keyEnd2, valueToBytes.get(value)).putInStore();
				}
				catch (IOException e) {
					throw new TrieException(e);
				}

				children[selection1] = hashOfChild1;
				children[selection2] = hasherForNodes.hash(child2);

				AbstractNode branch = new Branch(children).putInStore();

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternatives
					var sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(this.sharedNibbles, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hasherForNodes.hash(branch)).putInStore();
				}
				else
					// yield a branch node with two alternatives
					return branch;
			}
		}
	}

	/**
	 * A leaf node of a Patricia trie.
	 */
	private class Leaf extends AbstractNode {

		/**
		 * The key end of the only path passing through this node.
		 * Each byte uses only its least significant 4 bits (a nibble).
		 * Its 4 most significant bits are constantly set to 0.
		 * This array can be empty.
		 */
		private final byte[] keyEnd;

		/**
		 * The marshalled bytes of the value bound to the key leading to this node.
		 */
		private final byte[] value;

		/**
		 * Builds an extension node of a Patricia trie.
		 * 
		 * @param keyEnd the key end of the only path passing through this node.
		 *               Each byte uses only its least significant 4 bits (a nibble).
		 *               Its 4 most significant bits are constantly set to 0. This
		 *               array can be empty
		 * @param value the marshalled bytes of the value bound to the key leading to this node
		 */
		private Leaf(byte[] keyEnd, byte[] value) {
			this.keyEnd = keyEnd;
			this.value = value;
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeBytes(compactNibblesIntoBytes(keyEnd, (byte) 0x02, (byte) 0x03));
			context.writeBytes(value);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException, TrieException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < keyEnd.length; cursor1++, cursor++)
				if (keyEnd[cursor1] != nibblesOfHashedKey[cursor])
					throw new UnknownKeyException("Key not found in Patricia trie");

			if (cursor1 != keyEnd.length || cursor != nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie: " + (cursor1 != keyEnd.length) + ", " + (cursor != nibblesOfHashedKey.length));

			try {
				return bytesToValue.get(value);
			}
			catch (IOException e) {
				throw new TrieException(e);
			}
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws TrieException {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < keyEnd.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == keyEnd[lengthOfSharedPortion])
				lengthOfSharedPortion++;

			int lengthOfDistinctPortion = keyEnd.length - lengthOfSharedPortion;

			try {
				if (lengthOfDistinctPortion == 0)
					// the keys coincide
					return new Leaf(keyEnd, valueToBytes.get(value)).putInStore();
				else {
					// since there is a distinct portion, there must be at least a nibble in keyEnd
					var keyEnd1 = new byte[keyEnd.length - lengthOfSharedPortion - 1];
					System.arraycopy(keyEnd, lengthOfSharedPortion + 1, keyEnd1, 0, keyEnd1.length);
					var keyEnd2 = new byte[keyEnd1.length];
					System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
					byte selection1 = keyEnd[lengthOfSharedPortion];
					byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
					var children = new byte[16][];
					var leaf1 = new Leaf(keyEnd1, this.value).putInStore();
					var leaf2 = new Leaf(keyEnd2, valueToBytes.get(value)).putInStore();
					children[selection1] = hasherForNodes.hash(leaf1);
					children[selection2] = hasherForNodes.hash(leaf2);
					var branch = new Branch(children).putInStore();

					if (lengthOfSharedPortion > 0) {
						// yield an extension node linked to a branch node with two alternatives leaves
						var sharedNibbles = new byte[lengthOfSharedPortion];
						System.arraycopy(keyEnd, 0, sharedNibbles, 0, lengthOfSharedPortion);
						return new Extension(sharedNibbles, hasherForNodes.hash(branch)).putInStore();
					}
					else
						// yield a branch node with two alternatives leaves
						return branch;
				}
			}
			catch (IOException e) {
				throw new TrieException(e);
			}
		}
	}

	/**
	 * An empty node, that represents an empty Patricia trie.
	 */
	private class Empty extends AbstractNode {

		/**
		 * Builds an empty node of a Patricia trie.
		 */
		private Empty() {}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeByte((byte) 0x05);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException {
			throw new UnknownKeyException("Key not found in Patricia trie");
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws TrieException {
			var nibblesEnd = new byte[nibblesOfHashedKey.length - cursor];
			System.arraycopy(nibblesOfHashedKey, cursor, nibblesEnd, 0, nibblesEnd.length);

			try {
				return new Leaf(nibblesEnd, valueToBytes.get(value)).putInStore();
			}
			catch (IOException e) {
				throw new TrieException(e);
			}
		}

		@Override
		protected void markAsGarbageCollectable(byte[] key) throws TrieException {
			// we disable garbage collection for the empty nodes, since they are not kept in store
		}
	}
}