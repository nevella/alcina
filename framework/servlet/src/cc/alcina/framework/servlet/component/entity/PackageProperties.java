package cc.alcina.framework.servlet.component.entity;

import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;

public class PackageProperties {
	// auto-generated, do not modify
	//@formatter:off
    
    public static _EntityBrowser_Ui entityBrowser_ui = new _EntityBrowser_Ui();
    static _NonOptimisedQueryCache_EntrySummary nonOptimisedQueryCache_entrySummary = new _NonOptimisedQueryCache_EntrySummary();
    
    public static class _EntityBrowser_Ui implements TypedProperty.Container {
      public TypedProperty<EntityBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(EntityBrowser.Ui.class, "appCommandContexts");
      public TypedProperty<EntityBrowser.Ui, NonOptimisedQueryCache> cache = new TypedProperty<>(EntityBrowser.Ui.class, "cache");
      public TypedProperty<EntityBrowser.Ui, Boolean> clearPostSelectionLayers = new TypedProperty<>(EntityBrowser.Ui.class, "clearPostSelectionLayers");
      public TypedProperty<EntityBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(EntityBrowser.Ui.class, "commandContextProvider");
      public TypedProperty<EntityBrowser.Ui, RemoteComponentObservables.ObservableEntry> history = new TypedProperty<>(EntityBrowser.Ui.class, "history");
      public TypedProperty<EntityBrowser.Ui, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(EntityBrowser.Ui.class, "keybindingsHandler");
      public TypedProperty<EntityBrowser.Ui, String> mainCaption = new TypedProperty<>(EntityBrowser.Ui.class, "mainCaption");
      public TypedProperty<EntityBrowser.Ui, EntityBrowser.Ui.EntityPeer> peer = new TypedProperty<>(EntityBrowser.Ui.class, "peer");
      public TypedProperty<EntityBrowser.Ui, TraversalPlace> place = new TypedProperty<>(EntityBrowser.Ui.class, "place");
      public TypedProperty<EntityBrowser.Ui, SelectionMarkup> selectionMarkup = new TypedProperty<>(EntityBrowser.Ui.class, "selectionMarkup");
      public TypedProperty<EntityBrowser.Ui, TraversalSettings> settings = new TypedProperty<>(EntityBrowser.Ui.class, "settings");
      public TypedProperty<EntityBrowser.Ui, SelectionTraversal> traversal = new TypedProperty<>(EntityBrowser.Ui.class, "traversal");
      public TypedProperty<EntityBrowser.Ui, String> traversalId = new TypedProperty<>(EntityBrowser.Ui.class, "traversalId");
      public TypedProperty<EntityBrowser.Ui, String> traversalPath = new TypedProperty<>(EntityBrowser.Ui.class, "traversalPath");
      public TypedProperty<EntityBrowser.Ui, Boolean> useSelectionSegmentPath = new TypedProperty<>(EntityBrowser.Ui.class, "useSelectionSegmentPath");
    }
    
    static class _NonOptimisedQueryCache_EntrySummary implements TypedProperty.Container {
      TypedProperty<NonOptimisedQueryCache.EntrySummary, Object> message = new TypedProperty<>(NonOptimisedQueryCache.EntrySummary.class, "message");
    }
    
//@formatter:on
}
