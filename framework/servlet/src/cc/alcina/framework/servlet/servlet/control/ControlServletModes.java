package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ControlServletModes {
	public static ControlServletModes fromProperties() {
		ControlServletModes modes = new ControlServletModes();
		modes.writerMode = CommonUtils.getEnumValueOrNull(WriterMode.class,
				ResourceUtilities.get(AppLifecycleManager.class, "writerMode"),
				true, null);
		modes.writerRelayMode = CommonUtils.getEnumValueOrNull(
				WriterRelayMode.class, ResourceUtilities
						.get(AppLifecycleManager.class, "writerRelayMode"),
				true, null);
		modes.writerServiceMode = CommonUtils.getEnumValueOrNull(
				WriterServiceMode.class, ResourceUtilities
						.get(AppLifecycleManager.class, "writerServiceMode"),
				true, null);
		return modes;
	}

	public static ControlServletModes memberModes() {
		return new ControlServletModes();
	}

	public static ControlServletModes standaloneModes() {
		ControlServletModes modes = new ControlServletModes();
		modes.setWriterMode(WriterMode.CLUSTER_WRITER);
		modes.setWriterRelayMode(WriterRelayMode.WRITE);
		modes.setWriterServiceMode(WriterServiceMode.CONTROLLER);
		return modes;
	}

	private WriterMode writerMode = WriterMode.READ_ONLY;

	private WriterRelayMode writerRelayMode = WriterRelayMode.REJECT;

	private WriterServiceMode writerServiceMode = WriterServiceMode.NOT_CONTROLLER;

	public WriterMode getWriterMode() {
		return this.writerMode;
	}

	public WriterRelayMode getWriterRelayMode() {
		return this.writerRelayMode;
	}

	public WriterServiceMode getWriterServiceMode() {
		return this.writerServiceMode;
	}

	public void setWriterMode(WriterMode writerMode) {
		this.writerMode = writerMode;
	}

	public void setWriterRelayMode(WriterRelayMode writerRelayMode) {
		this.writerRelayMode = writerRelayMode;
	}

	public void setWriterServiceMode(WriterServiceMode writerServiceMode) {
		this.writerServiceMode = writerServiceMode;
	}

	@Override
	public String toString() {
		return Ax.format(
				"writerMode:\t%s\twriterRelayMode:\t%s\twriterServiceMode:\t%s",
				CommonUtils.nullSafeToString(writerMode),
				CommonUtils.nullSafeToString(writerRelayMode),
				CommonUtils.nullSafeToString(writerServiceMode));
	}
}
