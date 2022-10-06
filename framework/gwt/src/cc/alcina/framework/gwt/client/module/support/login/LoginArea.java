package cc.alcina.framework.gwt.client.module.support.login;

import com.google.gwt.place.shared.Place;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsort;
import cc.alcina.framework.gwt.client.place.BasePlace;

@Directed
/*
 * Non-generic, since it ignores place
 */
public abstract class LoginArea extends DirectedActivity {
	private LoginConsort loginConsort;

	private Object contents;

	public LoginArea(Place place) {
		if (place instanceof BasePlace) {
			setPlace((BasePlace) place);
		}
		this.loginConsort = createLoginConsort();
		this.loginConsort.init(model -> setContents(model));
		loginConsort.exitListenerDelta(v -> {
			if (v instanceof Throwable) {
			} else {
				Registry.impl(HandshakeConsort.class)
						.handleLoggedIn(loginConsort.getLastResponse());
			}
		}, false, true);
		loginConsort.start();
	}

	@Directed
	public Object getContents() {
		return this.contents;
	}

	public void setContents(Object contents) {
		var old_contents = this.contents;
		this.contents = contents;
		propertyChangeSupport().firePropertyChange("contents", old_contents,
				contents);
	}

	protected abstract LoginConsort createLoginConsort();
}
