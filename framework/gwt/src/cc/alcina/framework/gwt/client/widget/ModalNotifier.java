package cc.alcina.framework.gwt.client.widget;

public interface ModalNotifier {
	public void modalOn();

	public void modalOff();

	public void setMasking(boolean masking);

	public void setStatus(String status);
	
	public void setProgress(double progress);
}
