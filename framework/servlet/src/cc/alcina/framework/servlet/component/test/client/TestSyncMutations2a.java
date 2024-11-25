package cc.alcina.framework.servlet.component.test.client;

import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.Consort;
import cc.alcina.framework.common.client.consort.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagText;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.test.client.TestSyncMutations2a.Tests.Phase;

class TestSyncMutations2a {
	static boolean hadException = false;

	static native void consoleLog(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
	}-*/;

	void run() {
		ProcessObservers.observe(DirectedLayout.EventObservable.class, Ax::out,
				true);
		Registry.register().singleton(ClientNotifications.class,
				new ClientNotificationsImpl());
		Tests consort = new Tests();
		consort.addOneTimeFinishedCallback(new AsyncCallback() {
			@Override
			public void onFailure(Throwable e) {
				e.printStackTrace();
				ClientUtils.consoleInfo("   [TestSyncMutations2a] Failed");
			}

			@Override
			public void onSuccess(Object arg0) {
				ClientUtils.consoleInfo("   [TestSyncMutations2a] Passed");
			}
		});
		consort.start();
	}

	@Directed(
		bindings = { @Binding(type = Type.PROPERTY, from = "style"),
				@Binding(type = Type.STYLE_ATTRIBUTE, from = "background") })
	static class TestContainer extends Model {
		TagText string = new TagText("div", "some text");

		String style = " padding: 1em; margin: 2em; "
				+ "font-size:3em;display: flex;flex-direction: column;align-items: center";

		String background;

		TestContainer(String background) {
			this.background = background;
		}

		String getBackground() {
			return this.background;
		}

		@Directed
		TagText getString() {
			return this.string;
		}

		String getStyle() {
			return this.style;
		}

		void setString(TagText string) {
			var old_string = this.string;
			this.string = string;
			propertyChangeSupport().firePropertyChange("string", old_string,
					string);
		}
	}

	static class Tests extends Consort<Phase> {
		Tests() {
			addPlayer(new TEST_INSERTION());
			addPlayer(new TEST_INSERTION_2());
			addPlayer(new REMOVE_EXISTING_LOCAL());
			addPlayer(new REMOVE_EXISTING_LOCAL_REINSERT());
			addPlayer(new REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT());
			addPlayer(new MODIFY_TEXT());
			addPlayer(new MODIFY_TEXT_2());
			addPlayer(new MODIFY_TEXT_3());
			addPlayer(new MODIFY_ATTR());
			addEndpointPlayer();
		}

		class MODIFY_ATTR extends Test {
			public MODIFY_ATTR() {
				super(Phase.MODIFY_ATTR, "pink");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        var sub = testContainer.firstChild;
        sub.setAttribute("happily", "nonce");
			}-*/;
		}

		class MODIFY_TEXT extends Test {
			public MODIFY_TEXT() {
				super(Phase.MODIFY_TEXT, "orange");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        var sub = testContainer.firstChild;
        sub.innerText = "happily";
			}-*/;
		}

		class MODIFY_TEXT_2 extends Test {
			public MODIFY_TEXT_2() {
				super(Phase.MODIFY_TEXT_2, "purple");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        testContainer.innerText = "broush";
			}-*/;
		}

		class MODIFY_TEXT_3 extends Test {
			public MODIFY_TEXT_3() {
				super(Phase.MODIFY_TEXT_3, "yellow");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        testContainer.firstChild.firstChild.nodeValue = "broush";
			}-*/;
		}

		enum Phase {
			MODIFY_TEXT, MODIFY_TEXT_2, MODIFY_TEXT_3, TEST_INSERTION,
			TEST_INSERTION_2, REMOVE_EXISTING_LOCAL,
			REMOVE_EXISTING_LOCAL_REINSERT,
			REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT, MODIFY_ATTR
		}

		class REMOVE_EXISTING_LOCAL extends Test {
			public REMOVE_EXISTING_LOCAL() {
				super(Phase.REMOVE_EXISTING_LOCAL, "pink");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        $wnd.document.body.removeChild(testContainer);

			}-*/;
		}

		class REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT extends Test {
			public REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT() {
				super(Phase.REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT, "blue");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        //create some nodes, shuffle parenting
        var div1 = $wnd.document.createElement("div1");
        $wnd.document.body.removeChild(testContainer);
        var sub = testContainer.firstChild;
        div1.appendChild(sub);
        $wnd.document.body.appendChild(div1);
        var div3 = $wnd.document.createElement("div3");
        $wnd.document.body.appendChild(div3);
        div3.appendChild(sub);
			}-*/;
		}

		class REMOVE_EXISTING_LOCAL_REINSERT extends Test {
			public REMOVE_EXISTING_LOCAL_REINSERT() {
				super(Phase.REMOVE_EXISTING_LOCAL_REINSERT, "green");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        //create some nodes, shuffle parenting
        var div1 = $wnd.document.createElement("div1");
        div1.appendChild(testContainer);
        $wnd.document.body.appendChild(div1);
        var div3 = $wnd.document.createElement("div3");
        $wnd.document.body.appendChild(div3);
        div3.appendChild(testContainer);
			}-*/;
		}

		abstract class Test
				extends EnumRunnableAsyncCallbackPlayer<Void, Phase> {
			private String color;

			ElementJso remote;

			public Test(Phase state, String color) {
				super(state);
				this.color = color;
			}

			protected abstract void mutate(Object testContainer);

			@Override
			public void run() {
				try {
					consoleLog("Running " + getClass().getSimpleName(), false);
					AlcinaGwtTestClient.Utils.clearRootPanel();
					TestContainer cont = new TestContainer(color);
					Rendered rendered = new DirectedLayout().render(cont)
							.getRendered();
					rendered.appendToRoot();
					LocalDom.flush();
					this.remote = rendered.asElement().jsoRemote();
					LocalDom.invokeExternal(() -> mutate(this.remote));
					new Timer() {
						@Override
						public void run() {
							if (LocalDom.getRemoteMutations().hadExceptions()) {
								consoleLog("Cancelling tests - exceptions",
										true);
							} else {
								ClientUtils.consoleInfo("   [%s] - passed",
										getClass().getSimpleName());
								onSuccess(null);
							}
						}
					}.schedule(500);
				} catch (RuntimeException e) {
					hadException = true;
					throw e;
				}
			}
		}

		class TEST_INSERTION extends Test {
			public TEST_INSERTION() {
				super(Phase.TEST_INSERTION, "blue");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        //create some nodes, shuffle parenting
        var div1 = $wnd.document.createElement("div1");
        var div2 = $wnd.document.createElement("div2");
        div2.innerText = 'precious';
        div1.appendChild(div2);
        $wnd.document.body.appendChild(div1);
        var div3 = $wnd.document.createElement("div3");
        $wnd.document.body.appendChild(div3);
        div3.appendChild(div2);
			}-*/;
		}

		class TEST_INSERTION_2 extends Test {
			public TEST_INSERTION_2() {
				super(Phase.TEST_INSERTION_2, "green");
			}

			@Override
			protected native void mutate(Object testContainer) /*-{
        //create some nodes, shuffle parenting
        var div1 = $wnd.document.createElement("div1");
        var div2 = $wnd.document.createElement("div2");
        div2.innerText = 'precious';
        div1.appendChild(div2);
        $wnd.document.body.appendChild(div1);
        var div3 = $wnd.document.createElement("div3");
        $wnd.document.body.appendChild(div3);
        div3.appendChild(div2);
			}-*/;
		}
	}
}
