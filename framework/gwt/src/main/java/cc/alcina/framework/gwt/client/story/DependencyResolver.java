package cc.alcina.framework.gwt.client.story;

import cc.alcina.framework.gwt.client.story.Story.State;
import cc.alcina.framework.gwt.client.story.Story.State.Provider;

public interface DependencyResolver {
	Provider resolveSatisfies(Class<? extends State> state);
}
