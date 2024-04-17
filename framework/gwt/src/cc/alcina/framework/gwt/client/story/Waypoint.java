package cc.alcina.framework.gwt.client.story;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;

public class Waypoint implements Story.Point {
	public static Story.Point toStoryPoint(Story.Decl.Point declPoint) {
		Waypoint waypoint = new Waypoint();
		Story.Decl.Action.Code[] code = declPoint.code();
		Preconditions.checkArgument(code.length < 2);
		if (code.length == 1) {
			waypoint.action = Reflections.newInstance(code[0].value());
		}
		return waypoint;
	}

	protected String name;

	protected List<Class<? extends Story.State>> requires;

	public List<Class<? extends Story.State>> getRequires() {
		ensurePopulated();
		return requires;
	}

	protected Story.Action action;

	protected Class<? extends Feature> feature;

	public Class<? extends Feature> getFeature() {
		return feature;
	}

	boolean populated;

	public Waypoint() {
	}

	public Story.Action getAction() {
		ensurePopulated();
		return action;
	}

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
						.stream().map(Story.Decl.Require::value).toList();
			} else {
				requires = List.of();
			}
		}
		if (children == null) {
			{
				List<Story.Decl.Child> childAnns = reflector
						.annotations(Story.Decl.Child.class);
				if (childAnns.size() > 0) {
					children = childAnns.stream().map(Story.Decl.Child::value)
							.map(Reflections::newInstance).toList();
				}
			}
			if (children == null) {
				List<Story.Decl.Point> points = reflector
						.annotations(Story.Decl.Point.class);
				if (points.size() > 0) {
					children = points.stream().map(Waypoint::toStoryPoint)
							.toList();
				}
			}
		}
		if (feature == null) {
			Story.Decl.Feature featureAnn = reflector
					.annotation(Story.Decl.Feature.class);
			if (featureAnn != null) {
				feature = featureAnn.value();
			}
		}
	}

	public void setRequires(List<Class<? extends Story.State>> requires) {
		this.requires = requires;
	}

	protected List<? extends Story.Point> children;

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

	public static abstract class Code extends Waypoint
			implements Story.Action.Code {
		@Override
		public Story.Action getAction() {
			return this;
		}
	}
}
