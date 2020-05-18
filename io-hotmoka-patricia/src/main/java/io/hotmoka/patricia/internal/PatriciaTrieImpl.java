package io.hotmoka.patricia.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.NoSuchElementException;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.patricia.HashingAlgorithm;
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
	 * The hashing algorithm for the values of the trie.
	 */
	private final HashingAlgorithm<? super Value> hashingForValues;

	/**
	 * The hashing algorithm for the nodes of the trie.
	 */
	private final HashingAlgorithm<? super Node> hashingForNodes;

	/**
	 * A function able to unmarshall a value from its byte representation.
	 */
	private final Unmarshaller<? extends Value> valueUnmarshaller;

	/**
	 * Creates a new Merkle-Patricia trie supported by the underlying store,
	 * using the given hashing algorithm to hash nodes and values.
	 * 
	 * @param store the store used to store a mapping from nodes' hashes to the marshalled
	 *              representation of the nodes
	 * @param hashingForKeys the hashing algorithm for the keys
	 * @param hashingForValues the hashing algorithm for the values
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueUnmarshaller a function able to unmarshall a value from its byte representation
	 */
	public PatriciaTrieImpl(KeyValueStore store,
			HashingAlgorithm<? super Key> hashingForKeys, HashingAlgorithm<? super Value> hashingForValues, HashingAlgorithm<? super Node> hashingForNodes,
			Unmarshaller<? extends Value> valueUnmarshaller) {

		this.store = store;
		this.hashingForKeys = hashingForKeys;
		this.hashingForValues = hashingForValues;
		this.hashingForNodes = hashingForNodes;
		this.valueUnmarshaller = valueUnmarshaller;
	}

	@Override
	public Value get(Key key) throws NoSuchElementException {
		byte[] hashOfRoot = store.getRoot();
		if (hashOfRoot == null)
			throw new NoSuchElementException("no " + key + " in Patricia trie");

		try {
			byte[] hashedKey = hashingForKeys.hash(key);
			byte[] nibblesOfHashedKey = toNibbles(hashedKey);
			byte[] bytesOfValue = getNodeFromHash(hashOfRoot).get(nibblesOfHashedKey, 0);

			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(bytesOfValue)))) {
				return valueUnmarshaller.from(ois);
			}
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void put(Key key, Value value) {
		// TODO Auto-generated method stub
	}

	/**
	 * Factory method that unmarshals a node from the given stream.
	 * 
	 * @param ois the stream
	 * @return the node
	 * @throws IOException if the node could not be unmarshalled
	 * @throws ClassNotFoundException if the node could not be unmarshalled
	 */
	private AbstractNode from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte kind = ois.readByte();
		int nodeHashSize = hashingForNodes.length();

		if (kind == Extension.SELECTOR) {
			byte[] value = new byte[nodeHashSize];
			if (nodeHashSize != ois.readNBytes(value, 0, nodeHashSize))
				throw new IOException("hash length mismatch in an extension node of a Patricia trie");

			byte[] sharedNibbles = ois.readAllBytes(); // TODO: expand

			return new Extension(sharedNibbles, value);
		}
		else if (kind == Branch.SELECTOR) {
			short selector = ois.readShort();
			byte[][] children = new byte[16][];
			for (int pos = 0, bit = 0x8000; pos < 16; pos++, bit >>= 1)
				if ((selector & bit) != 0) {
					children[pos] = new byte[nodeHashSize];
					if (nodeHashSize != ois.readNBytes(children[pos], 0, nodeHashSize))
						throw new IOException("hash length mismatch in Patricia node");
				}

			return new Branch(children);
		}
		else if (kind == Leaf.SELECTOR) {
			int valueHashSize = hashingForValues.length();
			byte[] value = new byte[valueHashSize];
			if (valueHashSize != ois.readNBytes(value, 0, valueHashSize))
				throw new IOException("hash length mismatch in a leaf node of a Patricia trie");

			byte[] keyEnd = ois.readAllBytes(); // TODO: expand

			return new Leaf(keyEnd, value);
		}
		else
			throw new IOException("unexpected Patricia node kind: " + kind);
	}

	/**
	 * Yields the node whose hash is the given one.
	 * 
	 * @param hash the hash of the node to look up
	 * @return the node
	 * @throws NoSuchElementException if the store has no node with the given {@code hash}
	 */
	private AbstractNode getNodeFromHash(byte[] hash) throws NoSuchElementException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(store.get(hash))))) {
			return from(ois);
		}
		catch (NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
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

	private abstract class AbstractNode extends Node {

		/**
		 * Yields the value, as a byte sequence, bound to the given key.
		 * It considers only the portion of the key starting at the {@code cursor}th nibble.
		 * 
		 * @param nibblesOfHashedKey the nibbles of the key to look up; only the 4 least significant bits
		 *                           of each element are significant; the 4 most significant bits must be
		 *                           constantly 0
		 * @param cursor the starting point of the significant portion of {@code nibblesOfHashedKey}
		 * @return the value, as a sequence of bytes
		 * @throws NoSuchElementException if there is not such value
		 */
		protected abstract byte[] get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException;
	}

	/**
	 * A branch node of a Patricia trie.
	 */
	public class Branch extends AbstractNode {
		public final static byte SELECTOR = 1;

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
		public Branch(byte[][] children) {
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
		public void into(ObjectOutputStream oos) throws IOException {
			oos.writeByte(SELECTOR);
			oos.writeShort(selector());

			for (byte[] child: children)
				if (child != null)
					oos.write(child);
		}

		@Override
		protected byte[] get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException {
			if (cursor >= nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			byte selection = nibblesOfHashedKey[cursor];
			if (children[selection] == null)
				throw new NoSuchElementException("key not found in Patricia trie");

			return getNodeFromHash(children[selection]).get(nibblesOfHashedKey, cursor + 1);
		}
	}

	/**
	 * An extension node of a Patricia trie.
	 */
	public class Extension extends AbstractNode {
		public final static byte SELECTOR = 0;

		/**
		 * The prefix nibbles shared among all paths passing through this node.
		 * Each byte uses only its least significant 4 bits (a nibble).
		 * Its 4 most significant bits are constantly set to 0.
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
		 *                      It 4 most significant bits are constantly set to 0
		 * @param next the hash of the next node, the only child of the extension node
		 */
		public Extension(byte[] sharedNibbles, byte[] next) {
			this.sharedNibbles = sharedNibbles;
			this.next = next;
		}

		@Override
		public void into(ObjectOutputStream oos) throws IOException {
			oos.writeByte(SELECTOR);
			oos.write(next);
			oos.write(sharedNibbles); // TODO: compaction
		}

		@Override
		protected byte[] get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < sharedNibbles.length; cursor1++, cursor++)
				if (sharedNibbles[cursor1] != nibblesOfHashedKey[cursor])
					throw new NoSuchElementException("key not found in Patricia trie");

			if (cursor1 != sharedNibbles.length || cursor == nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			return getNodeFromHash(next).get(nibblesOfHashedKey, cursor);
		}
	}

	/**
	 * A leaf node of a Patricia trie.
	 */
	public class Leaf extends AbstractNode {
		public final static byte SELECTOR = 2;

		/**
		 * The key end of the only path passing through this node.
		 * Each byte uses only its least significant 4 bits (a nibble).
		 * Its 4 most significant bits are constantly set to 0.
		 */
		private final byte[] keyEnd;

		/**
		 * The hash of the value bound to the key leading to this node.
		 */
		private final byte[] value;

		/**
		 * Builds an extension node of a Patricia trie.
		 * 
		 * @param keyEnd the key end of the only path passing through this node.
		 *               Each byte uses only its least significant 4 bits (a nibble).
		 *               Its 4 most significant bits are constantly set to 0
		 * @param value the hash of the value bound to the key leading to this node
		 */
		public Leaf(byte[] keyEnd, byte[] value) {
			this.keyEnd = keyEnd;
			this.value = value;
		}

		@Override
		public void into(ObjectOutputStream oos) throws IOException {
			oos.writeByte(SELECTOR);
			oos.write(value);
			oos.write(keyEnd); // TODO: compaction
		}

		@Override
		protected byte[] get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < keyEnd.length; cursor1++, cursor++)
				if (keyEnd[cursor1] != nibblesOfHashedKey[cursor])
					throw new NoSuchElementException("key not found in Patricia trie");

			if (cursor1 != keyEnd.length || cursor != nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			return value;
		}
	}
}