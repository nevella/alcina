package cc.alcina.framework.entity.story.doc;

import java.util.Date;

import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;

public abstract class StoryDocObservable
		implements ProcessObservers.ContextObservers.Observable {
	public Date date;

	public transient Visit visit;

	public String pathDisplayName;

	public String displayName;

	public String treePath;

	protected StoryDocObservable() {
	}

	protected StoryDocObservable(Visit visit) {
		this.date = new Date();
		this.visit = visit;
		this.pathDisplayName = visit.pathDisplayName();
		this.displayName = visit.displayName();
		this.treePath = visit.processNode().treePath();
	}

	public static class DocumentPoint extends StoryDocObservable {
		protected DocumentPoint() {
		}

		public DocumentPoint(Visit visit, String message) {
			super(visit);
			this.message = message;
		}

		public String message;
	}
}