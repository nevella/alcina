package cc.alcina.framework.servlet.component.sequence;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.TypedProperties;

@TypedProperties
public class SequenceSettings extends Bindable.Fields {
	public static SequenceSettings get() {
		return SequenceBrowser.Ui.get().settings;
	}
	//

	static PackageProperties._SequenceSettings properties = PackageProperties.sequenceSettings;

	public PropertyDisplayMode propertyDisplayMode = PropertyDisplayMode.QUARTER_WIDTH;

	public String sequenceKey;

	public enum PropertyDisplayMode {
		QUARTER_WIDTH, HALF_WIDTH, NONE
	}

	public PropertyDisplayMode nextPropertyDisplayMode() {
		PropertyDisplayMode next = PropertyDisplayMode
				.values()[(propertyDisplayMode.ordinal() + 1)
						% PropertyDisplayMode.values().length];
		properties.propertyDisplayMode.set(this, next);
		return next;
	}
}
