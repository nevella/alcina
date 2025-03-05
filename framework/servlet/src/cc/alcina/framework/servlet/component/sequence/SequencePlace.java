package cc.alcina.framework.servlet.component.sequence;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.PropertySerialization.TypesProvider_Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

/**
 * 
 * 
 */
@Bean(PropertySource.FIELDS)
@ReflectiveSerializer.Checks(ignore = true)
public class SequencePlace extends BasePlace
		implements SequenceBrowserPlace, TreeSerializable {
	public InstanceQuery instanceQuery = new InstanceQuery();

	public String filter;

	public String highlight;

	public int highlightIdx = -1;

	public int selectedElementIdx = -1;

	public IntPair selectedRange;

	@PropertySerialization(
		typesProvider = { TypesProvider_Registry.class,
				SequenceSearchDefinition.class })
	public SequenceSearchDefinition search;

	@Override
	public SequencePlace copy() {
		return super.copy();
	}

	public static class Tokenizer extends BasePlaceTokenizer<SequencePlace> {
		@Override
		protected SequencePlace getPlace0(String token) {
			SequencePlace place = new SequencePlace();
			if (parts.length > 1) {
				try {
					place = FlatTreeSerializer.deserialize(SequencePlace.class,
							parts[1]);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			return place;
		}

		@Override
		protected void getToken0(SequencePlace place) {
			addTokenPart(FlatTreeSerializer.serializeSingleLine(place));
		}
	}

	public SequencePlace withFilter(String filter) {
		this.filter = filter;
		return this;
	}

	public SequencePlace withHighlight(String highlight) {
		this.selectedElementIdx = -1;
		this.highlightIdx = -1;
		this.highlight = highlight;
		return this;
	}

	public SequencePlace withHighlightIdxDelta(int delta) {
		this.highlightIdx += delta;
		return this;
	}

	public SequencePlace withSelectedElementIdx(int selectedElementIdx) {
		this.selectedElementIdx = selectedElementIdx;
		return this;
	}

	public SequencePlace withSelectedRange(IntPair selectedRange) {
		this.selectedRange = selectedRange;
		return this;
	}

	public SequencePlace withHighlightIndicies(int highlightIndex,
			int selectedElementIdx) {
		this.selectedElementIdx = selectedElementIdx;
		this.highlightIdx = highlightIndex;
		return this;
	}

	public boolean hasFilterChange(SequencePlace lastPlace) {
		return lastPlace == null || !Objects.equals(filter, lastPlace.filter);
	}

	public boolean hasHighlightChange(SequencePlace lastPlace) {
		return lastPlace == null
				|| !Objects.equals(highlight, lastPlace.highlight);
	}

	public boolean hasSelectedIndexChange(SequencePlace lastPlace) {
		if (lastPlace == null) {
			return selectedElementIdx != -1;
		} else {
			return selectedElementIdx != lastPlace.selectedElementIdx;
		}
	}

	public SequencePlace deltaSelectedRow(int delta, int size) {
		selectedElementIdx = selectedElementIdx + delta;
		if (selectedElementIdx < 0) {
			selectedElementIdx = size - 1;
		}
		if (selectedElementIdx >= size) {
			selectedElementIdx = size == 0 ? -1 : 0;
		}
		return this;
	}
}
