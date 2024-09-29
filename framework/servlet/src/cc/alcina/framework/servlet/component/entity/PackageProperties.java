package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.servlet.component.entity.EntityGraphView;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _EntityGraphView_Ui entityGraphView_ui = new _EntityGraphView_Ui();
    
    public static class _EntityGraphView_Ui implements TypedProperty.Container {
      public TypedProperty<EntityGraphView.Ui, Class> appCommandContext = new TypedProperty<>(EntityGraphView.Ui.class, "appCommandContext");
      public TypedProperty<EntityGraphView.Ui, CommandContext.Provider> commandContextProvider = new TypedProperty<>(EntityGraphView.Ui.class, "commandContextProvider");
      public TypedProperty<EntityGraphView.Ui, RemoteComponentObservables.ObservableHistory> history = new TypedProperty<>(EntityGraphView.Ui.class, "history");
      public TypedProperty<EntityGraphView.Ui, String> mainCaption = new TypedProperty<>(EntityGraphView.Ui.class, "mainCaption");
      public TypedProperty<EntityGraphView.Ui, EntityGraphView.Ui.EntityPeer> peer = new TypedProperty<>(EntityGraphView.Ui.class, "peer");
      public TypedProperty<EntityGraphView.Ui, TraversalPlace> place = new TypedProperty<>(EntityGraphView.Ui.class, "place");
      public TypedProperty<EntityGraphView.Ui, TraversalSettings> settings = new TypedProperty<>(EntityGraphView.Ui.class, "settings");
      public TypedProperty<EntityGraphView.Ui, String> traversalId = new TypedProperty<>(EntityGraphView.Ui.class, "traversalId");
      public TypedProperty<EntityGraphView.Ui, String> traversalPath = new TypedProperty<>(EntityGraphView.Ui.class, "traversalPath");
      public TypedProperty<EntityGraphView.Ui, Boolean> useSelectionSegmentPath = new TypedProperty<>(EntityGraphView.Ui.class, "useSelectionSegmentPath");
    }
    
//@formatter:on
}
