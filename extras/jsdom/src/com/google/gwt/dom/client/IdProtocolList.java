package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;

/*
 * Note that this list may not be purely ids, since some extra information
 * (empty text nodes, repeated text nodes) may need to be encoded
 * 
 * TODO - some unit tests
 */
@Bean(PropertySource.FIELDS)
public class IdProtocolList {
	static int PROTOCOL_0_SPECIAL = 0;

	/*
	 * [0,0,attachId,parentId,previousSiblingId]
	 */
	static int PROTOCOL_1_TEXT_BLANK_NON_SEQUENCE = 0;

	/*
	 * [0,1,n,attachIds,lengths](the last two will be int lists of length n)
	 */
	static int PROTOCOL_1_TEXT_NODE_SEQUENCE = 1;

	@Override
	public String toString() {
		return ids.toString();
	}

	public List<Integer> ids = new ArrayList<>();

	public int[] toIntArray() {
		int[] result = new int[ids.size()];
		for (int idx = 0; idx < result.length; idx++) {
			result[idx] = ids.get(idx);
		}
		return result;
	}

	/*
	 * Models nodes {t1,...tn} of a sequence {t0...tn} - they will be output as
	 * a single node in the markup
	 */
	class ConsecutiveTextNodes {
		List<Integer> attachIds = new ArrayList<>();

		List<Integer> lengths = new ArrayList<>();

		/*
		 * the first text, it must be non-empty (so it will have a markup
		 * representation)
		 */
		Text first;

		/*
		 * the most recent node added to this instance
		 */
		Text cursor;

		/*
		 * Add a text to this sequence. The comprised texts are a sibling
		 * sequence;
		 */
		boolean addText(Text text) {
			if (cursor == null) {
				return false;
			}
			if (text.getPreviousSibling() != cursor) {
				flush(text);
				return false;
			}
			cursor = text;
			attachIds.add(text.getAttachId());
			lengths.add(text.getLength());
			return true;
		}

		void flush(Node node) {
			if (attachIds.size() > 0) {
				add(PROTOCOL_0_SPECIAL);
				add(PROTOCOL_1_TEXT_NODE_SEQUENCE);
				add(attachIds.size());
				attachIds.forEach(id -> add(id));
				lengths.forEach(id -> add(id));
				attachIds.clear();
				lengths.clear();
			}
			if (node.getNodeType() == Node.TEXT_NODE) {
				first = (Text) node;
			} else {
				first = null;
			}
			cursor = first;
		}
	}

	/**
	 * <p>
	 * This version never does not run against the browser Node subtree created
	 * from an HTML string (there's a js version that does).
	 * 
	 * <p>
	 * That version is responsible for ensuring the browser model matches the
	 * source model - merging text nodes if they were created by the legacy
	 * KHTML/Webkit expansion of large text nodes
	 * 
	 * <p>
	 * The generated list matches the idprotocolspec (above)
	 * 
	 * <p>
	 * The three special cases are:
	 * <ul>
	 * <li>non-empty text node with non-empty textnode previous sibling
	 * <li>empty text node, not-first child (previous-sibling of any type)
	 * <li>empty text node, first child
	 * </ul>
	 * 
	 */
	static IdProtocolList of(Node subtreeRoot) {
		IdProtocolList list = new IdProtocolList();
		list.fromSubtree(subtreeRoot);
		return list;
	}

	@Property.Not
	ConsecutiveTextNodes consecutiveTextNodes = new ConsecutiveTextNodes();

	void fromSubtree(Node subtreeRoot) {
		List<Node> depthFirstNodes = subtreeRoot.traverse().toList();
		Text lastNonemptyEmittedText = null;
		for (int idx = 0; idx < depthFirstNodes.size(); idx++) {
			Node node = depthFirstNodes.get(idx);
			boolean special = false;
			if (node.provideIsText()) {
				Text text = (Text) node;
				if (consecutiveTextNodes.addText(text)) {
					continue;
				}
				String content = text.getNodeValue();
				if (content.length() > 0) {
					//
				} else {
					special = true;
					add(PROTOCOL_0_SPECIAL);
					add(PROTOCOL_1_TEXT_BLANK_NON_SEQUENCE);
					add(node.attachId);
					add(node.getParentNode().attachId);
					add(node.getPreviousSibling() == null ? 0
							: node.getPreviousSibling().attachId);
				}
			}
			if (!special) {
				consecutiveTextNodes.flush(node);
				add(node.attachId);
			}
		}
	}

	void add(int value) {
		ids.add(value);
	}
}