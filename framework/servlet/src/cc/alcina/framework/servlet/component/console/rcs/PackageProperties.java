package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.SubHeading;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionArea;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentServer;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Date;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _RomcomSessionArea romcomSessionArea = new _RomcomSessionArea();
    static _RomcomSessionArea_Header romcomSessionArea_header = new _RomcomSessionArea_Header();
    static _RomcomSessionArea_SequenceComponentContainer romcomSessionArea_sequenceComponentContainer = new _RomcomSessionArea_SequenceComponentContainer();
    static _RomcomSessionCriterion_ActiveCriterion romcomSessionCriterion_activeCriterion = new _RomcomSessionCriterion_ActiveCriterion();
    public static _RomcomSessionEntry romcomSessionEntry = new _RomcomSessionEntry();
    static _RomcomSessionSequence_RomcomSessionView romcomSessionSequence_romcomSessionView = new _RomcomSessionSequence_RomcomSessionView();
    
    static class _RomcomSessionArea implements TypedProperty.Container {
      TypedProperty<RomcomSessionArea, List> actions = new TypedProperty<>(RomcomSessionArea.class, "actions");
      TypedProperty<RomcomSessionArea, RomcomSessionArea.SequenceComponentContainer> active = new TypedProperty<>(RomcomSessionArea.class, "active");
      TypedProperty<RomcomSessionArea, LoginPage.HeadingArea> heading = new TypedProperty<>(RomcomSessionArea.class, "heading");
      TypedProperty<RomcomSessionArea, RomcomSessionArea.SequenceComponentContainer> inactive = new TypedProperty<>(RomcomSessionArea.class, "inactive");
      TypedProperty<RomcomSessionArea, ServerConsolePlace> place = new TypedProperty<>(RomcomSessionArea.class, "place");
      TypedProperty<RomcomSessionArea, SubHeading> subHeadingActions = new TypedProperty<>(RomcomSessionArea.class, "subHeadingActions");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionArea> {
         InstanceProperties(RomcomSessionArea source){super(source);}
        InstanceProperty<RomcomSessionArea, List> actions(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.actions);}
        InstanceProperty<RomcomSessionArea, RomcomSessionArea.SequenceComponentContainer> active(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.active);}
        InstanceProperty<RomcomSessionArea, LoginPage.HeadingArea> heading(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.heading);}
        InstanceProperty<RomcomSessionArea, RomcomSessionArea.SequenceComponentContainer> inactive(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.inactive);}
        InstanceProperty<RomcomSessionArea, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.place);}
        InstanceProperty<RomcomSessionArea, SubHeading> subHeadingActions(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.subHeadingActions);}
      }
      
       InstanceProperties instance(RomcomSessionArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionArea_Header implements TypedProperty.Container {
      TypedProperty<RomcomSessionArea.Header, SearchDefinition> searchDefinition = new TypedProperty<>(RomcomSessionArea.Header.class, "searchDefinition");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionArea.Header> {
         InstanceProperties(RomcomSessionArea.Header source){super(source);}
        InstanceProperty<RomcomSessionArea.Header, SearchDefinition> searchDefinition(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea_header.searchDefinition);}
      }
      
       InstanceProperties instance(RomcomSessionArea.Header instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionArea_SequenceComponentContainer implements TypedProperty.Container {
      TypedProperty<RomcomSessionArea.SequenceComponentContainer, RomcomSessionArea.Header> header = new TypedProperty<>(RomcomSessionArea.SequenceComponentContainer.class, "header");
      TypedProperty<RomcomSessionArea.SequenceComponentContainer, SequenceComponentServer> sequence = new TypedProperty<>(RomcomSessionArea.SequenceComponentContainer.class, "sequence");
      TypedProperty<RomcomSessionArea.SequenceComponentContainer, SequencePlace> sequencePlace = new TypedProperty<>(RomcomSessionArea.SequenceComponentContainer.class, "sequencePlace");
      TypedProperty<RomcomSessionArea.SequenceComponentContainer, SubHeading> subHeading = new TypedProperty<>(RomcomSessionArea.SequenceComponentContainer.class, "subHeading");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionArea.SequenceComponentContainer> {
         InstanceProperties(RomcomSessionArea.SequenceComponentContainer source){super(source);}
        InstanceProperty<RomcomSessionArea.SequenceComponentContainer, RomcomSessionArea.Header> header(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea_sequenceComponentContainer.header);}
        InstanceProperty<RomcomSessionArea.SequenceComponentContainer, SequenceComponentServer> sequence(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea_sequenceComponentContainer.sequence);}
        InstanceProperty<RomcomSessionArea.SequenceComponentContainer, SequencePlace> sequencePlace(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea_sequenceComponentContainer.sequencePlace);}
        InstanceProperty<RomcomSessionArea.SequenceComponentContainer, SubHeading> subHeading(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea_sequenceComponentContainer.subHeading);}
      }
      
       InstanceProperties instance(RomcomSessionArea.SequenceComponentContainer instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionCriterion_ActiveCriterion implements TypedProperty.Container {
      TypedProperty<RomcomSessionCriterion.ActiveCriterion, BooleanEnum> booleanEnum = new TypedProperty<>(RomcomSessionCriterion.ActiveCriterion.class, "booleanEnum");
      TypedProperty<RomcomSessionCriterion.ActiveCriterion, String> displayName = new TypedProperty<>(RomcomSessionCriterion.ActiveCriterion.class, "displayName");
      TypedProperty<RomcomSessionCriterion.ActiveCriterion, StandardSearchOperator> operator = new TypedProperty<>(RomcomSessionCriterion.ActiveCriterion.class, "operator");
      TypedProperty<RomcomSessionCriterion.ActiveCriterion, String> targetPropertyName = new TypedProperty<>(RomcomSessionCriterion.ActiveCriterion.class, "targetPropertyName");
      TypedProperty<RomcomSessionCriterion.ActiveCriterion, BooleanEnum> value = new TypedProperty<>(RomcomSessionCriterion.ActiveCriterion.class, "value");
      TypedProperty<RomcomSessionCriterion.ActiveCriterion, Boolean> withNull = new TypedProperty<>(RomcomSessionCriterion.ActiveCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionCriterion.ActiveCriterion> {
         InstanceProperties(RomcomSessionCriterion.ActiveCriterion source){super(source);}
        InstanceProperty<RomcomSessionCriterion.ActiveCriterion, BooleanEnum> booleanEnum(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_activeCriterion.booleanEnum);}
        InstanceProperty<RomcomSessionCriterion.ActiveCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_activeCriterion.displayName);}
        InstanceProperty<RomcomSessionCriterion.ActiveCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_activeCriterion.operator);}
        InstanceProperty<RomcomSessionCriterion.ActiveCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_activeCriterion.targetPropertyName);}
        InstanceProperty<RomcomSessionCriterion.ActiveCriterion, BooleanEnum> value(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_activeCriterion.value);}
        InstanceProperty<RomcomSessionCriterion.ActiveCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_activeCriterion.withNull);}
      }
      
       InstanceProperties instance(RomcomSessionCriterion.ActiveCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _RomcomSessionEntry implements TypedProperty.Container {
      public TypedProperty<RomcomSessionEntry, Boolean> active = new TypedProperty<>(RomcomSessionEntry.class, "active");
      public TypedProperty<RomcomSessionEntry, Date> end = new TypedProperty<>(RomcomSessionEntry.class, "end");
      public TypedProperty<RomcomSessionEntry, Integer> largestPacket = new TypedProperty<>(RomcomSessionEntry.class, "largestPacket");
      public TypedProperty<RomcomSessionEntry, String> path = new TypedProperty<>(RomcomSessionEntry.class, "path");
      public TypedProperty<RomcomSessionEntry, String> sessionId = new TypedProperty<>(RomcomSessionEntry.class, "sessionId");
      public TypedProperty<RomcomSessionEntry, Date> start = new TypedProperty<>(RomcomSessionEntry.class, "start");
      public static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionEntry> {
        public  InstanceProperties(RomcomSessionEntry source){super(source);}
        public InstanceProperty<RomcomSessionEntry, Boolean> active(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.active);}
        public InstanceProperty<RomcomSessionEntry, Date> end(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.end);}
        public InstanceProperty<RomcomSessionEntry, Integer> largestPacket(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.largestPacket);}
        public InstanceProperty<RomcomSessionEntry, String> path(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.path);}
        public InstanceProperty<RomcomSessionEntry, String> sessionId(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.sessionId);}
        public InstanceProperty<RomcomSessionEntry, Date> start(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.start);}
      }
      
      public  InstanceProperties instance(RomcomSessionEntry instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionSequence_RomcomSessionView implements TypedProperty.Container {
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Date> end = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "end");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Integer> largestPacket = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "largestPacket");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, String> sessionId = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "sessionId");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Date> start = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "start");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Link> view = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "view");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionSequence.RomcomSessionView> {
         InstanceProperties(RomcomSessionSequence.RomcomSessionView source){super(source);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Date> end(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.end);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Integer> largestPacket(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.largestPacket);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, String> sessionId(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.sessionId);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Date> start(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.start);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Link> view(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.view);}
      }
      
       InstanceProperties instance(RomcomSessionSequence.RomcomSessionView instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
