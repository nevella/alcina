package cc.alcina.framework.gwt.client.module.support.login;

import java.util.List;

import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.Validator;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.ModelEvents.Forward;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.PropertyNameTags;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage.Navigation.PutTo;
import cc.alcina.framework.gwt.client.module.support.login.pub.ProcessStatus;

@Directed(
	cssClass = "login-page",
	receives = { ModelEvents.Forward.class, DomEvents.Change.class })
public abstract class LoginPage extends Model
		implements ModelEvents.Forward.Handler, DomEvents.Change.Handler {
	protected LoginConsort loginConsort;

	private final HeadingArea headingArea;

	private Object contents;

	private final ProcessStatus processStatus = new ProcessStatus();

	private final Navigation navigation = new Navigation();

	private Link defaultButton;

	public LoginPage(LoginConsort loginConsort) {
		super();
		this.loginConsort = loginConsort;
		headingArea = new HeadingArea(loginConsort.getTitleText(),
				getSubtitleText());
		populateNavigation();
		connectStatusPanel();
	}

	@Directed
	public Object getContents() {
		return this.contents;
	}

	@Directed
	public HeadingArea getHeadingArea() {
		return this.headingArea;
	}

	public LoginConsort getLoginConsort() {
		return this.loginConsort;
	}

	@Directed
	public Navigation getNavigation() {
		return this.navigation;
	}

	@Directed
	public ProcessStatus getProcessStatus() {
		return this.processStatus;
	}

	@Override
	public void onChange(Change event) {
		// caused by an enter on a form field. Equivalent to "next" since
		// single-input
		onForward(null);
	}

	@Override
	public void onForward(Forward event) {
		if (!validate()) {
			return;
		}
		onForwardValidated();
	}

	public void setContents(Object contents) {
		this.contents = contents;
	}

	protected void connectStatusPanel() {
		processStatus.connectToTopics(loginConsort.topicCallingRemote,
				loginConsort.topicMessage);
	}

	protected abstract String getEnteredText();

	protected String getMessage(ValidationException e) {
		return e.getMessage();
	}

	protected abstract String getSubtitleText();

	protected abstract Validator getValidator();

	protected void onForwardValidated() {
		loginConsort.onClickNext();
	}

	protected void populateNavigation() {
		defaultButton = new Link().withText("Next")
				.withModelEvent(Forward.class);
		// FIXME - ui2 - definitely want progress here
		// .withAsyncTopic(controller.topicCallingRemote);
		navigation.put(defaultButton, PutTo.NEXT);
	}

	protected boolean validate() {
		Validator validator = getValidator();
		try {
			validator.validate(getEnteredText());
			return true;
		} catch (ValidationException e) {
			processStatus.addMessage(getMessage(e), "validation");
			return false;
		}
	}

	@Directed
	@PropertyNameTags
	public static class HeadingArea extends Model {
		private final String heading;

		private final String subHeading;

		public HeadingArea(String heading, String subHeading) {
			this.heading = heading;
			this.subHeading = subHeading;
		}

		@Directed
		public String getHeading() {
			return this.heading;
		}

		@Directed(renderer = LeafRenderer.Html.class)
		public String getSubHeading() {
			return this.subHeading;
		}
	}

	@Directed
	public static class Navigation extends Model {
		private Link back;

		private List<Link> options;

		private Link next;

		@Directed.Wrap("back")
		public Link getBack() {
			return this.back;
		}

		@Directed.Wrap("next")
		public Link getNext() {
			return this.next;
		}

		@Directed.Wrap("options")
		public List<Link> getOptions() {
			return this.options;
		}

		public void put(Link link, PutTo putTo) {
			switch (putTo) {
			case BACK:
				setBack(link);
				break;
			case OPTIONS:
				setOptions(IdentityArrayList.add(getOptions(), link));
				break;
			case NEXT:
				setNext(link);
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}

		public void setBack(Link back) {
			var old_back = this.back;
			this.back = back;
			propertyChangeSupport().firePropertyChange("back", old_back, back);
		}

		public void setNext(Link next) {
			var old_next = this.next;
			this.next = next;
			propertyChangeSupport().firePropertyChange("next", old_next, next);
		}

		public void setOptions(List<Link> options) {
			var old_options = this.options;
			this.options = options;
			propertyChangeSupport().firePropertyChange("options", old_options,
					options);
		}

		public enum PutTo {
			BACK, OPTIONS, NEXT
		}
	}
}
