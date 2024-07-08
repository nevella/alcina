package cc.alcina.extras.dev.console.alcina.sub1;

import cc.alcina.extras.dev.console.alcina.AlcinaDevConsoleRunnable;
import cc.alcina.extras.webdriver.story.WdContextPart;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTellerPeer;
import cc.alcina.framework.servlet.story.component.traversal.Story_TraversalBrowser;
import cc.alcina.framework.servlet.story.console.Story_Console;

public class AlcSubStory extends AlcinaDevConsoleRunnable {
	@Override
	public void run() throws Exception {
		Story_TraversalBrowser story = new Story_TraversalBrowser();
		StoryTellerPeer peer = new PeerImpl();
		StoryTeller teller = new StoryTeller(peer);
		teller.tell(story);
	}

	static class PeerImpl extends StoryTellerPeer {
		@Override
		public void init(StoryTeller storyTeller) {
			super.init(storyTeller);
			WdContextPart wdContextPart = new WdContextPart();
			wdContextPart.setShouldMaximiseTab(true);
			addPart(WdContextPart.class, wdContextPart);
			storyTeller.setAttribute(
					Story_Console.Attribute.ConsoleShouldRestart.class, true);
		}
	}
}
