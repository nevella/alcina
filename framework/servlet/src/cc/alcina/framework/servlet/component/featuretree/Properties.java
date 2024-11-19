package cc.alcina.framework.servlet.component.featuretree;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.Tables;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features;
import cc.alcina.framework.servlet.component.featuretree.FeatureTable.Features.Entry;

class Properties extends Model.All {
	Heading header = new Heading("Properties");

	FeatureProperties properties;

	@Directed.Exclude
	Features features;

	Properties(Features features) {
		this.features = features;
		PlaceChangeEvent.Handler handler = evt -> {
			updateProperties();
		};
		bindings().addRegistration(() -> Client.eventBus()
				.addHandler(PlaceChangeEvent.TYPE, handler));
		updateProperties();
	}

	public void setProperties(FeatureProperties properties) {
		set("properties", this.properties, properties,
				() -> this.properties = properties);
	}

	void updateProperties() {
		Place currentPlace = Client.currentPlace();
		FeatureProperties properties = null;
		if (currentPlace instanceof FeaturePlace) {
			FeaturePlace featurePlace = (FeaturePlace) currentPlace;
			if (featurePlace.feature != null) {
				properties = new FeatureProperties(featurePlace.feature);
			}
		}
		setProperties(properties);
	}

	@Directed.Transform(Tables.Single.class)
	class FeatureProperties extends Model.Fields {
		String parent;

		String name;

		String internalName;

		String status;

		String depends;

		FeatureProperties(Class<? extends Feature> feature) {
			Entry entry = features.entriesByFeature.get(feature);
			parent = entry.parent == null ? null : entry.parent.displayName();
			name = entry.displayName();
			internalName = entry.feature.getSimpleName();
			status = entry.status() == null ? ""
					: entry.status().getSimpleName().toLowerCase().replace("_",
							" ");
			// TODO - add type - type inherits from parent, overridden by @type
			// at any point
			//
			// report redundant in console
		}
	}
}
