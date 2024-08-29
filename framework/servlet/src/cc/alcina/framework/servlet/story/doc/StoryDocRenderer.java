package cc.alcina.framework.servlet.story.doc;

import java.io.File;
import java.util.List;

import cc.alcina.framework.gwt.client.story.doc.StoryDocObservable;

public interface StoryDocRenderer {
	void render(StoryDocPart part, File folder,
			List<StoryDocObservable> observables);
}
