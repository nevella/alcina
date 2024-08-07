package com.google.gwt.dom.client.mutations;

import com.google.gwt.dom.client.AttachId;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public class EventListenerChange {
	public Class eventListenerClass;

	public AttachId path;
}
