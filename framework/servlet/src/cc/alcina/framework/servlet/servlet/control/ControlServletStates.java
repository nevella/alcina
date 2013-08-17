package cc.alcina.framework.servlet.servlet.control;

public class ControlServletStates {
	private WriterMode writerMode;

	private WriterRelayMode writerRelayMode;

	private WriterServiceMode writerServiceMode;

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
}
