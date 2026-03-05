package cc.alcina.framework.servlet.component.featuretree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features.Entry;

class Table extends Model.Fields {
	@Directed.Wrap("thead")
	Columns columns = new Columns();

	@Directed.Wrap("tbody")
	List<Row> rows;

	Table(List<Entry> entries, Class<? extends Feature> featureFilter) {
		entries.removeIf(e -> !e.filter(featureFilter));
		columns.columns = new ArrayList<>();
		columns.columns.add(new CoverageColumn());
		entries.stream().map(Entry::releaseVersion).distinct()
				.sorted(new Feature.ReleaseVersion.Cmp())
				.map(ReleaseColumn::new).forEach(columns.columns::add);
		rows = entries.stream().map(Row::new).collect(Collectors.toList());
		PlaceChangeEvent.Handler handler = evt -> {
			rows.forEach(Row::updateSelected);
		};
		bindings().addRegistration(() -> Client.eventBus()
				.addHandler(PlaceChangeEvent.TYPE, handler));
	}

	@Directed(tag = "td")
	class Cell extends Model.Fields {
		@Directed
		String text = "\u00a0";

		@Binding(type = Type.CLASS_PROPERTY)
		String statusName;

		Entry entry;

		Column col;

		Cell(Entry entry, Column col) {
			this.entry = entry;
			this.col = col;
		}

		@Override
		public void onNodeContext(NodeContext event) {
			if (entry.children.size() > 0) {
				statusName = "container";
				return;
			}
			if (col instanceof ReleaseColumn) {
				populateFromReleaseColulmn((ReleaseColumn) col);
			}
			if (col instanceof CoverageColumn) {
				populateFromCoverage();
			}
		}

		void populateFromCoverage() {
			if (service(FeatureTable.Service.class).getFeatures()
					.hasTestCoverage(entry.feature)) {
				statusName = "test-coverage";
			}
			if (service(FeatureTable.Service.class).getFeatures()
					.hasNonStandardTestCoverage(entry.feature)) {
				statusName = "non-standard-test-coverage";
			}
		}

		void populateFromReleaseColulmn(ReleaseColumn col) {
			if (col.version != null && entry.releaseVersion() != col.version) {
				return;
			}
			Class<? extends Feature.Status> status = entry.status();
			statusName = status == null ? "unknown"
					: Ax.cssify(status.getSimpleName().replace("_", ""));
		}
	}

	@Directed(tag = "tr")
	class Columns extends Model.Fields {
		@Directed(tag = "th")
		String featureColumn = "Feature";

		@Directed(tag = "th")
		List<Column> columns;
	}

	@Directed(tag = "td")
	class FeatureCell extends Model.Fields {
		@Directed
		String featureName;

		@Binding(
			type = Type.STYLE_ATTRIBUTE,
			to = "paddingLeft",
			transform = Binding.UnitRem.class)
		double depth;

		FeatureCell(Entry entry) {
			featureName = entry.displayName();
			depth = entry.depth() * 0.7;
		}
	}

	class Column extends Model.Fields {
	}

	@Directed
	class CoverageColumn extends Column {
		@Directed
		String name = "Test coverage";
	}

	@Directed
	class ReleaseColumn extends Column {
		Class<? extends Feature.ReleaseVersion> version;

		@Directed
		String versionNumber;

		@Directed
		String versionName;

		@Binding(type = Type.PROPERTY)
		String title;

		ReleaseColumn(Class<? extends Feature.ReleaseVersion> version) {
			this.version = version;
			versionNumber = version == null ? "---"
					: version.getSimpleName()
							.replaceFirst("(.+?)(\\d+x[^_]+).*", "$2");
			versionName = version == null ? null
					: version.getSimpleName()
							.replaceFirst("(.+?)(\\d+x[^_]+)_?(.*)", "$3");
			versionName = Ax.blankTo(versionName, "\u00a0");
			versionName = CommonUtils.deInfix(versionName.replace("_", " "));
			title = version == null ? "no version" : version.getSimpleName();
		}
	}

	@Directed(tag = "tr")
	class Row extends Model.Fields implements DomEvents.Click.Handler {
		@Directed
		FeatureCell featureCell;

		@Directed
		List<Cell> cells = new ArrayList<>();

		Entry entry;

		@Binding(type = Type.CSS_CLASS)
		boolean selected;

		Row(Entry entry) {
			this.entry = entry;
			featureCell = new FeatureCell(entry);
			columns.columns.stream().map(col -> new Cell(entry, col))
					.forEach(cells::add);
			updateSelected();
		}

		@Override
		public void onClick(Click event) {
			FeaturePlace place = new FeaturePlace();
			place.feature = entry.feature;
			place.go();
		}

		public void setSelected(boolean selected) {
			set("selected", this.selected, selected,
					() -> this.selected = selected);
		}

		void updateSelected() {
			Place currentPlace = Client.currentPlace();
			boolean selected = FeatureTree.place().feature == entry.feature;
			setSelected(selected);
			if (selected) {
				exec(() -> provideElement().scrollIntoView()).deferred()
						.dispatch();
			}
		}
	}
}