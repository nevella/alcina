package cc.alcina.framework.jvmclient.service;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl.MessageType;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

public class ClientNotificationsJvmImpl implements ClientNotifications {
	@Override
	public void confirm(String msg, OkCallback callback) {
	}

	@Override
	public String getLogString() {
		return null;
	}

	@Override
	public ModalNotifier getModalNotifier(String message) {
		return new ModalNotifierConsole();
	}

	@Override
	public void hideDialog() {
	}

	@Override
	public boolean isDialogAnimationEnabled() {
		return false;
	}

	@Override
	public void log(String s) {
		System.out.println(s);
	}

	@Override
	public void metricLogEnd(String key) {
	}

	@Override
	public void metricLogStart(String key) {
	}

	public void notifyOfCompletedSaveFromOffline() {
	}

	@Override
	public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
	}

	@Override
	public void showDialog(String captionHTML, Widget captionWidget, String msg,
			MessageType messageType, List<Button> extraButtons) {
	}

	@Override
	public void showDialog(String captionHTML, Widget captionWidget, String msg,
			MessageType messageType, List<Button> extraButtons,
			String containerStyle) {
	}

	@Override
	public void showError(String msg, Throwable throwable) {
		System.out.println(msg);
		throwable.printStackTrace();
	}

	@Override
	public void showError(Throwable throwable) {
		showError("", throwable);
	}

	@Override
	public void showLog() {
	}

	@Override
	public void showMessage(String msg) {
		System.out.println(msg);
	}

	@Override
	public void showMessage(Widget msg) {
	}

	@Override
	public void showWarning(String msg) {
		System.out.println(msg);
	}

	@Override
	public void showWarning(String msg, String detail) {
		System.out.println(msg);
		System.out.println(detail);
	}

	static class ModalNotifierConsole implements ModalNotifier {
		@Override
		public void modalOff() {
			System.out.println("modalnotifier::modal-off");
		}

		@Override
		public void modalOn() {
			System.out.println("modalnotifier::modal-on");
		}

		@Override
		public void setMasking(boolean masking) {
			System.out.println("modalnotifier::masking:" + masking);
		}

		@Override
		public void setProgress(double progress) {
			System.out.println("modalnotifier::progress:" + progress);
		}

		@Override
		public void setStatus(String status) {
			System.out.println("modalnotifier::status:" + status);
		}
	}
}
