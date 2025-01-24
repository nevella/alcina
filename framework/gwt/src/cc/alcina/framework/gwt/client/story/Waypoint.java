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
import cc.alcina.framework.common.client.util.Ax;
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

	protected List<Story.Action.Annotate> annotateActions;

	protected Story.Action.Location location;

	protected Class<? extends Feature> feature;

	boolean populated;

	protected List<? extends Story.Point> children;

	protected ConditionalImpl conditional;

	protected String label;

	protected String description;

	public Waypoint() {
	}

	public String getLabel() {
		ensurePopulated();
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		ensurePopulated();
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	@Override
	public List<Story.Action.Annotate> getAnnotateActions() {
		ensurePopulated();
		return annotateActions;
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
			name = getClass().getSimpleName().replaceFirst("^_", "");
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
			Story.Decl.Feature ann = reflector
					.annotation(Story.Decl.Feature.class);
			if (ann != null) {
				feature = ann.value();
			}
		}
		if (label == null) {
			Story.Decl.Label ann = reflector.annotation(Story.Decl.Label.class);
			if (ann != null) {
				label = ann.value();
			}
		}
		if (description == null) {
			Story.Decl.Description ann = reflector
					.annotation(Story.Decl.Description.class);
			if (ann != null) {
				description = Registry.impl(TextInterpolator.class)
						.interpolate(getClass(), ann.value());
			}
			if (Ax.isBlank(description) && Ax.notBlank(label)) {
				description = label;
			}
		}
		if (action == null) {
			Annotation actionAnnotation = Registry
					.query(DeclarativeAction.class).untypedRegistrations()
					.filter(DeclarativeAction::isNotAnnotate)
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
		if (annotateActions == null) {
			List<Annotation> annotateActionAnnotations = Registry
					.query(DeclarativeAction.class).untypedRegistrations()
					.filter(DeclarativeAction::isAnnotate)
					.map(clazz -> reflector
							.annotation((Class<? extends Annotation>) clazz))
					.filter(Objects::nonNull).collect(Collectors.toList());
			annotateActions = annotateActionAnnotations.stream().map(ann -> {
				Story.Decl.Action.Converter converter = Registry.impl(
						Story.Decl.Action.Converter.class,
						ann.annotationType());
				return (Story.Action.Annotate) converter.convert(ann);
			}).collect(Collectors.toList());
			if (action != null && annotateActions.size() > 0) {
				throw new IllegalStateException(
						"To enforce separation of concerns,  an actiom and a non-empty annotateActions list on the same point is illegal");
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

	public interface TextInterpolator {
		String interpolate(Class<?> waypointClass, String value);
	}
}
