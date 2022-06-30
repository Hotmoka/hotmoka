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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.KeyValueStore;
import io.hotmoka.patricia.Node;
import io.hotmoka.patricia.PatriciaTrie;

public class PatriciaTrieImpl<Key, Value extends Marshallable> implements PatriciaTrie<Key, Value> {

	/**
	 * The store that supports this trie.
	 */
	private final KeyValueStore store;

	/**
	 * The hashing algorithm for the keys of the trie.
	 */
	private final HashingAlgorithm<? super Key> hashingForKeys;

	/**
	 * The hashing algorithm for the nodes of the trie.
	 */
	private final HashingAlgorithm<? super Node> hashingForNodes;

	/**
	 * A function able to unmarshall a value from its byte representation.
	 */
	private final Unmarshaller<? extends Value> valueUnmarshaller;

	/**
	 * True if and only if unused nodes must be garbage collected; in general,
	 * this can be true if previous configurations of the trie needn't be rechecked out in the future.
	 */
	private final boolean garbageCollected;

	private final static Logger logger = Logger.getLogger(PatriciaTrieImpl.class.getName());

	/**
	 * Creates a new Merkle-Patricia trie supported by the underlying store,
	 * using the given hashing algorithm to hash nodes and values.
	 * 
	 * @param store the store used to store a mapping from nodes' hashes to the marshalled
	 *              representation of the nodes
	 * @param hashingForKeys the hashing algorithm for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueUnmarshaller a function able to unmarshall a value from its byte representation
	 * @param garbageCollected true if and only if unused nodes must be garbage collected; in general,
	 *                         this can be true if previous configurations of the trie needn't be
	 *                         rechecked out in the future
	 */
	public PatriciaTrieImpl(KeyValueStore store,
			HashingAlgorithm<? super Key> hashingForKeys, HashingAlgorithm<? super Node> hashingForNodes,
			Unmarshaller<? extends Value> valueUnmarshaller,
			boolean garbageCollected) {

		this.store = store;
		this.hashingForKeys = hashingForKeys;
		this.hashingForNodes = hashingForNodes;
		this.valueUnmarshaller = valueUnmarshaller;
		this.garbageCollected = garbageCollected;
	}

