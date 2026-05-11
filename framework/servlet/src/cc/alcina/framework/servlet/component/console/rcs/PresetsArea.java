package cc.alcina.framework.servlet.component.console.rcs;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.domain.search.BindableSearchFilter;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionSearchDefinition.Preset;

class PresetsArea extends Model.All {
	static class PresetArea extends Model.All {
		String name;

		@Directed.Wrap("count")
		Link count;

		@Property.Not
		Preset preset;

		PresetArea(Preset preset) {
			this.preset = preset;
			this.name = preset.name();
			RomcomSessionSearchDefinition definition = preset.getDefinition();
			InstanceQuery query = RomcomSessionSequence.createInstanceQuery();
			RomcomSessionSequence sequence = (RomcomSessionSequence) query
					.toOracleQuery().get();
			BindableSearchFilter bsf = new BindableSearchFilter(definition);
			int rows = (int) sequence.elements.stream().filter(bsf).count();
			RomcomSessionPlace place = new RomcomSessionPlace();
			place.sequencePlace.search = preset.getDefinition();
			count = Link.of(place).withText(String.valueOf(rows));
		}
	}

	List<PresetArea> presets = Arrays.stream(Preset.values())
			.map(PresetArea::new).toList();
}
