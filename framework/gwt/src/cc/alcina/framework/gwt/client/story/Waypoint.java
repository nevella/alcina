package cc.alcina.framework.gwt.client.story;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.story.Story.Decl.Action.DeclarativeAction;
import cc.alcina.framework.gwt.client.story.Story.Decl.Location.DeclarativeLocation;

public class Waypoint implements Story.Point {
	public static abstract class Code extends Waypoint
			implements Story.Action.Code {
		@Override
		public Story.Action getAction() {
			return this;
		}
	}

	protected class ConditionalImpl implements Story.Conditional {
		Set<Class<? extends Story.Point>> exitOkOnFalse = Set.of();

		@Override
		public Set<Class<? extends Story.Point>> exitOkOnFalse() {
			return exitOkOnFalse;
		}
	}

	protected String name;

	protected List<Class<? extends Story.State>> requires;

	protected Story.Action action;

	protected Story.Action.Location location;

	protected Class<? extends Feature> feature;

	boolean populated;

	protected List<? extends Story.Point> children;

	protected ConditionalImpl conditional;

	public Waypoint() {
	}

	public ConditionalImpl getConditional() {
		return conditional;
	}

	public List<Class<? extends Story.State>> getRequires() {
		ensurePopulated();
		return requires;
	}

	public Story.Action.Location getLocation() {
		return location;
	}

	public void setLocation(Story.Action.Location location) {
		this.location = location;
	}

	public Class<? extends Feature> getFeature() {
		return feature;
	}

	public Story.Action getAction() {
		ensurePopulated();
		return action;
	}

	public void setRequires(List<Class<? extends Story.State>> requires) {
		this.requires = requires;
	}

	public List<? extends Story.Point> getChildren() {
		ensurePopulated();
		return children;
	}

	public void setChildren(List<? extends Story.Point> children) {
		this.children = children;
	}

	public String getName() {
		ensurePopulated();
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*
	 * Note that created requires/children collections are intentionally mutable
	 */
	protected void ensurePopulated() {
		if (populated == true) {
			return;
		}
		populated = true;
		ClassReflector<? extends Waypoint> reflector = Reflections
				.at(getClass());
		if (name == null) {
			name = getClass().getSimpleName();
		}
		if (requires == null) {
			List<Story.Decl.Require> requireAnns = reflector
					.annotations(Story.Decl.Require.class);
			if (requireAnns.size() > 0) {
				requires = (List<Class<? extends Story.State>>) (List<?>) requireAnns
						.stream().map(Story.Decl.Require::value)
						.collect(Collectors.toList());
			} else {
				requires = new ArrayList<>();
			}
		}
		if (children == null) {
			{
				List<Story.Decl.Child> childAnns = reflector
						.annotations(Story.Decl.Child.class);
				if (childAnns.size() > 0) {
					children = childAnns.stream().map(Story.Decl.Child::value)
							.map(Reflections::newInstance)
							.collect(Collectors.toList());
				}
			}
			if (children == null) {
				children = new ArrayList<>();
			}
		}
		if (feature == null) {
			Story.Decl.Feature featureAnn = reflector
					.annotation(Story.Decl.Feature.class);
			if (featureAnn != null) {
				feature = featureAnn.value();
			}
		}
		if (action == null) {
			Annotation actionAnnotation = Registry
					.query(DeclarativeAction.class).untypedRegistrations()
					.map(clazz -> reflector
							.annotation((Class<? extends Annotation>) clazz))
					.filter(Objects::nonNull).findFirst().orElse(null);
			if (actionAnnotation != null) {
				Story.Decl.Action.Converter converter = Registry.impl(
						Story.Decl.Action.Converter.class,
						actionAnnotation.annotationType());
				action = converter.convert(actionAnnotation);
			}
		}
		if (location == null) {
			Annotation locationAnnotation = Registry
					.query(DeclarativeLocation.class).untypedRegistrations()
					.map(clazz -> reflector
							.annotation((Class<? extends Annotation>) clazz))
					.filter(Objects::nonNull).findFirst().orElse(null);
			if (locationAnnotation != null) {
				Story.Decl.Location.Converter converter = Registry.impl(
						Story.Decl.Location.Converter.class,
						locationAnnotation.annotationType());
				location = converter.convert(locationAnnotation);
			}
		}
		if (conditional == null) {
			conditional = new ConditionalImpl();
			Story.Decl.Conditional.ExitOkOnFalse exitOkOnFalseAnn = reflector
					.annotation(Story.Decl.Conditional.ExitOkOnFalse.class);
			if (exitOkOnFalseAnn != null) {
				conditional.exitOkOnFalse = Set.of(exitOkOnFalseAnn.value());
			}
		}
	}
}
