package cc.alcina.framework.gwt.client.data.view;

import java.util.function.Supplier;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.widget.VisibilityChangeEvent;
import cc.alcina.framework.gwt.client.widget.VisibilityChangeEvent.HasVisibilityChangeHandlers;

public class SubwidgetActivitySupport
		implements AttachEvent.Handler, VisibilityChangeEvent.Handler {
	private Widget widget;

	private Supplier<ActivityMapper> activityMapperSupplier;

	private ActivityMapper activityMapper;

	private ActivityManager activityManager;

	private AcceptsOneWidget display;

	private boolean activateOnInitialAttach;

	private boolean receivingEvents;

	public SubwidgetActivitySupport(Widget widget,
			Supplier<ActivityMapper> activityMapperSupplier,
			AcceptsOneWidget display) {
		this.widget = widget;
		this.activityMapperSupplier = activityMapperSupplier;
		this.display = display;
		assert display != null;
	}

	public void goToWhere() {
		Place where = ClientFactory.get().getPlaceController().getWhere();
		Activity activity = activityMapper.getActivity(where);
		if (activity != null) {
			activity.start(display, null);
		}
	}

	public void installAttachHandler() {
		this.activateOnInitialAttach = true;
		installAttachHandler0();
	}

	public void installVisibleHandler() {
		installAttachHandler0();
		((HasVisibilityChangeHandlers) widget).addVisibilityChangeHandler(this);
	}

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		if (activityManager == null && !activateOnInitialAttach) {
			return;
		}
		activate(event.isAttached());
	}

	@Override
	public void onVisiblityChange(VisibilityChangeEvent event) {
		activate(event.isVisible());
	}

	private void installAttachHandler0() {
		widget.addAttachHandler(this);
	}

	protected void activate(boolean activate) {
		if (activate) {
			if (activityManager == null) {
				activityMapper = activityMapperSupplier.get();
				activityManager = new ActivityManager(activityMapper,
						ClientFactory.get().getEventBus());
			}
			if (!receivingEvents) {
				receivingEvents = true;
				activityManager.setDisplay(display);
				goToWhere();
			}
		} else {
			if (activityManager != null) {
				receivingEvents = false;
				activityManager.setDisplay(null);
			}
		}
	}
}
