package cc.alcina.framework.gwt.client.story;

import cc.alcina.framework.gwt.client.story.Story.State.Provider;

public interface TellerContext {
	void init(StoryTeller storyTeller);

	Provider resolveSatisfies(Class<? extends Story.State> state);
}
