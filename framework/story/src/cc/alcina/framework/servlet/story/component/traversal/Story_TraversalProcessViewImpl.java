package cc.alcina.framework.servlet.story.component.traversal;

import java.lang.System.Logger.Level;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.story.console.Story_Console;

/*
 * Implementations of Story_TraversalProcessView action performers
 */
class Story_TraversalProcessViewImpl {
	static final int TIMEOUT = 5000;

	/*
	 * Ensures the traversal was performed
	 */
	/*
	 * TODO - if this times out, restart the alcina devconsole (the traversal
	 * was evicted). That'll be a good test of dependency resolution anyway -
	 * add as child deps Story_Console.State.ConsoleNotRunning - which will
	 * clear ConsoleRunning - and Story_Console.State.ConsoleRunning.
	 */
	static class EnsuresCroissanteriaTraversalPerformed extends Waypoint.Code
			implements
			Story.State.Provider<Story_TraversalProcessView.State.CroissanteriaTraversalPerformed> {
		@Override
		public void perform(Context context) throws Exception {
			String url = Ax.format(
					"http://127.0.0.1:%s/traversal?action=await&path=0.1",
					Story_Console.port());
			SimpleHttp http = new SimpleHttp(url).withTimeout(TIMEOUT);
			try {
				String response = http.asString();
				context.log(Level.INFO, "%s >> %s", url, response);
			} catch (Exception e) {
				context.log(Level.WARNING, "issue loading :: %s", url);
				throw e;
			}
		}
	}
	/* Loads the traversal UI in the browser */

	static class TraversalUiLoaded extends Waypoint implements
			Story.State.Provider<Story_TraversalProcessView.State.TraversalUiLoaded> {
		TraversalUiLoaded() {
			String url = Ax.format("http://127.0.0.1:%s/traversal",
					Story_Console.port());
			action = new Story.Action.Ui.Go();
			location = new Story.Action.Location.Url().withText(url);
		}
	}
}
