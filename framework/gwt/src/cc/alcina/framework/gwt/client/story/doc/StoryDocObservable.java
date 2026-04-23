package cc.alcina.framework.gwt.client.story.doc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.process.TreeProcess.TreeIndexPath;
import cc.alcina.framework.gwt.client.story.Story.Point;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;

@Bean(PropertySource.FIELDS)
public abstract class StoryDocObservable
		implements ContextObservable, Comparable<StoryDocObservable> {
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

	public String getPointClassName() {
		return pointClassName;
	}

	public List<String> ancestorClassNames;

	public List<String> filterPointDisplayNames;

	public String path() {
		return ancestorDisplayNames.stream().skip(1)
				.collect(Collectors.joining(" > "));
	}

	protected StoryDocObservable() {
	}

	@Override
	public int compareTo(StoryDocObservable o) {
		return TreeIndexPath.of(treePath)
				.compareTo(TreeIndexPath.of(o.treePath));
	}

	protected StoryDocObservable(Visit visit) {
		this.date = new Date();
		this.visit = visit;
		ancestorDisplayNames = new ArrayList<>();
		ancestorClassNames = new ArrayList<>();
		filterPointDisplayNames = new ArrayList<>();
		Visit cursor = visit;
		Class<? extends Point> restrictToPoint = visit.teller().restrictToPoint;
		boolean seenPointRestriction = false;
		while (cursor != null) {
			ancestorDisplayNames.add(0, cursor.getDisplayName());
			ancestorClassNames.add(0, cursor.point.getClass().getName());
			if (cursor.point.getClass() == restrictToPoint) {
				seenPointRestriction = true;
			}
			if (seenPointRestriction) {
				filterPointDisplayNames.add(0, cursor.getDisplayName());
			}
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

	public String filterPathOrPath() {
		if (filterPointDisplayNames.size() > 0) {
			return filterPointDisplayNames.stream().skip(1)
					.collect(Collectors.joining(" > "));
		} else {
			return path();
		}
	}
}