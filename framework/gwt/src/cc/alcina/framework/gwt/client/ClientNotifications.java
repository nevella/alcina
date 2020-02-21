package cc.alcina.framework.gwt.client;

import java.util.List;
import java.util.Optional;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.spi.LogWriter;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl.MessageType;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

public interface ClientNotifications extends LogWriter {
	public static final String CONTEXT_AUTOSHOW_DIALOG_DETAIL = ClientNotifications.class
			.getName() + ".CONTEXT_AUTOSHOW_DIALOG_DETAIL";

	static ClientNotifications get() {
		return Registry.impl(ClientNotifications.class);
	}

	public abstract void confirm(String msg, final OkCallback callback);

	public abstract String getLogString();

	public abstract ModalNotifier getModalNotifier(String message);

	public abstract void hideDialog();

	public abstract boolean isDialogAnimationEnabled();

	@Override
	public abstract void log(String s);

	public abstract void metricLogEnd(String key);

	public abstract void metricLogStart(String key);

	public abstract void notifyOfCompletedSaveFromOffline();

	public abstract void
			setDialogAnimationEnabled(boolean dialogAnimationEnabled);

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

	default void log(String s, Object... args) {
		log(Ax.format(s, args));
	}

	default Label showCodePopup(String popupText,
			Optional<String> extraClassName) {
		ScrollPanel sp = new ScrollPanel();
		Label label = new InlineHTML(popupText);
		sp.add(label);
		sp.setStyleName("alcina-expandable-label-popup");
		if (extraClassName.isPresent()) {
			sp.addStyleName(extraClassName.get());
		}
		ClientNotifications.get().setDialogAnimationEnabled(false);
		ClientNotifications.get().showMessage(sp);
		ClientNotifications.get().setDialogAnimationEnabled(true);
		return label;
	}

	default void showDevError(Throwable e) {
	}
}