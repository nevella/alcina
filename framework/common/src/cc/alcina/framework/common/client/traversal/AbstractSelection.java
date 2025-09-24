package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasFilterableString;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public abstract class AbstractSelection<T> implements Selection<T> {
	T value;

	Node node;

	String pathSegment;

	List<String> filterableSegments = new ArrayList<>();

	int segmentCounter = -1;

	volatile Selection.Relation relations;

	public boolean hasReplacedBy() {
		return hasRelations() && relations.hasReplacedBy();
	}

	@Override
	public boolean hasRelations() {
		return relations != null;
	}

	public AbstractSelection(Node parentNode, T value, String pathSegment) {
		if (!(this instanceof AllowsNullValue)) {
			Preconditions.checkNotNull(value);
		}
		this.value = value;
		this.node = parentNode.add(this);
		if (pathSegment == null) {
			pathSegment = parentNode.tree().createUniqueSegment(this);
		}
		setPathSegment(pathSegment);
	}

	@Override
	public Relation getRelations() {
		if (relations == null) {
			synchronized (this) {
				if (relations == null) {
					relations = new Relation(this);
				}
			}
		}
		return relations;
	}

	public AbstractSelection(Selection parent, T value) {
		this(parent.processNode(), value, null);
	}

	/*
	 * Use this if collisions/duplicate selections are possible (pathSegment
	 * defines equivalence). Otherwise use the constructor *without* a
	 * pathsegment
	 */
	public AbstractSelection(Selection parent, T value, String pathSegment) {
		this(parent.processNode(), value, pathSegment);
	}

	protected String contentsToString() {
		T t = get();
		if (t instanceof DomNode) {
			return "[DomNode]";
		} else {
			return t.toString();
		}
	}

	public int ensureSegmentCounter() {
		if (segmentCounter == -1) {
			segmentCounter = Integer.parseInt(pathSegment);
		}
		return segmentCounter;
	}

	@Override
	public T get() {
		return value;
	}

	@Override
	public List<String> getFilterableSegments() {
		return this.filterableSegments;
	}

	@Override
	public String getPathSegment() {
		return this.pathSegment;
	}

	@Override
	public Node processNode() {
		return node;
	}

	public void set(T value) {
		this.value = value;
	}

	public void setPathSegment(String pathSegment) {
		Preconditions.checkState(this.pathSegment == null);
		this.pathSegment = pathSegment;
		if (pathSegment != null) {
			filterableSegments.add(pathSegment);
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", getPathSegment(),
				get() == null ? null : Ax.trim(contentsToString(), 150));
	}

	public interface AllowsNullValue {
	}

	Selection.View view;

	@Override
	public Selection.View view() {
		if (view == null) {
			view = Registry.impl(Selection.View.class, getClass());
		}
		return view;
	}

	Selection.RowView rowView;

	@Override
	public Selection.RowView rowView() {
		if (rowView == null) {
			rowView = Registry.impl(Selection.RowView.class, getClass());
			rowView.putSelection(this);
		}
		return rowView;
	}

	protected static class View<S extends AbstractSelection>
			implements Selection.View<S> {
		@Property.Not
		String text;

		@Directed.Exclude
		@Display.Exclude
		public final String getText(S selection) {
			if (text == null) {
				text = computeText(selection);
			}
			return text;
		}

		protected String computeText(S selection) {
			return HasFilterableString.filterableString(selection.get());
		}
	}

	public abstract static class RowView<S extends AbstractSelection>
			extends Model.All implements Selection.RowView<S> {
		@Property.Not
		protected S selection;

		@Directed.Exclude
		@Display.Exclude
		public S getSelection() {
			return selection;
		}

		@Override
		public void putSelection(S selection) {
			this.selection = selection;
		}

		@Registration(
			value = { Selection.RowView.class, AbstractSelection.class })
		public static class Default<S extends AbstractSelection>
				extends RowView<S> {
			public String type;

			@Directed(className = "x-wide")
			public String text;

			@Override
			public void putSelection(S selection) {
				super.putSelection(selection);
				Selection.View view = selection.view();
				this.type = NestedName.get(selection);
				this.text = view.getText(selection);
			}
		}
	}
}