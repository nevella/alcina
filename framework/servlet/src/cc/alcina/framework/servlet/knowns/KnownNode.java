package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.entity.entityaccess.KnownNodePersistent;
import cc.alcina.framework.entity.entityaccess.KnownNodePersistentDomainStore;

public abstract class KnownNode {
	public transient KnownNodePersistent persistent;

	public transient KnownNode parent;

	public transient String name;
	
	public transient KnownsPersistence persistence;
	
	public transient KnownRenderableNode renderableNode;

	public KnownNode(KnownsPersistence persistence,KnownNode parent, String name) {
		this.persistence = persistence;
		this.parent = parent;
		this.name = name;
	}
	public KnownNode(KnownNode parent, String name) {
		this(parent.persistence,parent,name);
	}

	public <T extends KnownNode> T forName(Object key) {
		try {
			Field field = getClass()
					.getDeclaredField(key.toString().toLowerCase());
			field.setAccessible(true);
			return (T) field.get(this);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public String path() {
		KnownNode cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(cursor.name);
			cursor = cursor.parent;
		}
		Collections.reverse(segments);
		return segments.stream().collect(Collectors.joining("/"));
	}

	public void persist() {
		Knowns.reconcile(this, false);
	}

	public void restore() {
		Knowns.reconcile(this, true);
	}
}
