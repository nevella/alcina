package cc.alcina.extras.dev.console.alcina.sub1;

import java.util.List;

import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTeller.BeforePerformAction;

/**
 * <p>
 * This VCS version of this file will be stored as a template (.java.template
 * extension).
 * 
 * <p>
 * Update the non-vcs (.java) version for local dev. The template may change -
 * to regenerate your local version from the template, delete your local version
 * and start the dev console
 * 
 * <p>
 * These observers are used for deep process debugging - see
 * {@link ProcessObserver}
 * 
 */
@SuppressWarnings({ "rawtypes", "unused" })
public class AlcDevProcessObserverStory extends ProcessObserver.AppDebug {
	static void debug() {
		int debug = 4;
	}

	List<ProcessObserver> observers;

	public AlcDevProcessObserverStory() {
		initObservers();
	}

	private void initObservers() {
	}

	@Override
	public List<ProcessObserver> getObservers() {
		observers = List.of(new StoryBeforeActionObserver());
		return observers;
	}

	class StoryBeforeActionObserver
			implements ProcessObserver<StoryTeller.BeforePerformAction> {
		@Override
		public void topicPublished(BeforePerformAction action) {
			if (action.getVisit().displayName().equals("TestShowing")) {
				debug();
			}
		}
	}
}