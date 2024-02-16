package com.google.gwt.dom.client.mutations;

import java.util.stream.Collectors;

import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationsTests.Tests.Phase;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;

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
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TagTextModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * A set of tests that mutate the dom in different ways, and validate the
 * localdom is updated correctly
 *
 *
 *
 */
public class MutationsTests {
	static boolean hadException = false;

	static native void consoleLog(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
	}-*/;

	public void run() {
		ProcessObservers.observe(DirectedLayout.EventObservable.class, Ax::out,
				true);
		Registry.register().singleton(ClientNotifications.class,
				new ClientNotificationsImpl());
		new Tests().start();
	}

	@Directed(
		bindings = { @Binding(type = Type.PROPERTY, from = "style"),
				@Binding(type = Type.STYLE_ATTRIBUTE, from = "background") })
	public static class TestContainer extends Model {
		private TagTextModel string = new TagTextModel("div", "some text");

		private String style = " padding: 1em; margin: 2em; "
				+ "font-size:3em;display: flex;flex-direction: column;align-items: center";

		private String background;

		public TestContainer(String background) {
			this.background = background;
		}

		public String getBackground() {
			return this.background;
		}

		@Directed
		public TagTextModel getString() {
			return this.string;
		}

		public String getStyle() {
			return this.style;
		}

		public void setString(TagTextModel string) {
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
			protected native void mutate() /*-{
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
        var sub = testContainer.firstChild;
        sub.setAttribute("happily", "nonce");
			}-*/;
		}

		class MODIFY_TEXT extends Test {
			public MODIFY_TEXT() {
				super(Phase.MODIFY_TEXT, "orange");
			}

			@Override
			protected native void mutate() /*-{
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
        var sub = testContainer.firstChild;
        sub.innerText = "happily";
			}-*/;
		}

		class MODIFY_TEXT_2 extends Test {
			public MODIFY_TEXT_2() {
				super(Phase.MODIFY_TEXT_2, "purple");
			}

			@Override
			protected native void mutate() /*-{
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
        testContainer.innerText = "broush";
			}-*/;
		}

		class MODIFY_TEXT_3 extends Test {
			public MODIFY_TEXT_3() {
				super(Phase.MODIFY_TEXT_3, "yellow");
			}

			@Override
			protected native void mutate() /*-{
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
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
			protected native void mutate() /*-{
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
        $wnd.document.body.removeChild(testContainer);

			}-*/;
		}

		class REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT extends Test {
			public REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT() {
				super(Phase.REMOVE_EXISTING_LOCAL_PARTIAL_REINSERT, "blue");
			}

			@Override
			protected native void mutate() /*-{
        //create some nodes, shuffle parenting
        var div1 = $wnd.document.createElement("div1");
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
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
			protected native void mutate() /*-{
        //create some nodes, shuffle parenting
        var div1 = $wnd.document.createElement("div1");
        var testContainer = this.@com.google.gwt.dom.client.mutations.MutationsTests.Tests.Test::remote;
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

			protected abstract void mutate();

			@Override
			public void run() {
				try {
					consoleLog("Running " + getClass().getSimpleName(), false);
					RootPanel.get().clear();
					RootPanel.get().getElement().streamChildren().filter(n -> {
						switch (n.getNodeName().toLowerCase()) {
						case "iframe":
						case "script":
							return false;
						default:
							return true;
						}
					}).collect(Collectors.toList())
							.forEach(Node::removeFromParent);
					TestContainer cont = new TestContainer(color);
					Rendered rendered = new DirectedLayout().render(cont)
							.getRendered();
					rendered.appendToRoot();
					LocalDom.flush();
					this.remote = rendered.asElement().implAccess().jsoRemote();
					LocalDom.invokeExternal(this::mutate);
					new Timer() {
						@Override
						public void run() {
							if (LocalDom.getRemoteMutations().hadExceptions()) {
								consoleLog("Cancelling tests - exceptions",
										true);
							} else {
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
			protected native void mutate() /*-{
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
			protected native void mutate() /*-{
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
