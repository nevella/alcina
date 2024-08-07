package cc.alcina.framework.gwt.client.ide.provider;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar.BreadcrumbBarButton;

public class BreadcrumbBarLinkChangesButton extends BreadcrumbBarButton
		implements ClickHandler {
	private boolean linked = false;

	private BoundTableExt table;

	private PropertyChangeListener collectionPropertyChangeListener = new PropertyChangeListener() {
		private boolean inChange;

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (inChange || !linked) {
				return;
			}
			inChange = true;
			try {
				Collection<? extends SourcesPropertyChangeEvents> coll = (Collection<? extends SourcesPropertyChangeEvents>) table
						.getValue();
				Object source = evt.getSource();
				for (SourcesPropertyChangeEvents spce : coll) {
					if (spce != source) {
						Reflections.at(spce).property(evt.getPropertyName())
								.set(spce, evt.getNewValue());
					}
				}
			} finally {
				inChange = false;
			}
		}
	};

	public BreadcrumbBarLinkChangesButton() {
		super();
		addClickHandler(this);
		updateText();
	}

	public BoundTableExt getTable() {
		return this.table;
	}

	public boolean isLinked() {
		return this.linked;
	}

	private void maybeAttachToTable() {
		if (this.table != null) {
			this.table.addCollectionPropertyChangeListener(
					collectionPropertyChangeListener);
		}
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		maybeAttachToTable();
	}

	@Override
	public void onClick(ClickEvent event) {
		setLinked(!linked);
	}

	public void setLinked(boolean linked) {
		this.linked = linked;
		updateText();
	}

	public void setTable(BoundTableExt table) {
		this.table = table;
		maybeAttachToTable();
	}

	private void updateText() {
		setText(linked ? "Unlink column changes" : "Link column changes");
	}
}
