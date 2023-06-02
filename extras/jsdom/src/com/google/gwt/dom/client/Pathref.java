package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.AlcinaCollections;

/**
 * Models a reference to a DOM node (attached to root) via its path
 * 
 * @author nick@alcina.cc
 *
 */
@Bean(PropertySource.FIELDS)
public class Pathref {
	private static transient Map<Node, Pathref> preRemovalPaths;

	public static Pathref forNode(Node node) {
		if (preRemovalPaths != null && preRemovalPaths.containsKey(node)) {
			return preRemovalPaths.remove(node);
		}
		Pathref result = new Pathref();
		Node cursor = node;
		List<Integer> ordinals = new ArrayList<>();
		do {
			Element parentElement = cursor.getParentElement();
			int ordinal = parentElement == null ? 0
					: parentElement.getChildIndexLocal(cursor);
			ordinals.add(ordinal);
			cursor = cursor.getParentElement();
		} while (cursor != null);
		Collections.reverse(ordinals);
		result.path = ordinals.stream().map(String::valueOf)
				.collect(Collectors.joining("."));
		return result;
	}

	public static void onPreRemove(Node node) {
		if (preRemovalPaths == null) {
			synchronized (Pathref.class) {
				if (preRemovalPaths == null) {
					preRemovalPaths = AlcinaCollections.newWeakMap();
				}
			}
		}
		preRemovalPaths.put(node, forNode(node));
	}

	// FIXME - refser - final
	public String path;

	public Pathref() {
	}

	public Pathref(String path) {
		this.path = path;
	}

	public Pathref append(int ordinal) {
		return new Pathref(path + "." + ordinal);
	}

	public List<Integer> childOrdinals() {
		List<Integer> ordinals = Arrays.stream(path.split("\\."))
				.map(Integer::parseInt).collect(Collectors.toList());
		ordinals.remove(0);
		return ordinals;
	}

	public Node node() {
		return Document.get().getDocumentElement().implAccess().local()
				.queryRelativePath(this).node();
	}

	@Override
	public String toString() {
		return path;
	}
}
