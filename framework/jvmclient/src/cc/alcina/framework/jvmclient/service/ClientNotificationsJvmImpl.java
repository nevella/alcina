package cc.alcina.framework.jvmclient.service;

import java.util.List;

import cc.alcina.framework.gwt.client.ClientNofications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl.MessageType;
import cc.alcina.framework.gwt.client.logic.OkCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

public class ClientNotificationsJvmImpl implements ClientNofications {
	@Override
	public void confirm(String msg, OkCallback callback) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getLogString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hideDialog() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isDialogAnimationEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void log(String s) {
		System.out.println(s);
	}

	@Override
	public void metricLogEnd(String key) {
		// TODO Auto-generated method stub
	}

	@Override
	public void metricLogStart(String key) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
		// TODO Auto-generated method stub
	}

	@Override
	public void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons) {
		// TODO Auto-generated method stub
	}

	@Override
	public void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons,
			String containerStyle) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
	}

	@Override
	public void showMessage(String msg) {
		System.out.println(msg);
	}

	@Override
	public void showMessage(Widget msg) {
		// TODO Auto-generated method stub
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
}
