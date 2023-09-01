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
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features.Entry;
import cc.alcina.framework.servlet.component.featuretree.place.FeaturePlace;

class Table extends Model.Fields {
	@Directed.Wrap("thead")
	Columns columns = new Columns();

	@Directed.Wrap("tbody")
	List<Row> rows;

	Table(List<Entry> entries) {
		columns.releaseColumns = entries.stream().map(Entry::releaseVersion)
				.distinct().sorted(new Feature.ReleaseVersion.Cmp())
				.map(ReleaseColumn::new).collect(Collectors.toList());
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

		Cell(Entry entry, ReleaseColumn col) {
			if (entry.children.size() > 0
					|| entry.releaseVersion() != col.version) {
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
		List<ReleaseColumn> releaseColumns;
	}

	@Directed(tag = "td")
	@Directed.PropertyNameTags
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

	@Directed
	@Directed.PropertyNameTags
	class ReleaseColumn extends Model.Fields {
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
			columns.releaseColumns.stream().map(col -> new Cell(entry, col))
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
			boolean selected = currentPlace instanceof FeaturePlace
					&& ((FeaturePlace) currentPlace).feature == entry.feature;
			setSelected(selected);
		}
	}
}