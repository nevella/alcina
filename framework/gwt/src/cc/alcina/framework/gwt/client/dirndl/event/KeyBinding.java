package cc.alcina.framework.gwt.client.dirndl.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NativeEvent.Modifier;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.event.KeyBinding.Keybindings;

/**
 * Models a keybinding, e.g. META-SHIFT-F7
 */
@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE })
@Repeatable(Keybindings.class)
public @interface KeyBinding {
	String key();

	NativeEvent.Modifier[] modifiers() default {};

	Class<? extends CommandContext>[] context() default {};

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

		public Class<? extends ModelEvent> getEventType() {
			return eventType;
		}

		class Entry {
			KeyBinding binding;

			Set<NativeEvent.Modifier> modifiers;

			Set<Class<? extends CommandContext>> contexts;

			Entry(KeyBinding binding) {
				this.binding = binding;
				modifiers = Arrays.stream(binding.modifiers())
						.map(Modifier::osDependent).collect(Collectors.toSet());
				contexts = Arrays.stream(binding.context())
						.collect(Collectors.toSet());
			}

			boolean matches(List<Class<? extends CommandContext>> contexts,
					Set<Modifier> modifiers, String key) {
				if (contexts.isEmpty() || contexts.stream()
						.anyMatch(ctx -> contexts.contains(ctx))) {
					return modifiers.equals(this.modifiers)
							&& key.equalsIgnoreCase(binding.key());
				} else {
					return false;
				}
			}

			@Override
			public String toString() {
				return Ax.format("%s '%s'", modifiers, binding.key());
			}
		}

		public MatchData(Class<? extends ModelEvent> eventType) {
			this.eventType = eventType;
			entries = Reflections.at(eventType).annotations(KeyBinding.class)
					.stream().map(Entry::new).collect(Collectors.toList());
		}

		boolean matches(List<Class<? extends CommandContext>> contexts,
				Set<Modifier> modifiers, String key) {
			return entries.get(0).matches(contexts, modifiers, key);
		}

		@Override
		public String toString() {
			return Ax.format("%s => %s", entries, eventType.getSimpleName());
		}
	}

	public static class Support {
		public static boolean matches(MatchData matchData,
				NativeEvent nativeEvent) {
			List<Class<? extends CommandContext>> contexts = Registry
					.optional(CommandContext.Provider.class)
					.map(CommandContext.Provider::getContexts)
					.orElse(List.of());
			return matchData.matches(contexts, nativeEvent.getModifiers(),
					nativeEvent.getKey());
		}
	}
}
