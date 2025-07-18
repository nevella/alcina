package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.CommandContext;
import cc.alcina.framework.gwt.client.dirndl.cmp.command.KeybindingsHandler;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.environment.AbstractUi;
import cc.alcina.framework.servlet.servlet.AuthenticationTokenStore;
import com.google.gwt.place.shared.Place;
import java.lang.Boolean;
import java.util.List;
import java.util.Set;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _AbstractUi abstractUi = new _AbstractUi();
    
    public static class _AbstractUi implements TypedProperty.Container {
      public TypedProperty<AbstractUi, Set> appCommandContexts = new TypedProperty<>(AbstractUi.class, "appCommandContexts");
      public TypedProperty<AbstractUi, AuthenticationTokenStore> authenticationTokenStore = new TypedProperty<>(AbstractUi.class, "authenticationTokenStore");
      public TypedProperty<AbstractUi, AbstractUi.ClientExceptionNotificationPolicy> clientExceptionNotificationPolicy = new TypedProperty<>(AbstractUi.class, "clientExceptionNotificationPolicy");
      public TypedProperty<AbstractUi, CommandContext.Provider> commandContextProvider = new TypedProperty<>(AbstractUi.class, "commandContextProvider");
      public TypedProperty<AbstractUi, KeybindingsHandler> keybindingsHandler = new TypedProperty<>(AbstractUi.class, "keybindingsHandler");
      public TypedProperty<AbstractUi, DirectedLayout> layout = new TypedProperty<>(AbstractUi.class, "layout");
      public TypedProperty<AbstractUi, List> notifiedExceptions = new TypedProperty<>(AbstractUi.class, "notifiedExceptions");
      public TypedProperty<AbstractUi, Place> place = new TypedProperty<>(AbstractUi.class, "place");
      public TypedProperty<AbstractUi, Boolean> reloading = new TypedProperty<>(AbstractUi.class, "reloading");
      public TypedProperty<AbstractUi, RemoteComponentProtocol.Session> session = new TypedProperty<>(AbstractUi.class, "session");
      public static class InstanceProperties extends InstanceProperty.Container<AbstractUi> {
        public  InstanceProperties(AbstractUi source){super(source);}
        public InstanceProperty<AbstractUi, Set> appCommandContexts(){return new InstanceProperty<>(source,PackageProperties.abstractUi.appCommandContexts);}
        public InstanceProperty<AbstractUi, AuthenticationTokenStore> authenticationTokenStore(){return new InstanceProperty<>(source,PackageProperties.abstractUi.authenticationTokenStore);}
        public InstanceProperty<AbstractUi, AbstractUi.ClientExceptionNotificationPolicy> clientExceptionNotificationPolicy(){return new InstanceProperty<>(source,PackageProperties.abstractUi.clientExceptionNotificationPolicy);}
        public InstanceProperty<AbstractUi, CommandContext.Provider> commandContextProvider(){return new InstanceProperty<>(source,PackageProperties.abstractUi.commandContextProvider);}
        public InstanceProperty<AbstractUi, KeybindingsHandler> keybindingsHandler(){return new InstanceProperty<>(source,PackageProperties.abstractUi.keybindingsHandler);}
        public InstanceProperty<AbstractUi, DirectedLayout> layout(){return new InstanceProperty<>(source,PackageProperties.abstractUi.layout);}
        public InstanceProperty<AbstractUi, List> notifiedExceptions(){return new InstanceProperty<>(source,PackageProperties.abstractUi.notifiedExceptions);}
        public InstanceProperty<AbstractUi, Place> place(){return new InstanceProperty<>(source,PackageProperties.abstractUi.place);}
        public InstanceProperty<AbstractUi, Boolean> reloading(){return new InstanceProperty<>(source,PackageProperties.abstractUi.reloading);}
        public InstanceProperty<AbstractUi, RemoteComponentProtocol.Session> session(){return new InstanceProperty<>(source,PackageProperties.abstractUi.session);}
      }
      
      public  InstanceProperties instance(AbstractUi instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
