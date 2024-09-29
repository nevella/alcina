package cc.alcina.framework.servlet.story.doc;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.TellerContext;
import cc.alcina.framework.gwt.client.story.doc.Feature_StoryDoc;

/**
 * <p>
 * Stories emit observables during their 'telling' - those can be collected
 * (initially, only to a local folder), and then packaged (either post-story-end
 * or offline) to a result format (such as a standalone markup document, or a
 * resource file for interactive help)
 * 
 * <h3>Implementation parts:</h3>
 * <ul>
 * <li>Observables - subtypes of {@link StoryDocObservable}
 * <li>ObservableRecorder - a class which registers on Story start and records
 * observables as json files
 * </ul>
 */
@Feature.Ref(Feature_StoryDoc.class)
@Registration({ TellerContext.PartConfigurable.class, StoryDocPart.class })
public class StoryDoc implements TellerContext.PartConfigurable<StoryDocPart> {
	StoryTeller teller;

	StoryDocPart part;

	ObservableRecorder observableRecorder;

	@Override
	public void configure(StoryTeller teller, StoryDocPart part) {
		this.teller = teller;
		this.teller.setAttribute(Story.Action.Annotate.Enabled.class, true);
		this.part = part;
		this.observableRecorder = new ObservableRecorder(this);
		observableRecorder.observe();
	}
}
