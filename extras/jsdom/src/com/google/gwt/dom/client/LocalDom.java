package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.util.Ax;

public class LocalDom {
	static LocalDom instance = new LocalDom();

	private static LocalDom get() {
		return instance;
	}

	public static void eventMod(NativeEvent evt, String eventName) {
		get().eventMod0(evt, eventName);
	}

	private void eventMod0(NativeEvent evt, String eventName) {
		log(LocalDomDebug.EVENT_MOD,
				Ax.format("eventMod - %s %s", evt, eventName));
		if (!eventMods.keySet().contains(evt)) {
			eventMods.clear();
			eventMods.put(evt, new ArrayList<>());
		}
		eventMods.get(evt).add(eventName);
	}

	public static void log(LocalDomDebug channel, String message) {
		LocalDomBridge.log(channel, message);
	}

	Map<NativeEvent, List<String>> eventMods = new LinkedHashMap<>();

	public static void flush() {
		// TODO Auto-generated method stub
		
	}
}
