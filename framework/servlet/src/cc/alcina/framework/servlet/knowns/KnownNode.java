package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.entity.entityaccess.KnownNodePersistent;

public abstract class KnownNode {
	public transient KnownNodePersistent persistent;

	public transient KnownNode parent;

	public transient String name;

	public transient KnownsPersistence persistence;

	private transient KnownRenderableNode renderableNode;

	public KnownNode(KnownNode parent, String name) {
		this(parent.persistence, parent, name);
	}

	public KnownNode(KnownsPersistence persistence, KnownNode parent,
			String name) {
		this.persistence = persistence;
		this.parent = parent;
		this.name = name;
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

	public KnownRenderableNode getRenderableNode() {
		return renderableNode;
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

	public void refresh() {
		restore();
	}

	public void restore() {
		Knowns.reconcile(this, true);
	}

	public void setRenderableNode(KnownRenderableNode renderableNode) {
		if (this.renderableNode != null && this.renderableNode.field != null) {
			renderableNode.field = this.renderableNode.field;
		}
		this.renderableNode = renderableNode;
	}
}
