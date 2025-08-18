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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.UnmarshallingContexts;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.patricia.FromBytes;
import io.hotmoka.patricia.ToBytes;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * Implementation of a Merkle-Patricia trie.
 *
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 * @param <T> the type of this trie
 */
@Immutable
public abstract class AbstractPatriciaTrieImpl<Key, Value, T extends AbstractPatriciaTrieImpl<Key, Value, T>> implements PatriciaTrie<Key, Value, T> {

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
	 * The only instance of the Empty node.
	 */
	private final Empty EMPTY = new Empty();

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
	 * @param root the root of the trie
	 * @param hasherForKeys the hasher for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param hashOfEmpty the hash of the empty trie
	 * @param valueToBytes a function that marshals values into their byte representation
	 * @param bytesToValue a function that unmarshals bytes into the represented value; it is assumed
	 *                     that this function always works, without exception, if the bytes have been
	 *                     previously marshalled by {@code valueToBytes}
	 * @throws UnknownKeyException if {@code root} cannot be found in {@code store}
	 */
	public AbstractPatriciaTrieImpl(KeyValueStore store, byte[] root,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes, byte[] hashOfEmpty,
			ToBytes<? super Value> valueToBytes, FromBytes<? extends Value> bytesToValue) throws UnknownKeyException {

		this.store = store;
		this.hasherForKeys = hasherForKeys;
		// the hashing of the nodes does not consider the reference counter
		var hasher = hashingForNodes.getHasher(AbstractNode::toByteArrayWithoutReferenceCounter);
		this.hashOfEmpty = hashOfEmpty.clone();
		this.hasherForNodes = new Hasher<>() {

			@Override
			public byte[] hash(AbstractNode what) {
				if (what instanceof Empty)
					return AbstractPatriciaTrieImpl.this.hashOfEmpty;
				else
					return hasher.hash(what);
			}

			@Override
			public byte[] hash(AbstractNode what, int start, int length) {
				if (what instanceof Empty)
					return AbstractPatriciaTrieImpl.this.hashOfEmpty;
				else
					return hasher.hash(what, start, length);
			}

			@Override
			public int length() {
				return hasher.length();
			}
		};

		this.bytesToValue = bytesToValue;
		this.valueToBytes = valueToBytes;
		this.root = root.clone();

		enforceExistence();
	}

	/**
	 * Clones the given trie, but for its root, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param root the root to use in the cloned trie
	 * @throws UnknownKeyException if {@code root} cannot be found in the store of this trie
	 */
	protected AbstractPatriciaTrieImpl(AbstractPatriciaTrieImpl<Key, Value, T> cloned, byte[] root) throws UnknownKeyException {
		this.store = cloned.store;
		this.hasherForKeys = cloned.hasherForKeys;
		this.hasherForNodes = cloned.hasherForNodes;
		this.bytesToValue = cloned.bytesToValue;
		this.valueToBytes = cloned.valueToBytes;
		this.hashOfEmpty = cloned.hashOfEmpty;
		this.root = root.clone();

		enforceExistence();
	}

	@Override
	public Optional<Value> get(Key key) {
		try {
			byte[] hashedKey = hasherForKeys.hash(key);
			byte[] nibblesOfHashedKey = toNibbles(hashedKey);
			AbstractNode rootNode = getNodeFromExistingHash(root, 0);
			return Optional.of(rootNode.get(nibblesOfHashedKey, 0));
		}
		catch (UnknownKeyException e) {
			return Optional.empty();
		}
	}

	@Override
	public T put(Key key, Value value) {
		byte[] hashedKey = hasherForKeys.hash(key);
		byte[] nibblesOfHashedKey = toNibbles(hashedKey);
		AbstractNode oldRoot = getNodeFromExistingHash(root, 0);
		AbstractNode newRoot = oldRoot.put(nibblesOfHashedKey, 0, value);

		try {
			return checkoutAt(hasherForNodes.hash(newRoot));
		}
		catch (UnknownKeyException e) {
			// we just got newRoot as result of the insertion, hence it must exist in store
			// or otherwise the store is corrupted
			throw new TrieException(e);
		}
	}

