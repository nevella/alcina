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
 * Models a reference to a DOM node (attached to root) via its path
 * 
 * <h3>Plans</h3>
 * <ul>
 * <li>Switch to *mostly* numeric node id, not pathref.
 * <li>Pathref is still required for cross-session node referencing (viz
 * romcom/tab reload)
 * <li>Move element.local-id attr to a dom node expando (out of the attrs) -
 * value should be an arr containing the (int/Number) id
 * <li>Mutations?
 * <li>Romcom - propagate mutation html add/removes as single node changes
 * 
 * </ul>
 * <h3>Implementations
 * <h3>
 * <ul>
 * <li>Node refid is generated on node attach, normally from the local dom's
 * counter (even for server, odd for browser)
 * <li>When propagating from one to the other,
 * </ul>
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
		result.id = node.getRefId();
		return result;
	}

	// FIXME - pathref - remove
	public static void onPreRemove(Node node) {
		if (preRemovalPaths == null) {
			synchronized (Pathref.class) {
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

	public Pathref() {
	}

	Pathref(String path, int id) {
		this.path = path;
		this.id = id;
	}

	public Pathref append(int ordinal, int id) {
		return new Pathref(path + "." + ordinal, id);
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
