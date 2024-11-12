package com.google.gwt.dom.client;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;

/**
 * <p>
 * Models the identity of a DOM node (attached to root) with an integer id
 * value. See {@link AttachIds} for how these ids are populated.
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
public final class AttachId {
	public static AttachId forNode(Node node) {
		if (node == null) {
			return null;
		}
		AttachId result = new AttachId();
		result.id = node.getAttachId();
		return result;
	}

	public int id;

	public AttachId() {
	}

	AttachId(int id) {
		this.id = id;
	}

	public Node node() {
		return Document.get().implAccess().getNode(this);
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	@Property.Not
	public boolean isDetached() {
		return id <= 0;
	}
}
