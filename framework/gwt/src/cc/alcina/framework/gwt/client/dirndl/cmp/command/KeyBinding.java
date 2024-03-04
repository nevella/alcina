package cc.alcina.framework.gwt.client.dirndl.cmp.command;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NativeEvent.Modifier;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;

/**
 * Models a keybinding, e.g. META-SHIFT-F7
 */
@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE })
public @interface KeyBinding {
	Class<? extends CommandContext>[] context() default {};

	String key();

	NativeEvent.Modifier[] modifiers() default {};

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Keybindings {
		KeyBinding[] value();
	}

	public static class MatchData {
		// initially only one (no chords)
		List<Entry> entries;

		Class<? extends ModelEvent> eventType;

		String checkInvalid() {
			if (entries.get(0).contexts.isEmpty()) {
				return Ax.format("KeyBound event type %s has no contexts",
						NestedName.get(eventType));
			} else {
				return null;
			}
		}

		public MatchData(Class<? extends ModelEvent> eventType) {
			this.eventType = eventType;
			entries = Reflections.at(eventType).annotations(KeyBinding.class)
					.stream().map(Entry::new).collect(Collectors.toList());
		}

		public Class<? extends ModelEvent> getEventType() {
			return eventType;
		}

		boolean matches(Set<Class<? extends CommandContext>> contexts,
				Set<Modifier> modifiers, String key) {
			return entries.get(0).matches(contexts, modifiers, key);
		}

		@Override
		public String toString() {
			return Ax.format("%s => %s", entries, eventType.getSimpleName());
		}

		class Entry {
			KeyBinding binding;

			Set<NativeEvent.Modifier> modifiers;

			Set<Class<? extends CommandContext>> contexts;

			Entry(KeyBinding binding) {
				this.binding = binding;
				modifiers = Arrays.stream(binding.modifiers())
						.map(Modifier::osDependent).collect(Collectors.toSet());
				// note that contexts will be the same for all keybindings (if
				// multiple)
				contexts = Support.getContexts(eventType);
			}

			boolean matches(Set<Class<? extends CommandContext>> contexts,
					Set<Modifier> modifiers, String key) {
				if (contexts.stream().anyMatch(ctx -> contexts.contains(ctx))) {
					return key.equalsIgnoreCase(binding.key())
							&& modifiers.equals(this.modifiers);
				} else {
					return false;
				}
			}

			@Override
			public String toString() {
				return Ax.format("%s '%s'", modifiers, binding.key());
			}
		}
	}

	public static class Support {
		public static boolean matches(
				Set<Class<? extends CommandContext>> contexts,
				MatchData matchData, NativeEvent nativeEvent) {
			return matchData.matches(contexts, nativeEvent.getModifiers(),
					nativeEvent.getKey());
		}

		static Map<Class<? extends ModelEvent>, Set<Class<? extends CommandContext>>> classContexts = AlcinaCollections
				.newUnqiueMap();

		/*
		 * If there are multiple keybinding annotations (for a chord), just use
		 * the first
		 */
		public static synchronized Set<Class<? extends CommandContext>>
				getContexts(Class<? extends ModelEvent> clazz) {
			return classContexts.computeIfAbsent(clazz, clz -> {
				Set<Class<? extends CommandContext>> result = AlcinaCollections
						.newUniqueSet();
				ClassReflector<? extends ModelEvent> reflector = Reflections
						.at(clazz);
				KeyBinding binding = reflector.annotation(KeyBinding.class);
				Arrays.stream(binding.context()).forEach(result::add);
				AppSuggestorCommand suggestorCommand = reflector
						.annotation(AppSuggestorCommand.class);
				if (suggestorCommand != null) {
					Arrays.stream(AppSuggestorCommand.Support
							.contexts(suggestorCommand)).forEach(result::add);
				}
				return result;
			});
		}
	}
}
