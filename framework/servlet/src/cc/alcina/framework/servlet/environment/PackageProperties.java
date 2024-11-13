package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import com.google.gwt.place.shared.Place;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AbstractUi abstractUi = new _AbstractUi();
    
    public static class _AbstractUi implements TypedProperty.Container {
      public TypedProperty<AbstractUi, Set> appCommandContexts = new TypedProperty<>(AbstractUi.class, "appCommandContexts");
      public TypedProperty<AbstractUi, CommandContext.Provider> commandContextProvider = new TypedProperty<>(AbstractUi.class, "commandContextProvider");
      public TypedProperty<AbstractUi, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(AbstractUi.class, "keybindingsHandler");
      public TypedProperty<AbstractUi, DirectedLayout> layout = new TypedProperty<>(AbstractUi.class, "layout");
      public TypedProperty<AbstractUi, Place> place = new TypedProperty<>(AbstractUi.class, "place");
      public TypedProperty<AbstractUi, RemoteComponentProtocol.Session> session = new TypedProperty<>(AbstractUi.class, "session");
    }
    
//@formatter:on
}
