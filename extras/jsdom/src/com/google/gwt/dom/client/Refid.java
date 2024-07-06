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
import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * Models the identity of a DOM node (attached to root) with an integer id
 * value. See {@link DomIds} for how these ids are populated.
 * 
 * <p>
 * It also contains accessor methods to access the referenced DOM node. It's the
 * interchange/synchronization term used in communication between the server and
 * client DOMs
 * 
 *
 * 
 */
@Bean(PropertySource.FIELDS)
public class Refid {
	private static transient Map<Node, Refid> preRemovalPaths;

	public static Refid forNode(Node node) {
		if (preRemovalPaths != null && preRemovalPaths.containsKey(node)) {
			return preRemovalPaths.remove(node);
		}
		Refid result = new Refid();
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
		result.id = node.getRefId();
		return result;
	}

	// FIXME - pathref - remove
	public static void onPreRemove(Node node) {
		if (preRemovalPaths == null) {
			synchronized (Refid.class) {
				if (preRemovalPaths == null) {
					preRemovalPaths = Collections
							.synchronizedMap(AlcinaCollections.newWeakMap());
				}
			}
		}
		preRemovalPaths.put(node, forNode(node));
	}

	// FIXME - refser - final
	public String path;

	public int id;

	public Refid() {
	}

	Refid(String path, int id) {
		this.path = path;
		this.id = id;
	}

	public Refid append(int ordinal, int id) {
		return new Refid(path + "." + ordinal, id);
	}

	public List<Integer> childOrdinals() {
		List<Integer> ordinals = Arrays.stream(path.split("\\."))
				.map(Integer::parseInt).collect(Collectors.toList());
		ordinals.remove(0);
		return ordinals;
	}

	public Node node() {
		return Document.get().implAccess().getNode(this);
	}

	@Override
	public String toString() {
		return Ax.format("%s/%s", path, id);
	}
}
