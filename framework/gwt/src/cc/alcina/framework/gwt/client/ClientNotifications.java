package cc.alcina.framework.gwt.client;

import java.util.List;

import cc.alcina.framework.common.client.spi.LogWriter;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl.MessageType;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

public interface ClientNotifications extends LogWriter{
	public abstract void confirm(String msg, final OkCallback callback);

	public abstract String getLogString();

	public abstract void hideDialog();

	public abstract boolean isDialogAnimationEnabled();

	public abstract void log(String s);

	public abstract void metricLogEnd(String key);

	public abstract void metricLogStart(String key);

	public abstract void setDialogAnimationEnabled(
			boolean dialogAnimationEnabled);

	public abstract void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons);

	public abstract void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons,
			String containerStyle);

	public abstract void showError(String msg, Throwable throwable);

	public abstract void showError(Throwable caught);

	public abstract void showLog();

	public abstract void showMessage(String msg);

	public abstract void showMessage(Widget msg);

	public abstract void showWarning(String msg);

	public abstract void showWarning(String msg, String detail);

	public abstract void notifyOfCompletedSaveFromOffline();

	public abstract ModalNotifier getModalNotifier(String message);
}