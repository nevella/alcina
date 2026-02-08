package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.model.edit.Feature_Dirndl_ChoiceEditor;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.gwt.client.story.Waypoints.Wait1000;
import cc.alcina.framework.servlet.story.component.gallery.Point_GalleryBrowser_ChoiceEditor_MutationConflict.SetDelayConfigurationOff;

/*
 * This tests mutation conflict resolution via the ChoiceEditor gallery page.
 * Through manipulation of server handling speed, it forces a server mutation of
 * an edit area which would conflict with a subsequent client edit of the same
 * area. The conflict is resolved in favour of the client, and _then_ the server
 * cascaded mutations to the combined client mutation are applied to the client
 */
@Decl.Require(Story_GalleryBrowser.State.Home.class)
@Decl.Child(SetDelayConfigurationOff.class)
@Decl.Child(Point_GalleryBrowser_Home.ToChoiceEditor.class)
@Decl.Child(Point_GalleryBrowser_ChoiceEditor_MutationConflict._DefinitionEditor.class)
@Feature.Parent(Feature_Dirndl_ChoiceEditor.class)
public class Point_GalleryBrowser_ChoiceEditor_MutationConflict
		extends Waypoint {
	@Decl.Child(Point_GalleryBrowser_ChoiceEditor._DefinitionEditor.ClickSuggestionsArea.class)
	/*
	 * don't await the overlay - deliberately cause a server mutation to be sent
	 * between SendNameChar1 and SendNameChar2
	 */
	// @Decl.Child(Point_GalleryBrowser_ChoiceEditor._DefinitionEditor.AwaitOverlay.class)
	@Decl.Child(_DefinitionEditor.SendNameChar1.class)
	@Decl.Child(SetDelayConfigurationOn.class)
	@Decl.Child(_DefinitionEditor.SendNameChar2.class)
	@Decl.Child(SetDelayConfigurationOff.class)
	@Decl.Child(Wait1000.class)
	@Decl.Child(_DefinitionEditor.VerifySuggestingNode.class)
	@Decl.Child(Point_GalleryBrowser_ChoiceEditor._DefinitionEditor.ClickLars.class)
	static class _DefinitionEditor extends Waypoint {
		@Decl.Location.CurrentFocus
		@Decl.Action.UI.Keys("l")
		static class SendNameChar1 extends Waypoint {
		}

		@Decl.Location.CurrentFocus
		@Decl.Action.UI.Keys("a")
		static class SendNameChar2 extends Waypoint {
		}

		@Decl.Location.Xpath("//suggesting-node[.='la']")
		@Decl.Action.UI.AwaitPresent
		static class VerifySuggestingNode extends Waypoint {
		}
	}

	@Decl.Location.Url("/control?action=set-property&key=MessageTransportLayerServer.ReceiveChannelImpl.receiveDelay&value=200")
	@Decl.Action.UI.Navigation.Get
	static class SetDelayConfigurationOn extends Waypoint {
	}

	@Decl.Location.Url("/control?action=set-property&key=MessageTransportLayerServer.ReceiveChannelImpl.receiveDelay&value=0")
	@Decl.Action.UI.Navigation.Get
	static class SetDelayConfigurationOff extends Waypoint {
	}
}
