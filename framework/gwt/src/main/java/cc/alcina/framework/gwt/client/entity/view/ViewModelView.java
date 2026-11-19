package cc.alcina.framework.gwt.client.entity.view;

import java.beans.PropertyChangeListener;

import com.google.gwt.user.client.ui.IsWidget;

public interface ViewModelView<VM extends ViewModel>
		extends IsWidget, PropertyChangeListener {
	public VM getModel();

	public void setModel(VM model);
}
