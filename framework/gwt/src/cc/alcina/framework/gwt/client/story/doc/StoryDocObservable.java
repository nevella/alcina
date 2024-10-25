package cc.alcina.framework.gwt.client.story.doc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;

@Bean(PropertySource.FIELDS)
public abstract class StoryDocObservable
		implements ContextObservers.Observable, Comparable<StoryDocObservable> {
	public Date date;

	public transient Visit visit;

	public String pathDisplayName;

	public String displayName;

	public String treePath;

	public String label;

	public String description;

	public byte[] screenshot;

	public List<String> ancestorDisplayNames;

	public String pointClassName;

	public long index;

	static transient IdCounter counter = new IdCounter();

	public String path() {
		return ancestorDisplayNames.stream().collect(Collectors.joining(" > "));
	}

	protected StoryDocObservable() {
	}

	@Override
	public int compareTo(StoryDocObservable o) {
		return CommonUtils.compareLongs(index, o.index);
	}

	protected StoryDocObservable(Visit visit) {
		index = counter.nextId();
		this.date = new Date();
		this.visit = visit;
		ancestorDisplayNames = new ArrayList<>();
		Visit cursor = visit;
		while (cursor != null) {
			ancestorDisplayNames.add(0, cursor.getDisplayName());
			Node parentNode = cursor.processNode().getParent();
			Object value = parentNode == null ? null : parentNode.getValue();
			if (value instanceof Visit) {
				cursor = (Visit) value;
			} else {
				cursor = null;
			}
		}
		this.pathDisplayName = visit.pathDisplayName();
		this.displayName = visit.displayName();
		this.treePath = visit.processNode().treePath();
		this.label = visit.getLabel();
		this.description = visit.getDescription();
		pointClassName = visit.pointClass().getName();
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