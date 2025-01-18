package cc.alcina.framework.servlet.story.component.gallery;

import java.lang.System.Logger.Level;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.story.component.traversal.Story_TraversalBrowser;
import cc.alcina.framework.servlet.story.console.Story_Console;

class Story_GalleryBrowserImpl {
	static final int TIMEOUT = 5000;

	/*
	 * Ensures the gallery page renders (i.e. the console is running + completed
	 * bootstrapping)
	 */
	static class EnsuresGalleryPageRenders extends Waypoint.Code implements
			Story.State.Provider<Story_GalleryBrowser.State.GalleryPageRenders> {
		@Override
		public void perform(Context context) throws Exception {
			String url = Ax.format("http://127.0.0.1:%s/gallery",
					Story_Console.port());
			SimpleHttp http = new SimpleHttp(url).withTimeout(TIMEOUT);
			try {
				String response = http.asString();
				context.log(Level.INFO, "%s >> %s", url, "OK");
			} catch (Exception e) {
				context.log(Level.WARNING, "issue loading :: %s", url);
				throw e;
			}
		}
	}

	/* Loads the Gallery UI in the browser */
	static class _UiLoaded extends Waypoint
			implements Story.State.Provider<Story_GalleryBrowser.State.Loaded> {
		_UiLoaded() {
			String url = Ax.format("http://127.0.0.1:%s/gallery",
					Story_Console.port());
			action = new Story.Action.Ui.Go();
			location = new Story.Action.Location.Url().withText(url);
		}
	}

	/* Loads the Gallery Home (ok, same as the UI provider) in the browser */
	static class _Home extends Waypoint
			implements Story.State.Provider<Story_GalleryBrowser.State.Home> {
		_Home() {
			String url = Ax.format("http://127.0.0.1:%s/gallery",
					Story_Console.port());
			action = new Story.Action.Ui.Go();
			location = new Story.Action.Location.Url().withText(url);
		}
	}
}
