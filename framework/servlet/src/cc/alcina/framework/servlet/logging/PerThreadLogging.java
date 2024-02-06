package cc.alcina.framework.servlet.logging;

public interface PerThreadLogging {
	void beginBuffer();

	String endBuffer();
}
