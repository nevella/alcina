package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import com.google.gwt.place.shared.Place;
import java.lang.Class;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AbstractUi abstractUi = new _AbstractUi();
    
    public static class _AbstractUi implements TypedProperty.Container {
      public TypedProperty<AbstractUi, Class> appCommandContext = new TypedProperty<>(AbstractUi.class, "appCommandContext");
      public TypedProperty<AbstractUi, CommandContext.Provider> commandContextProvider = new TypedProperty<>(AbstractUi.class, "commandContextProvider");
      public TypedProperty<AbstractUi, DirectedLayout> layout = new TypedProperty<>(AbstractUi.class, "layout");
      public TypedProperty<AbstractUi, Place> place = new TypedProperty<>(AbstractUi.class, "place");
    }
    
//@formatter:on
}
