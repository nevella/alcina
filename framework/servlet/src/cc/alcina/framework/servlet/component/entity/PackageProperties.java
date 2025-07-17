package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.entity.EntityBrowser;
import cc.alcina.framework.servlet.component.entity.NonOptimisedQueryCache;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _EntityBrowser_Ui entityBrowser_ui = new _EntityBrowser_Ui();
    static _NonOptimisedQueryCache_EntrySummary nonOptimisedQueryCache_entrySummary = new _NonOptimisedQueryCache_EntrySummary();
    
    public static class _EntityBrowser_Ui implements TypedProperty.Container {
      public TypedProperty<EntityBrowser.Ui, Set> appCommandContexts = new TypedProperty<>(EntityBrowser.Ui.class, "appCommandContexts");
      public TypedProperty<EntityBrowser.Ui, Boolean> appendTableSelections = new TypedProperty<>(EntityBrowser.Ui.class, "appendTableSelections");
      public TypedProperty<EntityBrowser.Ui, NonOptimisedQueryCache> cache = new TypedProperty<>(EntityBrowser.Ui.class, "cache");
      public TypedProperty<EntityBrowser.Ui, Boolean> clearPostSelectionLayers = new TypedProperty<>(EntityBrowser.Ui.class, "clearPostSelectionLayers");
      public TypedProperty<EntityBrowser.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(EntityBrowser.Ui.class, "commandContextProvider");
      public TypedProperty<EntityBrowser.Ui, Model> eventHandlerCustomisation = new TypedProperty<>(EntityBrowser.Ui.class, "eventHandlerCustomisation");
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
      public static class InstanceProperties extends InstanceProperty.Container<EntityBrowser.Ui> {
        public  InstanceProperties(EntityBrowser.Ui source){super(source);}
        public InstanceProperty<EntityBrowser.Ui, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.appCommandContexts);}
        public InstanceProperty<EntityBrowser.Ui, Boolean> appendTableSelections(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.appendTableSelections);}
        public InstanceProperty<EntityBrowser.Ui, NonOptimisedQueryCache> cache(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.cache);}
        public InstanceProperty<EntityBrowser.Ui, Boolean> clearPostSelectionLayers(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.clearPostSelectionLayers);}
        public InstanceProperty<EntityBrowser.Ui, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.commandContextProvider);}
        public InstanceProperty<EntityBrowser.Ui, Model> eventHandlerCustomisation(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.eventHandlerCustomisation);}
        public InstanceProperty<EntityBrowser.Ui, RemoteComponentObservables.ObservableEntry> history(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.history);}
        public InstanceProperty<EntityBrowser.Ui, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.keybindingsHandler);}
        public InstanceProperty<EntityBrowser.Ui, String> mainCaption(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.mainCaption);}
        public InstanceProperty<EntityBrowser.Ui, EntityBrowser.Ui.EntityPeer> peer(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.peer);}
        public InstanceProperty<EntityBrowser.Ui, TraversalPlace> place(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.place);}
        public InstanceProperty<EntityBrowser.Ui, SelectionMarkup> selectionMarkup(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.selectionMarkup);}
        public InstanceProperty<EntityBrowser.Ui, TraversalSettings> settings(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.settings);}
        public InstanceProperty<EntityBrowser.Ui, SelectionTraversal> traversal(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.traversal);}
        public InstanceProperty<EntityBrowser.Ui, String> traversalId(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.traversalId);}
        public InstanceProperty<EntityBrowser.Ui, String> traversalPath(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.traversalPath);}
        public InstanceProperty<EntityBrowser.Ui, Boolean> useSelectionSegmentPath(){return new InstanceProperty<>(source,PackageProperties.entityBrowser_ui.useSelectionSegmentPath);}
      }
      
      public  InstanceProperties instance(EntityBrowser.Ui instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _NonOptimisedQueryCache_EntrySummary implements TypedProperty.Container {
      TypedProperty<NonOptimisedQueryCache.EntrySummary, Object> message = new TypedProperty<>(NonOptimisedQueryCache.EntrySummary.class, "message");
      static class InstanceProperties extends InstanceProperty.Container<NonOptimisedQueryCache.EntrySummary> {
         InstanceProperties(NonOptimisedQueryCache.EntrySummary source){super(source);}
        InstanceProperty<NonOptimisedQueryCache.EntrySummary, Object> message(){return new InstanceProperty<>(source,PackageProperties.nonOptimisedQueryCache_entrySummary.message);}
      }
      
       InstanceProperties instance(NonOptimisedQueryCache.EntrySummary instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
