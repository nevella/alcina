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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NativeEvent.Modifier;
import com.google.gwt.event.dom.client.KeyCodes;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestorCommand;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

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

	int keyCode() default -1;

	NativeEvent.Modifier[] modifiers() default {};

	/*
	 * default signifies no filter
	 */
	Class<? extends Filter> filter() default Filter.class;

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface Keybindings {
		KeyBinding[] value();
	}

	@Reflected
	public interface Filter extends Predicate<Class<? extends ModelEvent>> {
	}

	public static class MatchData {
		// initially only one (no chords)
		public List<Entry> entries;

		public Class<? extends ModelEvent> eventType;

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
				NativeEvent nativeEvent) {
			return entries.get(0).matches(contexts, nativeEvent);
		}

		@Override
		public String toString() {
			return Ax.format("%s => %s", entries, eventType.getSimpleName());
		}

		public class Entry {
			public KeyBinding binding;

			public Set<NativeEvent.Modifier> modifiers;

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
					NativeEvent nativeEvent) {
				if (contexts.stream()
						.anyMatch(ctx -> this.contexts.contains(ctx))) {
					if (nativeEvent == null) {
						return true;
					}
					Set<Modifier> modifiers = nativeEvent.getModifiers();
					if (binding.key().length() > 0) {
						String key = nativeEvent.getKey();
						if (!key.equalsIgnoreCase(binding.key())) {
							return false;
						}
					} else {
						Preconditions.checkArgument(binding.keyCode() != 0);
						if (nativeEvent.getKeyCode() != binding.keyCode()) {
							return false;
						}
						switch (binding.keyCode()) {
						case KeyCodes.KEY_TAB:
						case KeyCodes.KEY_ENTER:
						case KeyCodes.KEY_SPACE:
							// these have focus meanings, so don't fire if an
							// element has focus
							if (Document.get().getActiveElement() != null) {
								return false;
							}
						}
					}
					if (!modifiers.equals(this.modifiers)) {
						return false;
					}
					return true;
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
			return matchData.matches(contexts, nativeEvent);
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

		public static boolean testFilter(Class<? extends ModelEvent> eventType,
				KeyBinding keyBinding) {
			Class<? extends Filter> filterClass = keyBinding.filter();
			if (filterClass != KeyBinding.Filter.class) {
				boolean test = Reflections.newInstance(filterClass)
						.test(eventType);
				if (!test) {
					return false;
				}
			}
			return true;
		}
	}
}
