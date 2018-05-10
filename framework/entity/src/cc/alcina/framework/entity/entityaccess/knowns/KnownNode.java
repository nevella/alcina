package cc.alcina.framework.entity.entityaccess.knowns;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public abstract class KnownNode {
	transient KnownNodePersistent persistent;

	transient KnownNode parent;

	transient String name;

	public KnownNode(KnownNode parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public void persist() {
		Knowns.reconcile(this, false);
	}

	public void restore() {
		Knowns.reconcile(this, true);
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
}
