package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

/**
 * Models a reference to a DOM node (attached to root) via its path
 * 
 * @author nick@alcina.cc
 *
 */
@Bean(PropertySource.FIELDS)
public class Pathref {
	public static Pathref from(Node node) {
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

	@Override
	public String toString() {
		return path;
	}
}
