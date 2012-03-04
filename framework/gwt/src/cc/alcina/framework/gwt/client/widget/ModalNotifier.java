package cc.alcina.framework.gwt.client.widget;

public interface ModalNotifier {
	public void modalOn();

	public void modalOff();

	public void setMasking(boolean masking);

	public void setStatus(String status);
	
	public void setProgress(double progress);
	public static class ModalNotifierNull implements ModalNotifier{

		@Override
		public void modalOn() {
			
		}

		@Override
		public void modalOff() {
			
		}

		@Override
		public void setMasking(boolean masking) {
			
		}

		@Override
		public void setStatus(String status) {
			
		}

		@Override
		public void setProgress(double progress) {
			
		}
		
	}
}