	@Override
	public Optional<Value> get(Key key) throws NoSuchElementException {
		byte[] hashOfRoot = store.getRoot();
		if (hashOfRoot == null)
			return Optional.empty();

		try {
			byte[] hashedKey = hashingForKeys.hash(key);
			byte[] nibblesOfHashedKey = toNibbles(hashedKey);
			return Optional.of(getNodeFromHash(hashOfRoot, 0).get(nibblesOfHashedKey, 0));
		}
		catch (NoSuchElementException e) {
			return Optional.empty();
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "error while getting key from Patricia trie", e);
			throw InternalFailureException.of("error while getting key from Patricia trie", e);
		}
	}

	@Override
	public void put(Key key, Value value) {
		try {
			byte[] hashedKey = hashingForKeys.hash(key);
			byte[] nibblesOfHashedKey = toNibbles(hashedKey);

			AbstractNode newRoot;
			byte[] hashOfRoot = store.getRoot();
			if (hashOfRoot == null)
				// the trie was empty: a leaf node with the value becomes the new root of the trie
				newRoot = new Leaf(nibblesOfHashedKey, value.toByteArray()).putInStore();
			else {
				newRoot = getNodeFromHash(hashOfRoot, 0).put(nibblesOfHashedKey, 0, value);
				if (garbageCollected)
					store.remove(hashOfRoot);
			}

			store.setRoot(hashingForNodes.hash(newRoot));
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "error while putting key into Patricia trie", e);
			throw InternalFailureException.of("error while putting key into Patricia trie", e);
		}
	}

	@Override
	public byte[] getRoot() {
		return store.getRoot();
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
	private AbstractNode from(ObjectInputStream ois, final int cursor) throws IOException {
		byte kind = ois.readByte();

		if (kind == 0x00 || (kind & 0xf0) == 0x10) {
			int nodeHashSize = hashingForNodes.length();
			int sharedBytesLength = ois.available() - nodeHashSize + 1;
			byte[] sharedBytes = new byte[sharedBytesLength];
			sharedBytes[0] = kind;
			if (sharedBytesLength - 1 != ois.readNBytes(sharedBytes, 1, sharedBytesLength - 1))
				throw new IOException("nibbles length mismatch in an extension node of a Patricia trie");

			byte[] sharedNibbles = expandBytesIntoNibbles(sharedBytes, (byte) 0x00);
			byte[] next = ois.readAllBytes();

			return new Extension(sharedNibbles, next);
		}
		else if (kind == 0x04) {
			short selector = ois.readShort();
			int nodeHashSize = hashingForNodes.length();
			byte[][] children = new byte[16][];
			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if ((selector & bit) != 0) {
					children[pos] = new byte[nodeHashSize];
					if (nodeHashSize != ois.readNBytes(children[pos], 0, nodeHashSize))
						throw new IOException("hash length mismatch in Patricia node");
				}

			return new Branch(children);
		}
		else if (kind == 0x02 || (kind & 0xf0) == 0x30) {
			int expected;
			if (cursor % 2 == 0)
				expected = hashingForKeys.length() - cursor / 2 + 1;
			else
				expected = hashingForKeys.length() - cursor / 2;

			byte[] nibbles = new byte[expected];
			nibbles[0] = kind;
			if (expected - 1 != ois.readNBytes(nibbles, 1, expected - 1))
				throw new IOException("keyEnd length mismatch in a leaf node of a Patricia trie");

			byte[] keyEnd = expandBytesIntoNibbles(nibbles, (byte) 0x02);
			byte[] value = ois.readAllBytes();

			return new Leaf(keyEnd, value);
		}
		else
			throw new IOException("unexpected Patricia node kind: " + kind);
	}

	/**
	 * Yields the node whose hash is the given one.
	 * 
	 * @param hash the hash of the node to look up
	 * @param cursor the number of nibbles in the path from the root of the trie to the node;
	 *               this is needed in order to foresee the size of the leaves
	 * @return the node
	 * @throws NoSuchElementException if the store has no node with the given {@code hash}
	 * @throws IOException if the node could not be unmarshalled
	 */
	private AbstractNode getNodeFromHash(byte[] hash, int cursor) throws NoSuchElementException, IOException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(store.get(hash))))) {
			return from(ois, cursor);
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
		byte[] split = new byte[length * 2];
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
		byte[] result = new byte[1 + length / 2];

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

	private abstract class AbstractNode extends Node {

		/**
		 * Yields the value bound to the given key.
		 * It considers only the portion of the key starting at the {@code cursor}th nibble.
		 * 
		 * @param nibblesOfHashedKey the nibbles of the key to look up; only the 4 least significant bits
		 *                           of each element are significant; the 4 most significant bits must be
		 *                           constantly 0
		 * @param cursor the starting point of the significant portion of {@code nibblesOfHashedKey}
		 * @return the value
		 * @throws NoSuchElementException if there is not such value
		 * @throws ClassNotFoundException if some data could not be unmarshalled
		 * @throws IOException if some data could not be unmarshalled
		 */
		protected abstract Value get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException, ClassNotFoundException, IOException;

		/**
		 * Binds the given value to the given key.
		 * It considers only the portion of the key starting at the {@code cursor}th nibble.
		 * 
		 * @param nibblesOfHashedKey the nibbles of the key to look up; only the 4 least significant bits
		 *                           of each element are significant; the 4 most significant bits must be
		 *                           constantly 0
		 * @param cursor the starting point of the significant portion of {@code nibblesOfHashedKey}
		 * @param value the value
		 * @return the new node that replaced this in the trie; if the key was already bound to the same
		 *         value, then this node will coincide with this, that is, they have the same hash
		 * @throws ClassNotFoundException if some data could not be unmarshalled
		 * @throws IOException if some data could not be unmarshalled
		 */
		protected abstract AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws IOException, ClassNotFoundException;

		/*
		protected abstract int depth(int cursor) throws NoSuchElementException, ClassNotFoundException, IOException;

		protected AbstractNode check(AbstractNode original) throws NoSuchElementException, ClassNotFoundException, IOException {
			int d1 = depth(0);
			int d2 = original.depth(0);
			if (d1 != d2)
				throw new IllegalStateException("inconsistent trie heights before: " + d2 + " after: " + d1);

			return this;
		}
		*/

		protected final AbstractNode putInStore() throws IOException {
			// we bind it to its hash in the store
			store.put(hashingForNodes.hash(this), toByteArray());
			return this;
		}
	}

	/**
	 * A branch node of a Patricia trie.
	 */
	private class Branch extends AbstractNode {

		/**
		 * The hashes of the branching children of the node. If the nth child is missing,
		 * the array will hold null for it.
		 */
		private final byte[][] children;

		/**
		 * Builds a branch node of a Patricia trie.
		 * 
		 * @param children the hashes of the branching children of the node.
		 *                 If the nth child is missing the array will hold null for it
		 */
		private Branch(byte[][] children) {
			this.children = children;
		}

		/**
		 * Yields a bitmap that describes which children exist.
		 * 
		 * @return the bitmap
		 */
		private short selector() {
			short result = 0;

			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if (children[pos] != null)
					result |= bit;

			return result;
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			context.writeByte(0x04);
			context.writeShort(selector());

			for (byte[] child: children)
				if (child != null)
					context.write(child);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, final int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			if (cursor >= nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie nibblesOfHashedKey.length = " + nibblesOfHashedKey.length + ", cursor = " + cursor);

			byte selection = nibblesOfHashedKey[cursor];
			if (children[selection] == null)
				throw new NoSuchElementException("key not found in Patricia trie");

			return getNodeFromHash(children[selection], cursor + 1).get(nibblesOfHashedKey, cursor + 1);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, final int cursor, Value value) throws IOException, ClassNotFoundException {
			if (cursor >= nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			byte selection = nibblesOfHashedKey[cursor];
			AbstractNode child;

			if (children[selection] == null) {
				// there was no path for this selection: we attach a leaf with the remaining nibbles
				byte[] nibblesButFirst = new byte[nibblesOfHashedKey.length - cursor - 1];
				System.arraycopy(nibblesOfHashedKey, cursor + 1, nibblesButFirst, 0, nibblesButFirst.length);
				child = new Leaf(nibblesButFirst, value.toByteArray()).putInStore();
			}
			else {
				// there was already a path for this selection: we recur
				child = getNodeFromHash(children[selection], cursor + 1).put(nibblesOfHashedKey, cursor + 1, value);
				if (garbageCollected)
					store.remove(children[selection]);
			}

			byte[][] childrenCopy = children.clone();
			childrenCopy[selection] = hashingForNodes.hash(child);

			return new Branch(childrenCopy).putInStore();
		}

		/*
		@Override
		protected int depth(int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			int height = 0;
			for (byte[] child: children)
				if (child != null) {
					int d = getNodeFromHash(child, cursor + 1).depth(cursor + 1) + 1;
					if (height > 0 && height != d)
						throw new IllegalStateException(height + " vs " + d);

					height = d;
				}

			return height;
		}
		*/
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
			context.write(compactNibblesIntoBytes(sharedNibbles, (byte) 0x00, (byte) 0x01));
			context.write(next);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < sharedNibbles.length; cursor1++, cursor++)
				if (sharedNibbles[cursor1] != nibblesOfHashedKey[cursor])
					throw new NoSuchElementException("key not found in Patricia trie");

			if (cursor1 != sharedNibbles.length || cursor >= nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			return getNodeFromHash(next, cursor).get(nibblesOfHashedKey, cursor);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, final int cursor, Value value) throws IOException, ClassNotFoundException {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < sharedNibbles.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == sharedNibbles[lengthOfSharedPortion])
				 lengthOfSharedPortion++;

			int lengthOfDistinctPortion = sharedNibbles.length - lengthOfSharedPortion;

			if (lengthOfDistinctPortion == 0) {
				// we recur
				AbstractNode newNext = getNodeFromHash(next, sharedNibbles.length + cursor).put(nibblesOfHashedKey, sharedNibbles.length + cursor, value);
				if (garbageCollected)
					store.remove(next);

				return new Extension(sharedNibbles, hashingForNodes.hash(newNext)).putInStore();
			}
			else {
				byte[] sharedNibbles1 = new byte[sharedNibbles.length - lengthOfSharedPortion - 1];
				System.arraycopy(sharedNibbles, lengthOfSharedPortion + 1, sharedNibbles1, 0, sharedNibbles1.length);
				byte[] keyEnd2 = new byte[nibblesOfHashedKey.length - cursor - lengthOfSharedPortion - 1];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = sharedNibbles[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				byte[][] children = new byte[16][];

				byte[] hashOfChild1;
				if (sharedNibbles1.length == 0)
					hashOfChild1 = next;
				else
					hashOfChild1 = hashingForNodes.hash(new Extension(sharedNibbles1, next).putInStore());
					
				AbstractNode child2 = new Leaf(keyEnd2, value.toByteArray()).putInStore();
				children[selection1] = hashOfChild1;
				children[selection2] = hashingForNodes.hash(child2);
				AbstractNode branch = new Branch(children).putInStore();

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternatives
					byte[] sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(this.sharedNibbles, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hashingForNodes.hash(branch)).putInStore();
				}
				else
					// yield a branch node with two alternatives
					return branch;
			}
		}

		/*
		@Override
		protected int depth(int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			return sharedNibbles.length + getNodeFromHash(next, sharedNibbles.length + cursor).depth(sharedNibbles.length + cursor);
		}
		*/
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
			context.write(compactNibblesIntoBytes(keyEnd, (byte) 0x02, (byte) 0x03));
			context.write(value);
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < keyEnd.length; cursor1++, cursor++)
				if (keyEnd[cursor1] != nibblesOfHashedKey[cursor])
					throw new NoSuchElementException("key not found in Patricia trie");

			if (cursor1 != keyEnd.length || cursor != nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie: " + (cursor1 != keyEnd.length) + ", " + (cursor != nibblesOfHashedKey.length));

			try (UnmarshallingContext context = new UnmarshallingContext(new ByteArrayInputStream(value))) {
				return valueUnmarshaller.from(context);
			}
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws IOException {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < keyEnd.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == keyEnd[lengthOfSharedPortion])
				 lengthOfSharedPortion++;

			int lengthOfDistinctPortion = keyEnd.length - lengthOfSharedPortion;

			if (lengthOfDistinctPortion == 0)
				// the keys coincide
				return new Leaf(keyEnd, value.toByteArray()).putInStore();
			else {
				// since there is a distinct portion, there must be at least a nibble in keyEnd
				byte[] keyEnd1 = new byte[keyEnd.length - lengthOfSharedPortion - 1];
				System.arraycopy(keyEnd, lengthOfSharedPortion + 1, keyEnd1, 0, keyEnd1.length);
				byte[] keyEnd2 = new byte[keyEnd1.length];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = keyEnd[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				byte[][] children = new byte[16][];
				AbstractNode leaf1 = new Leaf(keyEnd1, this.value).putInStore();
				AbstractNode leaf2 = new Leaf(keyEnd2, value.toByteArray()).putInStore();
				children[selection1] = hashingForNodes.hash(leaf1);
				children[selection2] = hashingForNodes.hash(leaf2);
				AbstractNode branch = new Branch(children).putInStore();

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternatives leaves
					byte[] sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(keyEnd, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hashingForNodes.hash(branch)).putInStore();
				}
				else
					// yield a branch node with two alternatives leaves
					return branch;
			}
		}

		/*
		@Override
		protected int depth(int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			return keyEnd.length;
		}
		*/
	}
}