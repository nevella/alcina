package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;

@TypedProperties
@ReflectiveSerializer.Checks(ignore = false)
public class SequenceSettings extends Bindable.Fields {
	public static SequenceSettings get() {
		return SequenceBrowser.Ui.get().settings;
	}
	//

	static PackageProperties._SequenceSettings properties = PackageProperties.sequenceSettings;

	public DetailDisplayMode detailDisplayMode = DetailDisplayMode.QUARTER_WIDTH;

	public ColumnSet columnSet = ColumnSet.STANDARD;

	public String sequenceKey;

	public enum DetailDisplayMode {
		QUARTER_WIDTH, HALF_WIDTH, FULL_WIDTH, NONE
	}

	public enum ColumnSet {
		STANDARD, DETAIL
	}

	public DetailDisplayMode nextDetailDisplayMode() {
		DetailDisplayMode next = DetailDisplayMode
				.values()[(detailDisplayMode.ordinal() + 1)
						% DetailDisplayMode.values().length];
		properties.detailDisplayMode.set(this, next);
		return next;
	}

	public ColumnSet nextColumnSet() {
		ColumnSet next = ColumnSet.values()[(columnSet.ordinal() + 1)
				% ColumnSet.values().length];
		properties.columnSet.set(this, next);
		return next;
	}
}
