package cc.alcina.framework.gwt.client.widget;

public interface ModalNotifier {
	public void modalOff();

	public void modalOn();

	public void setMasking(boolean masking);

	public void setProgress(double progress);

	public void setStatus(String status);

	public static class ModalNotifierNull implements ModalNotifier {
		@Override
		public void modalOff() {
		}

		@Override
		public void modalOn() {
		}

		@Override
		public void setMasking(boolean masking) {
		}

		@Override
		public void setProgress(double progress) {
		}

		@Override
		public void setStatus(String status) {
		}
	}
}
