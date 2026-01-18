package cc.alcina.framework.common.client.reflection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Intersection;
import cc.alcina.framework.common.client.util.Topic;

/**
 * Enables listening to a graph (generally a tree) of object/property changes,
 * with customised listening. Think React Store
 * 
 * Note that binding cannot be circular...future will be rules to short-circuit
 * (rather than throw)
 */
public class PropertyGraphListener {
	public class ChangeEvent {
		SourcesPropertyChangeEvents bean;

		String propertyName;

		Object object;

		boolean bound;

		public ChangeEvent(ListenerNode listenerNode, Object object,
				boolean bound) {
			this.bean = listenerNode.bean;
			this.propertyName = listenerNode.event.getPropertyName();
			this.object = object;
			this.bound = bound;
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("bean", bean, "propertyName",
					propertyName, "object", object, "newValue", bound);
		}
	}

	public Topic<ChangeEvent> topicChangeEvent = Topic.create();

	SourcesPropertyChangeEvents root;

	/**
	 * 
	 * @param root
	 *            The graph root to attach to
	 */
	public PropertyGraphListener(SourcesPropertyChangeEvents root) {
		this.root = root;
		register(this.root).bindReachables();
	}

	Map<ListenerNode, ListenerNode> nodes = AlcinaCollections.newHashMap();

	class ListenerNode implements PropertyChangeListener {
		SourcesPropertyChangeEvents bean;

		Map<ListenerNode, ListenerNode> boundDescendants = AlcinaCollections
				.newLinkedHashMap();

		PropertyChangeEvent event;

		/* */
		public ListenerNode(SourcesPropertyChangeEvents bean) {
			this.bean = bean;
		}

		@Override
		public int hashCode() {
			return Objects.hash(bean);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ListenerNode) {
				ListenerNode o = (ListenerNode) obj;
				/*
				 * identity equality
				 */
				return bean == o.bean;
			} else {
				return false;
			}
		}

		void bindReachables() {
			Reflections.at(bean).properties().stream().forEach(property -> {
				bindValues(property.get(bean));
			});
			bean.addPropertyChangeListener(this);
		}

		void bindValues(Object object) {
			boolean fire = event != null;
			if (object instanceof SourcesPropertyChangeEvents) {
				bindDescendant((SourcesPropertyChangeEvents) object);
			} else if (object instanceof Collection) {
				((Collection) object).forEach(this::bindValues);
				fire = false;
			} else if (object instanceof Map) {
				throw new UnsupportedOperationException();
			} else {
				/*
				 * primitive etc
				 */
			}
			maybeFire(object, fire, true);
		}

		void unbindValues(Object object) {
			boolean fire = event != null;
			if (object instanceof SourcesPropertyChangeEvents) {
				ListenerNode node = new ListenerNode(
						(SourcesPropertyChangeEvents) object);
				nodes.get(node);
				nodes.get(node).unbind();
			} else if (object instanceof Collection) {
				((Collection) object).forEach(this::unbindValues);
				fire = false;
			} else if (object instanceof Map) {
				throw new UnsupportedOperationException();
			} else {
				/*
				 * primitive etc
				 */
			}
			maybeFire(object, fire, false);
		}

		void bindDescendant(SourcesPropertyChangeEvents bean) {
			ListenerNode descendant = register(bean);
			boundDescendants.put(descendant, descendant);
			descendant.bindReachables();
		}

		void maybeFire(Object object, boolean fire, boolean bound) {
			if (fire) {
				topicChangeEvent.publish(new ChangeEvent(this, object, bound));
			}
		}

		void unbind() {
			boundDescendants.values().forEach(ListenerNode::unbind);
			bean.removePropertyChangeListener(this);
			nodes.remove(this);
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			this.event = event;
			Object oldValue = event.getOldValue();
			Object newValue = event.getNewValue();
			if (oldValue instanceof Collection
					|| newValue instanceof Collection) {
				Intersection intersection = Intersection
						.of((Collection) oldValue, (Collection) newValue);
				intersection.firstOnly.forEach(this::unbindValues);
				intersection.secondOnly.forEach(this::bindValues);
			} else {
				unbindValues(oldValue);
				bindValues(newValue);
			}
			this.event = null;
		}
	}

	ListenerNode register(SourcesPropertyChangeEvents bean) {
		ListenerNode node = new ListenerNode(bean);
		Preconditions.checkState(!nodes.containsKey(node));
		nodes.put(node, node);
		return node;
	}
}
