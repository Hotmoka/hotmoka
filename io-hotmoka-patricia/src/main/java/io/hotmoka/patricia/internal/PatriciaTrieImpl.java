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
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueUnmarshaller a function able to unmarshall a value from its byte representation
	 */
	public PatriciaTrieImpl(KeyValueStore store,
			HashingAlgorithm<? super Key> hashingForKeys, HashingAlgorithm<? super Node> hashingForNodes,
			Unmarshaller<? extends Value> valueUnmarshaller) {

		this.store = store;
		this.hashingForKeys = hashingForKeys;
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
			return getNodeFromHash(hashOfRoot).get(nibblesOfHashedKey, 0);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void put(Key key, Value value) {
		try {
			byte[] hashedKey = hashingForKeys.hash(key);
			byte[] nibblesOfHashedKey = toNibbles(hashedKey);

			Node newRoot;
			byte[] hashOfRoot = store.getRoot();
			if (hashOfRoot == null)
				// the trie was empty: a leaf node with the value becomes the new root of the trie
				newRoot = new Leaf(nibblesOfHashedKey, value.toByteArray());
			else
				newRoot = getNodeFromHash(hashOfRoot).put(nibblesOfHashedKey, 0, value);

			store.setRoot(hashingForNodes.hash(newRoot));
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
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
			int valueLength = ois.readInt();
			byte[] value = new byte[valueLength];
			if (valueLength != ois.readNBytes(value, 0, valueLength))
				throw new IOException("value length mismatch in a leaf node of a Patricia trie");

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
	 * @throws IOException if the node could not be unmarshalled
	 * @throws ClassNotFoundException if the node could not be unmarshalled
	 */
	private AbstractNode getNodeFromHash(byte[] hash) throws NoSuchElementException, ClassNotFoundException, IOException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(store.get(hash))))) {
			return from(ois);
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
		 * @param the value
		 * @return the new node that replaced this in the trie; if the key was already bound to the same
		 *         value, then this node will coincide with this, that is, they have the same hash
		 * @throws ClassNotFoundException if some data could not be unmarshalled
		 * @throws IOException if some data could not be unmarshalled
		 */
		protected abstract AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws IOException, ClassNotFoundException;
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
		 * @throws IOException if this node cannot be marshalled in store
		 */
		public Branch(byte[][] children) throws IOException {
			this.children = children;
			// we bind it to its hash in the store
			store.put(hashingForNodes.hash(this), toByteArray());
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
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			if (cursor >= nibblesOfHashedKey.length - 1)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			byte selection = nibblesOfHashedKey[cursor];
			if (children[selection] == null)
				throw new NoSuchElementException("key not found in Patricia trie");

			return getNodeFromHash(children[selection]).get(nibblesOfHashedKey, cursor + 1);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws IOException, ClassNotFoundException {
			if (cursor >= nibblesOfHashedKey.length - 1)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			byte selection = nibblesOfHashedKey[cursor];
			AbstractNode child;

			if (children[selection] == null) {
				// there was no path for this selection: we attach a leaf with the remaining nibbles
				byte[] nibblesButFirst = new byte[nibblesOfHashedKey.length - cursor - 1];
				System.arraycopy(nibblesOfHashedKey, cursor + 1, nibblesButFirst, 0, nibblesButFirst.length);
				child = new Leaf(nibblesButFirst, value.toByteArray());
			}
			else
				// there was already a path for this selection: we recur
				child = getNodeFromHash(children[selection]).put(nibblesOfHashedKey, cursor + 1, value);


			byte[][] childrenCopy = children.clone();
			childrenCopy[selection] = hashingForNodes.hash(child);

			return new Branch(childrenCopy);
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
		 * @throws IOException if this node cannot be marshalled in store
		 */
		public Extension(byte[] sharedNibbles, byte[] next) throws IOException {
			this.sharedNibbles = sharedNibbles;
			this.next = next;
			// we bind it to its hash in the store
			store.put(hashingForNodes.hash(this), toByteArray());
		}

		@Override
		public void into(ObjectOutputStream oos) throws IOException {
			oos.writeByte(SELECTOR);
			oos.write(next);
			oos.write(sharedNibbles); // TODO: compaction
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < sharedNibbles.length; cursor1++, cursor++)
				if (sharedNibbles[cursor1] != nibblesOfHashedKey[cursor])
					throw new NoSuchElementException("key not found in Patricia trie");

			if (cursor1 != sharedNibbles.length || cursor == nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			return getNodeFromHash(next).get(nibblesOfHashedKey, cursor);
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws IOException, ClassNotFoundException {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < sharedNibbles.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == nibblesOfHashedKey[lengthOfSharedPortion])
				 lengthOfSharedPortion++;

			int lengthOfDistinctPortion = sharedNibbles.length - lengthOfSharedPortion;

			if (lengthOfDistinctPortion == 0) {
				// we recur
				AbstractNode newNext = getNodeFromHash(next).put(nibblesOfHashedKey, sharedNibbles.length + cursor, value);
				return new Extension(sharedNibbles, newNext.toByteArray());
			}
			else {
				byte[] sharedNibbles1 = new byte[sharedNibbles.length - lengthOfSharedPortion - 1];
				System.arraycopy(sharedNibbles, lengthOfSharedPortion + 1, sharedNibbles1, 0, sharedNibbles1.length);
				byte[] keyEnd2 = new byte[sharedNibbles1.length];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = sharedNibbles[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				byte[][] children = new byte[16][];

				AbstractNode child1;
				if (sharedNibbles1.length == 0)
					child1 = getNodeFromHash(next); //TODO: avoid recomputation
				else
					child1 = new Extension(sharedNibbles1, next);
					
				AbstractNode child2 = new Leaf(keyEnd2, value.toByteArray());
				children[selection1] = hashingForNodes.hash(child1);
				children[selection2] = hashingForNodes.hash(child2);
				Branch branch = new Branch(children);

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternatives
					byte[] sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(this.sharedNibbles, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hashingForNodes.hash(branch));
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
	public class Leaf extends AbstractNode {
		public final static byte SELECTOR = 2;

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
		 * @throws IOException if this node cannot be marshalled in store
		 */
		public Leaf(byte[] keyEnd, byte[] value) throws IOException {
			this.keyEnd = keyEnd;
			this.value = value;
			// we bind it to its hash in the store
			store.put(hashingForNodes.hash(this), toByteArray());
		}

		@Override
		public void into(ObjectOutputStream oos) throws IOException {
			oos.writeByte(SELECTOR);
			oos.writeInt(value.length);
			oos.write(value);
			oos.write(keyEnd); // TODO: compaction
		}

		@Override
		protected Value get(byte[] nibblesOfHashedKey, int cursor) throws NoSuchElementException, ClassNotFoundException, IOException {
			int cursor1;
			for (cursor1 = 0; cursor < nibblesOfHashedKey.length && cursor1 < keyEnd.length; cursor1++, cursor++)
				if (keyEnd[cursor1] != nibblesOfHashedKey[cursor])
					throw new NoSuchElementException("key not found in Patricia trie");

			if (cursor1 != keyEnd.length || cursor != nibblesOfHashedKey.length)
				throw new InternalFailureException("inconsistent key length in Patricia trie");

			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(value)))) {
				return valueUnmarshaller.from(ois);
			}
		}

		@Override
		protected AbstractNode put(byte[] nibblesOfHashedKey, int cursor, Value value) throws IOException, ClassNotFoundException {
			int lengthOfSharedPortion = 0;

			while (lengthOfSharedPortion < keyEnd.length && nibblesOfHashedKey[lengthOfSharedPortion + cursor] == keyEnd[lengthOfSharedPortion])
				 lengthOfSharedPortion++;

			int lengthOfDistinctPortion = keyEnd.length - lengthOfSharedPortion;

			if (lengthOfDistinctPortion == 0)
				// the keys coincide
				return new Leaf(keyEnd, value.toByteArray());
			else {
				// since there is a distinct portion, there must be at least a nibble in keyEnd
				byte[] keyEnd1 = new byte[keyEnd.length - lengthOfSharedPortion - 1];
				System.arraycopy(keyEnd, lengthOfSharedPortion + 1, keyEnd1, 0, keyEnd1.length);
				byte[] keyEnd2 = new byte[keyEnd1.length];
				System.arraycopy(nibblesOfHashedKey, lengthOfSharedPortion + cursor + 1, keyEnd2, 0, keyEnd2.length);
				byte selection1 = keyEnd[lengthOfSharedPortion];
				byte selection2 = nibblesOfHashedKey[lengthOfSharedPortion + cursor];
				byte[][] children = new byte[16][];
				Leaf leaf1 = new Leaf(keyEnd1, this.value);
				Leaf leaf2 = new Leaf(keyEnd2, value.toByteArray());
				children[selection1] = hashingForNodes.hash(leaf1);
				children[selection2] = hashingForNodes.hash(leaf2);
				Branch branch = new Branch(children);

				if (lengthOfSharedPortion > 0) {
					// yield an extension node linked to a branch node with two alternatives leaves
					byte[] sharedNibbles = new byte[lengthOfSharedPortion];
					System.arraycopy(keyEnd, 0, sharedNibbles, 0, lengthOfSharedPortion);
					return new Extension(sharedNibbles, hashingForNodes.hash(branch));
				}
				else
					// yield a branch node with two alternatives leaves
					return branch;
			}
		}
	}
}