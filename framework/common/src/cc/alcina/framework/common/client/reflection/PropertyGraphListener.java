package cc.alcina.framework.common.client.reflection;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.search.SearchDefinitionEditor;

/**
 * Enables listening to a graph (generally a tree) of object/property changes,
 * with customised listening. Think React Store
 */
public class PropertyGraphListener {
	public class ChangeEvent {
	}

	public Topic<ChangeEvent> topicChangeEvent = Topic.create();

	InstanceProperty<?, ?> instanceProperty;

	public PropertyGraphListener(InstanceProperty<?, ?> instanceProperty) {
		this.instanceProperty = instanceProperty;
	}
}
