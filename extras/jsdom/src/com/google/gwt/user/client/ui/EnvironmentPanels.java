package com.google.gwt.user.client.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
@Registration.EnvironmentSingleton
@Reflected
public class EnvironmentPanels {
	public static EnvironmentPanels get() {
		return Registry.impl(EnvironmentPanels.class);
	}

	Map<String, RootPanel> rootPanels = new HashMap<String, RootPanel>();

	Set<Widget> widgetsToDetach = new HashSet<Widget>();
}