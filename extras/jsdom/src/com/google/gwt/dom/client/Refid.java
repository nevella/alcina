package com.google.gwt.dom.client;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

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
	public static Refid forNode(Node node) {
		Refid result = new Refid();
		result.id = node.getRefId();
		return result;
	}

	public int id;

	public Refid() {
	}

	Refid(int id) {
		this.id = id;
	}

	public Node node() {
		return Document.get().implAccess().getNode(this);
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
