package cc.alcina.framework.gwt.client.story;

import java.util.List;

public class Waypoint implements Story.Point {
	protected String name;

	protected List<Class<? extends Story.State>> requires;

	public List<Class<? extends Story.State>> getRequires() {
		return requires;
	}

	protected Story.Action action;

	public Waypoint() {
		name = getClass().getSimpleName();
	}

	public Story.Action getAction() {
		return action;
	}

	public void setRequires(List<Class<? extends Story.State>> requires) {
		this.requires = requires;
	}

	protected List<? extends Story.Point> children;

	public List<? extends Story.Point> getChildren() {
		return children;
	}

	public void setChildren(List<? extends Story.Point> children) {
		this.children = children;
	}

	public String getName() {
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
