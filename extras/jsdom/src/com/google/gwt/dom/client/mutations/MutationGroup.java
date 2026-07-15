package com.google.gwt.dom.client.mutations;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * <p>
 * A mutation is assigned to a mutationgroup (there are at least two mutations
 * per group) if the cumulative effect of the mutations is no modification to
 * the character sequence of the document
 * 
 * <p>
 * Examples are splitting a text node and wrapping any node. The primary use
 * case is to preserve the text index for Location instances, even when they're
 * within the text node being mutated; this extra modelling also allows
 * preservation of attachIds (and elides unbind/bind cycles) for wrapped nodes.
 * <p>
 * Note that {@link #merge} is unused - since the post-split locations will be
 * treeindex-mutated by split, there's no advantage to a merge (and no current
 * {@link DomNode} operation to support it )
 * <p>
 * Split requires no special attach/detach handling - where strip and wrap do
 * (see {@link Document#getWillReattach} )
 */
@Reflected
public enum MutationGroup {
	split {
		@Override
		public IntPair mutationTuple() {
			return IntPair.of(1, 0);
		}
	},
	wrap {
		@Override
		public IntPair mutationTuple() {
			return IntPair.of(1, 0);
		}
	},
	merge {
		@Override
		public IntPair mutationTuple() {
			throw new UnsupportedOperationException();
		}
	},
	strip {
		@Override
		public IntPair mutationTuple() {
			return IntPair.of(-1, 0);
		}
	};

	/*
	 * sugar to set the special mutation mode of the containing document's
	 * localdom
	 */
	public static class MutationGroups {
		int mutationGroupIndex;

		MutationGroup mutationGroup;

		public void exit() {
			mutationGroup = null;
		}

		public void enterWrap() {
			mutationGroup = wrap;
			mutationGroupIndex++;
		}

		public void enterStrip() {
			mutationGroup = strip;
			mutationGroupIndex++;
		}

		public void enterSplit() {
			mutationGroup = split;
			mutationGroupIndex++;
		}

		public MutationGroup getActiveGroup() {
			return mutationGroup;
		}

		public int getActiveGroupIndex() {
			return mutationGroup == null ? 0 : mutationGroupIndex;
		}
	}

	public abstract IntPair mutationTuple();
}
