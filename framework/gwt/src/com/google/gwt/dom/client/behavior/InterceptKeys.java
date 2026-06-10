package com.google.gwt.dom.client.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NativeEvent.Modifier;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeyBinding.MatchData;
import cc.alcina.framework.gwt.client.util.KeyboardShortcuts;

@Bean(PropertySource.FIELDS)
public final class InterceptKeys extends ElementBehavior.Parameterised {
	@Bean(PropertySource.FIELDS)
	public static final class Entry {
		public String key;

		//
		public int keyCode;

		public Set<NativeEvent.Modifier> modifiers;

		public Entry() {
		}

		Entry(String key, int keyCode, Set<Modifier> modifiers) {
			this.key = key;
			this.keyCode = keyCode;
			this.modifiers = modifiers;
		}

		Entry(MatchData.Entry matchDataEntry) {
			key = matchDataEntry.binding.key();
			keyCode = matchDataEntry.binding.keyCode();
			modifiers = matchDataEntry.modifiers;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Entry) {
				Entry o = (Entry) obj;
				return CommonUtils.equals(key, o.key, keyCode, o.keyCode,
						modifiers, o.modifiers);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(key, keyCode, modifiers);
		}

		boolean matches(NativeEvent nativeEvent) {
			return KeyBinding.MatchData.matchesEvent(nativeEvent, key, keyCode,
					modifiers);
		}
	}

	List<Entry> entries = new ArrayList<>();

	public InterceptKeys() {
	}

	public void addIntercept(String key, int keyCode, Set<Modifier> modifiers) {
		entries.add(new Entry(key, keyCode, modifiers));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof InterceptKeys) {
			InterceptKeys o = (InterceptKeys) obj;
			return CommonUtils.equals(entries, o.entries);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(entries);
	}

	@Override
	public String getEventType() {
		return BrowserEvents.KEYDOWN;
	}

	@Override
	public void onNativeEvent(NativePreviewEvent event,
			Element registeredElement) {
		NativeEvent nativeEvent = event.getNativeEvent();
		if (KeyboardShortcuts
				.eventFiredFromInputish(nativeEvent.getEventTarget())) {
			return;
		}
		if (entries.stream().anyMatch(e -> e.matches(nativeEvent))) {
			nativeEvent.preventDefault();
		}
	}

	@Override
	public List<?> provideParameters() {
		return List.of(entries);
	}
}