/*
 * Copyright 2005-2012 Roger Kapsi, Sam Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.util.trie;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A {@link Cursor} can be used to traverse a {@link Trie}, visit each node step
 * by step and make {@link Decision}s on each step how to continue with
 * traversing the {@link Trie}.
 */
public interface Cursor<K, V> {
	/**
	 * Called for each {@link Entry} in the {@link Trie}. Return
	 * {@link Decision#EXIT} to finish the {@link Trie} operation,
	 * {@link Decision#CONTINUE} to go to the next {@link Entry},
	 * {@link Decision#REMOVE} to remove the {@link Entry} and continue
	 * iterating or {@link Decision#REMOVE_AND_EXIT} to remove the {@link Entry}
	 * and stop iterating.
	 * 
	 * Note: Not all operations support {@link Decision#REMOVE}.
	 */
	public Decision select(Map.Entry<? extends K, ? extends V> entry);

	/**
	 * The {@link Decision} tells the {@link Cursor} what to do on each step
	 * while traversing the {@link Trie}.
	 * 
	 * NOTE: Not all operations that work with a {@link Cursor} support all
	 * {@link Decision} types
	 */
	public static enum Decision {
		/**
		 * Exit the traverse operation
		 */
		EXIT,
		/**
		 * Continue with the traverse operation
		 */
		CONTINUE,
		/**
		 * Remove the previously returned element from the {@link Trie} and
		 * continue
		 */
		REMOVE,
		/**
		 * Remove the previously returned element from the {@link Trie} and exit
		 * from the traverse operation
		 */
		REMOVE_AND_EXIT;
	}
}