	@Override
	public final byte[] getRoot() {
		return root.clone();
	}

	/**
	 * Increases the allocation counter of the root of this trie.
	 */
	protected void malloc() {
		// the empty node is no created, nor allocated, nor freed,
		// better avoid a useless database access
		if (!Arrays.equals(hashOfEmpty, root))
			incrementReferenceCountOfNode(root, 0);
	}

	/**
	 * Frees this trie, removing from the store all nodes that were only used for this trie.
	 */
	protected void free() {
		// the empty node is no created, nor allocated, nor freed,
		// better avoid a useless database access
		if (!Arrays.equals(hashOfEmpty, root))
			freeNodeWithHash(root, 0);
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
	 * Frees a node, given its hash. The node is assumed to be in store.
	 * 
	 * @param hash the hash of the node
	 * @param cursor the number of nibbles in the path from the root of the trie to the node;
	 *               this is needed in order to foresee the size of the leaves
	 */
	private void freeNodeWithHash(byte[] hash, int cursor) {
		getNodeFromExistingHash(hash, cursor).free(hash, cursor);
	}

	/**
	 * Factory method that unmarshals a node from the given stream.
	 * 
	 * @param context the stream
	 * @param cursor the number of nibbles in the path from the root of the trie to the node;
	 *               this is needed in order to foresee the size of the leaves
	 * @return the node
	 * @throws IOException if the node could not be unmarshalled
	 */
	private AbstractNode from(UnmarshallingContext context, final int cursor) throws IOException {
		int counter = context.readCompactInt();
		byte kind = context.readByte();

		if (kind == 0x00 || (kind & 0xf0) == 0x10) {
			int nodeHashSize = hasherForNodes.length();
			int sharedBytesLength = context.available() - nodeHashSize + 1;
			var sharedBytes = new byte[sharedBytesLength];
			sharedBytes[0] = kind;
			if (sharedBytesLength - 1 != context.readNBytes(sharedBytes, 1, sharedBytesLength - 1))
				throw new IOException("Nibbles length mismatch in an extension node of a Patricia trie");

			byte[] sharedNibbles = expandBytesIntoNibbles(sharedBytes, (byte) 0x00);
			byte[] next = context.readAllBytes();

			return new Extension(sharedNibbles, next, counter);
		}
		else if (kind == 0x04) {
			short selector = context.readShort();
			int nodeHashSize = hasherForNodes.length();
			var children = new byte[16][];
			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if ((selector & bit) != 0) {
					children[pos] = new byte[nodeHashSize];
					if (nodeHashSize != context.readNBytes(children[pos], 0, nodeHashSize))
						throw new IOException("Hash length mismatch in Patricia node");
				}

			return new Branch(children, counter);
		}
		else if (kind == 0x02 || (kind & 0xf0) == 0x30) {
			int expected;
			if (cursor % 2 == 0)
				expected = hasherForKeys.length() - cursor / 2 + 1;
			else
				expected = hasherForKeys.length() - cursor / 2;

			var bytes = new byte[expected];
			bytes[0] = kind;
			if (expected - 1 != context.readNBytes(bytes, 1, expected - 1))
				throw new IOException("keyEnd length mismatch in a leaf node of a Patricia trie");

			byte[] keyEnd = expandBytesIntoNibbles(bytes, (byte) 0x02);
			byte[] value = context.readAllBytes();

			return new Leaf(keyEnd, value, counter);
		}
		else if (kind == 0x05)
			return EMPTY;
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
	 */
	private AbstractNode getNodeFromExistingHash(byte[] hash, int cursor) {
		try {
			return getNodeFromHash(hash, cursor);
		}
		catch (IOException e) {
			// the database must be corrupted
			throw new TrieException(e);
		}
		catch (UnknownKeyException e) {
			throw new TrieException("This trie refers to a node that cannot be found in the trie itself", e);
		}
	}

	/**
	 * Yields the node whose hash is the given one.
	 * 
	 * @param hash the hash of the node to look up
	 * @param cursor the number of nibbles in the path from the root of the trie to the node;
	 *               this is needed in order to foresee the size of the leaves
	 * @return the node
	 * @throws IOException if there is an I/O problem for accessing the database
	 * @throws UnknownKeyException if there is no node with the given hash
	 */
	private AbstractNode getNodeFromHash(byte[] hash, int cursor) throws IOException, UnknownKeyException {
		if (Arrays.equals(hash, hashOfEmpty))
			return EMPTY;

		try (var bais = new ByteArrayInputStream(store.get(hash)); var context = UnmarshallingContexts.of(bais)) {
			return from(context, cursor);
		}
	}

	/**
	 * Enforces the existence of the root of this trie inside its store.
	 * 
	 * @throws UnknownKeyException if the root of this trie cannot be found in its store
	 */
	private void enforceExistence() throws UnknownKeyException {
		if (!Arrays.equals(root, hashOfEmpty))
			store.get(root);
	}

	/**
	 * Increments the reference counter of the given node in store.
	 * 
	 * @param hash the hash of the node whose reference counter must be incremented; this must exist in store
	 * @param cursor the distance of this node from the root of the trie (number of nibbles of the key)
	 */
	private void incrementReferenceCountOfNode(byte[] hash, int cursor) {
		var node = getNodeFromExistingHash(hash, cursor);
		node = node.withIncrementedReferenceCounter();
		store.put(hash, node.toByteArray());
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

	//private static long freed, allocated;

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
	@Immutable
	private abstract class AbstractNode extends AbstractMarshallable {

		/**
		 * The reference counter of this node (number of pointers to this node).
		 */
		protected final int count;

		/**
		 * Builds a node.
		 * 
		 * @param count the reference counter of the node (number of pointers leading to the node)
		 */
		protected AbstractNode(int count) {
			this.count = count;
		}

		@Override
		public final void into(MarshallingContext context) throws IOException {
			context.writeCompactInt(count);
			intoWithoutReferenceCounter(context);
		}

		/**
		 * Persist this node in the store of the trie.
		 * 
		 * @param cursor the distance of this node from the root of the trie (number of nibbles of the key)
		 * @return this same node
		 */
		protected final AbstractNode putInStore(int cursor) {
			byte[] hash = hasherForNodes.hash(this);
		
			try {
				// if an equal node exists in store (without considering its reference counter), then we return it,
				// so that the result will have the current reference counter for the node
				return getNodeFromHash(hash, cursor);
			}
			catch (IOException e) {
				// the database must be corrupted
				throw new TrieException(e);
			}
			catch (UnknownKeyException e) {
				//System.out.printf("%d/%d: %.2f\t\t", freed, ++allocated, freed * 100.0 / allocated);
				store.put(hash, toByteArray()); // we bind it to its hash in the store
				incrementReferenceCounterOfDescedants(cursor);
				return this;
			}
		}

		/**
		 * Removes this node from the store if its reference counter is 1; if this node
		 * gets removed, then the reference counter of the descendants gets decremented and
		 * they get potentially freed as well. It assumes that this node is present in store.
		 * 
		 * @param hash the hash of this same node; this must exist in store
		 * @param cursor the distance of this node from the root of the trie (number of nibbles of the key)
		 */
		protected void free(byte[] hash, int cursor) {
			AbstractNode replacement = withDecrementedReferenceCounter();

			try {
				if (replacement.count > 0)
					store.put(hash, replacement.toByteArray());
				else {
					//System.out.printf("%d/%d: %.2f\t\t", ++freed, allocated, freed * 100.0 / allocated);
					store.remove(hash);
					freeDescendants(cursor);
				}
			}
			catch (UnknownKeyException e) {
				// this node was assumed to exist in store, hence the store in corrupted
				throw new TrieException(e);
			}
		}

		/**
		 * Calls {@link #free(byte[], int)} on the descendants of this node.
		 *
		 * @param cursor the distance of this node from the root of the trie (number of nibbles of the key)
		 */
		protected abstract void freeDescendants(int cursor);

		/**
		 * Increments the reference counter of the descendants of this node.
		 * 
		 * @param cursor the distance of this node from the root of the trie (number of nibbles of the key)
		 */
		protected abstract void incrementReferenceCounterOfDescedants(int cursor);

		/**
		 * Yields a node identical to this but whose reference counter has been incremented by one.
		 * 
		 * @return the resulting node
		 */
		protected abstract AbstractNode withIncrementedReferenceCounter();

		/**
		 * Yields a node identical to this but whose reference counter has been decremented by one.
		 * 
		 * @return the resulting node
		 */
		protected abstract AbstractNode withDecrementedReferenceCounter();

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
		 */
		protected abstract Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException;

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
		 */
		protected abstract AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value);

		/**
		 * Marshals this object into the given context, but does not report the reference counter.
		 * 
		 * @param context the marshalling context
		 * @throws IOException if marshalling fails
		 */
		protected abstract void intoWithoutReferenceCounter(MarshallingContext context) throws IOException;

		/**
		 * Transforms this node into an array, but without the reference counter.
		 * 
		 * @return the resulting array
		 */
		private byte[] toByteArrayWithoutReferenceCounter() {
			try (var baos = new ByteArrayOutputStream(); var context = createMarshallingContext(baos)) {
				intoWithoutReferenceCounter(context);
				context.flush();
				return baos.toByteArray();
			}
			catch (IOException e) {
				// impossible with a ByteArrayOutputStream
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * A branch node of a Patricia trie.
	 */
	private class Branch extends AbstractNode {

		/**
		 * The hashes of the branching children of the node. If the nth child is missing,
		 * this array will hold {@code hashOfEmpty} for it.
		 */
		private final byte[][] children;

		/**
		 * Builds a branch node of a Patricia trie.
		 * 
		 * @param children the hashes of the branching children of the node.
		 *                 If the nth child is missing, this array can hold null for it,
		 *                 which will be replaced with {@code hashOfEmpty}
		 * @param count the number of pointers leading into the node
		 */
		private Branch(byte[][] children, int count) {
			super(count);

			this.children = children;

			for (int pos = 0; pos < children.length; pos++)
				if (children[pos] == null)
					this.children[pos] = hashOfEmpty;
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
		protected void intoWithoutReferenceCounter(MarshallingContext context) throws IOException {
			context.writeByte(0x04);
			context.writeShort(selector());

			for (byte[] child: children)
				// useless to write the empty nodes, since the selector keeps the same information
				if (!Arrays.equals(child, hashOfEmpty))
					context.writeBytes(child);
		}

		@Override
		protected void incrementReferenceCounterOfDescedants(int cursor) {
			int cursorOfChildren = cursor + 1;

			for (byte[] child: children)
				if (!Arrays.equals(child, hashOfEmpty))
					incrementReferenceCountOfNode(child, cursorOfChildren);
		}

		@Override
		protected AbstractNode withIncrementedReferenceCounter() {
			return new Branch(children, count + 1);
		}

		@Override
		protected AbstractNode withDecrementedReferenceCounter() {
			return new Branch(children, count - 1);
		}

		@Override
		protected void freeDescendants(int cursor) {
			int cursorOfChildren = cursor + 1;

			for (byte[] child: children)
				if (!Arrays.equals(child, hashOfEmpty))
					freeNodeWithHash(child, cursorOfChildren);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, final int cursor) throws UnknownKeyException {
			if (cursor >= nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie nibblesOfHashedKey.length = " + nibblesOfHashedKey.length + ", cursor = " + cursor);

			byte selection = nibblesOfHashedKey[cursor];
			byte[] child = children[selection];
			if (Arrays.equals(child, hashOfEmpty))
				throw new UnknownKeyException("Key not found in Patricia trie");

			return getNodeFromExistingHash(child, cursor + 1).get(nibblesOfHashedKey, cursor + 1);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, final int cursor, Value value) {
			if (cursor >= nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie");

			byte selection = nibblesOfHashedKey[cursor];
			AbstractNode oldChild = getNodeFromExistingHash(children[selection], cursor + 1); // we recur
			AbstractNode newChild = oldChild.put(nibblesOfHashedKey, cursor + 1, value);
			// the following only clones the backbone of the array, not its elements, which is fine
			byte[][] childrenCopy = children.clone();
			childrenCopy[selection] = hasherForNodes.hash(newChild);

			return new Branch(childrenCopy, 0).putInStore(cursor);
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
		 * @param count the number of pointers leading into the node
		 */
		private Extension(byte[] sharedNibbles, byte[] next, int count) {
			super(count);

			this.sharedNibbles = sharedNibbles;
			this.next = next;
		}

		@Override
		protected void intoWithoutReferenceCounter(MarshallingContext context) throws IOException {
			context.writeBytes(compactNibblesIntoBytes(sharedNibbles, (byte) 0x00, (byte) 0x01));
			context.writeBytes(next);
		}

		@Override
		protected void incrementReferenceCounterOfDescedants(int cursor) {
			incrementReferenceCountOfNode(next, cursor + sharedNibbles.length);
		}

		@Override
		protected AbstractNode withIncrementedReferenceCounter() {
			return new Extension(sharedNibbles, next, count + 1);
		}

		@Override
		protected AbstractNode withDecrementedReferenceCounter() {
			return new Extension(sharedNibbles, next, count - 1);
		}

		@Override
		protected void freeDescendants(int cursor) {
			freeNodeWithHash(next, cursor + sharedNibbles.length);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < sharedNibbles.length; cursor1++, cursor++)
				if (sharedNibbles[cursor1] != nibblesOfHashedKey[cursor])
					throw new UnknownKeyException("Key not found in Patricia trie");

			if (cursor1 != sharedNibbles.length || cursor >= nibblesOfHashedKey.length)
				throw new TrieException("Inconsistent key length in Patricia trie");

			return getNodeFromExistingHash(next, cursor).get(nibblesOfHashedKey, cursor);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, final int cursor, Value value) {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < sharedNibbles.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == sharedNibbles[lengthOfSharedPortion])
				 lengthOfSharedPortion++;

			int lengthOfDistinctPortion = sharedNibbles.length - lengthOfSharedPortion;

			if (lengthOfDistinctPortion == 0) {
				AbstractNode oldNext = getNodeFromExistingHash(next, sharedNibbles.length + cursor);
				AbstractNode newNext = oldNext.put(nibblesOfHashedKey, sharedNibbles.length + cursor, value); // we recur
				// TODO
				// in theory, newNext could be an Extension and consequently it could be merged with sharedNibbles:
				// but it could not see a case when this happens and consequently I do not dare to optimize this case
				return new Extension(sharedNibbles, hasherForNodes.hash(newNext), 0).putInStore(cursor);
			}
			else {
				var sharedNibbles1 = new byte[lengthOfDistinctPortion - 1];
				System.arraycopy(sharedNibbles, lengthOfSharedPortion + 1, sharedNibbles1, 0, sharedNibbles1.length);
				var keyEnd2 = new byte[nibblesOfHashedKey.length - cursor - lengthOfSharedPortion - 1];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = sharedNibbles[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				var children = new byte[16][];
				byte[] hashOfChild1 = (sharedNibbles1.length == 0) ? next : hasherForNodes.hash(new Extension(sharedNibbles1, next, 0).putInStore(cursor + lengthOfSharedPortion + 1));
				var child2 = new Leaf(keyEnd2, valueToBytes.get(value), 0).putInStore(cursor + lengthOfSharedPortion + 1);
				children[selection1] = hashOfChild1;
				children[selection2] = hasherForNodes.hash(child2);

				var branch = new Branch(children, 0).putInStore(cursor + lengthOfSharedPortion);

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternatives
					var sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(this.sharedNibbles, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hasherForNodes.hash(branch), 0).putInStore(cursor);
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
		 * @param count the number of pointers leading to the node
		 */
		private Leaf(byte[] keyEnd, byte[] value, int count) {
			super(count);

			this.keyEnd = keyEnd;
			this.value = value;
		}

		@Override
		protected void intoWithoutReferenceCounter(MarshallingContext context) throws IOException {
			context.writeBytes(compactNibblesIntoBytes(keyEnd, (byte) 0x02, (byte) 0x03));
			context.writeBytes(value);
		}

		@Override
		protected void incrementReferenceCounterOfDescedants(int cursor) {
		}

		@Override
		protected AbstractNode withIncrementedReferenceCounter() {
			return new Leaf(keyEnd, value, count + 1);
		}

		@Override
		protected AbstractNode withDecrementedReferenceCounter() {
			return new Leaf(keyEnd, value, count - 1);
		}

		@Override
		protected void freeDescendants(int cursor) {
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException {
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
				throw new TrieException("The value was previously marshalled into the trie but cannot be unmarshalled now: the database must be corrupted or the marshaller or unmarshaller is buggy", e);
			}
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < keyEnd.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == keyEnd[lengthOfSharedPortion])
				lengthOfSharedPortion++;

			int lengthOfDistinctPortion = keyEnd.length - lengthOfSharedPortion;
			if (lengthOfDistinctPortion == 0)
				// the keys coincide
				return new Leaf(keyEnd, valueToBytes.get(value), 0).putInStore(cursor);
			else {
				// since there is a distinct portion, there must be at least a nibble in keyEnd
				var keyEnd1 = new byte[keyEnd.length - lengthOfSharedPortion - 1];
				System.arraycopy(keyEnd, lengthOfSharedPortion + 1, keyEnd1, 0, keyEnd1.length);
				var keyEnd2 = new byte[keyEnd1.length];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = keyEnd[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				var children = new byte[16][];
				var leaf1 = new Leaf(keyEnd1, this.value, 0).putInStore(cursor + lengthOfSharedPortion + 1);
				var leaf2 = new Leaf(keyEnd2, valueToBytes.get(value), 0).putInStore(cursor + lengthOfSharedPortion + 1);
				children[selection1] = hasherForNodes.hash(leaf1);
				children[selection2] = hasherForNodes.hash(leaf2);
				var branch = new Branch(children, 0).putInStore(cursor + lengthOfSharedPortion);

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternative leaves
					var sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(keyEnd, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hasherForNodes.hash(branch), 0).putInStore(cursor);
				}
				else
					// yield a branch node with two alternative leaves
					return branch;
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
		private Empty() {
			// the reference counter is irrelevant, since this node is never allocated nor garbage-collected
			super(0);
		}

		@Override
		protected void intoWithoutReferenceCounter(MarshallingContext context) throws IOException {
			context.writeByte((byte) 0x05);
		}

		@Override
		protected void incrementReferenceCounterOfDescedants(int cursor) {
		}

		@Override
		protected void free(byte[] hash, int cursor) {
		}

		@Override
		protected void freeDescendants(int cursor) {
		}

		@Override
		protected AbstractNode withIncrementedReferenceCounter() {
			return this;
		}

		@Override
		protected AbstractNode withDecrementedReferenceCounter() {
			return this;
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws UnknownKeyException {
			throw new UnknownKeyException("Key not found in Patricia trie");
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) {
			var nibblesEnd = new byte[nibblesOfHashedKey.length - cursor];
			System.arraycopy(nibblesOfHashedKey, cursor, nibblesEnd, 0, nibblesEnd.length);
			return new Leaf(nibblesEnd, valueToBytes.get(value), 0).putInStore(cursor);
		}
	}
}