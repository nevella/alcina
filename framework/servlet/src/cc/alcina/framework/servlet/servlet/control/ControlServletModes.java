package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public class ControlServletModes {
	
	private WriterMode writerMode = WriterMode.READ_ONLY;

	private WriterRelayMode writerRelayMode = WriterRelayMode.REJECT;

	private WriterServiceMode writerServiceMode = WriterServiceMode.NOT_CONTROLLER;

	public WriterMode getWriterMode() {
		return this.writerMode;
	}

	public void setWriterMode(WriterMode writerMode) {
		this.writerMode = writerMode;
	}

	public WriterRelayMode getWriterRelayMode() {
		return this.writerRelayMode;
	}

	public void setWriterRelayMode(WriterRelayMode writerRelayMode) {
		this.writerRelayMode = writerRelayMode;
	}

	public WriterServiceMode getWriterServiceMode() {
		return this.writerServiceMode;
	}

	public void setWriterServiceMode(WriterServiceMode writerServiceMode) {
		this.writerServiceMode = writerServiceMode;
	}

	@Override
	public String toString() {
		return CommonUtils
				.formatJ(
						"writerMode:\t%s\twriterRelayMode:\t%s\twriterServiceMode:\t%s",
						CommonUtils.nullSafeToString(writerMode),
						CommonUtils.nullSafeToString(writerRelayMode),
						CommonUtils.nullSafeToString(writerServiceMode));
	}

	public static ControlServletModes fromProperties(StringMap props) {
		ControlServletModes modes = new ControlServletModes();
		modes.writerMode = CommonUtils.getEnumValueOrNull(WriterMode.class,
				props.get("writerMode"), true, null);
		modes.writerRelayMode = CommonUtils
				.getEnumValueOrNull(WriterRelayMode.class,
						props.get("writerRelayMode"), true, null);
		modes.writerServiceMode = CommonUtils.getEnumValueOrNull(
				WriterServiceMode.class, props.get("writerServiceMode"), true,
				null);
		return modes;
	}

	public static ControlServletModes standaloneModes() {
		ControlServletModes modes = new ControlServletModes();
		modes.setWriterMode(WriterMode.CLUSTER_WRITER);
		modes.setWriterRelayMode(WriterRelayMode.WRITE);
		modes.setWriterServiceMode(WriterServiceMode.CONTROLLER);
		return modes;
	}

	public static ControlServletModes memberModes() {
		return new ControlServletModes();
	}

}
