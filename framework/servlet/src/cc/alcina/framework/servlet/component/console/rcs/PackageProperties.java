package cc.alcina.framework.servlet.component.console.rcs;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.SubHeading;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.servlet.component.console.ServerConsolePlace;
import cc.alcina.framework.servlet.component.console.rcs.PresetsArea;
import cc.alcina.framework.servlet.component.console.rcs.RomcomSessionDetailArea;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentEditor;
import cc.alcina.framework.servlet.environment.replay.SessionReplay;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.Date;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _RomcomSessionArea romcomSessionArea = new _RomcomSessionArea();
    static _RomcomSessionCriterion_ActiveCriterion romcomSessionCriterion_activeCriterion = new _RomcomSessionCriterion_ActiveCriterion();
    static _RomcomSessionCriterion_ExceptionCriterion romcomSessionCriterion_exceptionCriterion = new _RomcomSessionCriterion_ExceptionCriterion();
    static _RomcomSessionCriterion_MarkedCriterion romcomSessionCriterion_markedCriterion = new _RomcomSessionCriterion_MarkedCriterion();
    static _RomcomSessionDetailArea romcomSessionDetailArea = new _RomcomSessionDetailArea();
    static _RomcomSessionDetailArea_ReplayArea romcomSessionDetailArea_replayArea = new _RomcomSessionDetailArea_ReplayArea();
    public static _RomcomSessionEntry romcomSessionEntry = new _RomcomSessionEntry();
    static _RomcomSessionSequence_RomcomSessionDetail romcomSessionSequence_romcomSessionDetail = new _RomcomSessionSequence_RomcomSessionDetail();
    static _RomcomSessionSequence_RomcomSessionView romcomSessionSequence_romcomSessionView = new _RomcomSessionSequence_RomcomSessionView();
    
    static class _RomcomSessionArea implements TypedProperty.Container {
      TypedProperty<RomcomSessionArea, List> actions = new TypedProperty<>(RomcomSessionArea.class, "actions");
      TypedProperty<RomcomSessionArea, LoginPage.HeadingArea> heading = new TypedProperty<>(RomcomSessionArea.class, "heading");
      TypedProperty<RomcomSessionArea, ServerConsolePlace> place = new TypedProperty<>(RomcomSessionArea.class, "place");
      TypedProperty<RomcomSessionArea, PresetsArea> presets = new TypedProperty<>(RomcomSessionArea.class, "presets");
      TypedProperty<RomcomSessionArea, SequenceComponentEditor> sequence = new TypedProperty<>(RomcomSessionArea.class, "sequence");
      TypedProperty<RomcomSessionArea, SubHeading> subHeadingActions = new TypedProperty<>(RomcomSessionArea.class, "subHeadingActions");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionArea> {
         InstanceProperties(RomcomSessionArea source){super(source);}
        InstanceProperty<RomcomSessionArea, List> actions(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.actions);}
        InstanceProperty<RomcomSessionArea, LoginPage.HeadingArea> heading(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.heading);}
        InstanceProperty<RomcomSessionArea, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.place);}
        InstanceProperty<RomcomSessionArea, PresetsArea> presets(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.presets);}
        InstanceProperty<RomcomSessionArea, SequenceComponentEditor> sequence(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.sequence);}
        InstanceProperty<RomcomSessionArea, SubHeading> subHeadingActions(){return new InstanceProperty<>(source,PackageProperties.romcomSessionArea.subHeadingActions);}
      }
      
       InstanceProperties instance(RomcomSessionArea instance) {
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
    
    static class _RomcomSessionCriterion_ExceptionCriterion implements TypedProperty.Container {
      TypedProperty<RomcomSessionCriterion.ExceptionCriterion, BooleanEnum> booleanEnum = new TypedProperty<>(RomcomSessionCriterion.ExceptionCriterion.class, "booleanEnum");
      TypedProperty<RomcomSessionCriterion.ExceptionCriterion, String> displayName = new TypedProperty<>(RomcomSessionCriterion.ExceptionCriterion.class, "displayName");
      TypedProperty<RomcomSessionCriterion.ExceptionCriterion, StandardSearchOperator> operator = new TypedProperty<>(RomcomSessionCriterion.ExceptionCriterion.class, "operator");
      TypedProperty<RomcomSessionCriterion.ExceptionCriterion, String> targetPropertyName = new TypedProperty<>(RomcomSessionCriterion.ExceptionCriterion.class, "targetPropertyName");
      TypedProperty<RomcomSessionCriterion.ExceptionCriterion, BooleanEnum> value = new TypedProperty<>(RomcomSessionCriterion.ExceptionCriterion.class, "value");
      TypedProperty<RomcomSessionCriterion.ExceptionCriterion, Boolean> withNull = new TypedProperty<>(RomcomSessionCriterion.ExceptionCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionCriterion.ExceptionCriterion> {
         InstanceProperties(RomcomSessionCriterion.ExceptionCriterion source){super(source);}
        InstanceProperty<RomcomSessionCriterion.ExceptionCriterion, BooleanEnum> booleanEnum(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_exceptionCriterion.booleanEnum);}
        InstanceProperty<RomcomSessionCriterion.ExceptionCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_exceptionCriterion.displayName);}
        InstanceProperty<RomcomSessionCriterion.ExceptionCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_exceptionCriterion.operator);}
        InstanceProperty<RomcomSessionCriterion.ExceptionCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_exceptionCriterion.targetPropertyName);}
        InstanceProperty<RomcomSessionCriterion.ExceptionCriterion, BooleanEnum> value(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_exceptionCriterion.value);}
        InstanceProperty<RomcomSessionCriterion.ExceptionCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_exceptionCriterion.withNull);}
      }
      
       InstanceProperties instance(RomcomSessionCriterion.ExceptionCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionCriterion_MarkedCriterion implements TypedProperty.Container {
      TypedProperty<RomcomSessionCriterion.MarkedCriterion, BooleanEnum> booleanEnum = new TypedProperty<>(RomcomSessionCriterion.MarkedCriterion.class, "booleanEnum");
      TypedProperty<RomcomSessionCriterion.MarkedCriterion, String> displayName = new TypedProperty<>(RomcomSessionCriterion.MarkedCriterion.class, "displayName");
      TypedProperty<RomcomSessionCriterion.MarkedCriterion, StandardSearchOperator> operator = new TypedProperty<>(RomcomSessionCriterion.MarkedCriterion.class, "operator");
      TypedProperty<RomcomSessionCriterion.MarkedCriterion, String> targetPropertyName = new TypedProperty<>(RomcomSessionCriterion.MarkedCriterion.class, "targetPropertyName");
      TypedProperty<RomcomSessionCriterion.MarkedCriterion, BooleanEnum> value = new TypedProperty<>(RomcomSessionCriterion.MarkedCriterion.class, "value");
      TypedProperty<RomcomSessionCriterion.MarkedCriterion, Boolean> withNull = new TypedProperty<>(RomcomSessionCriterion.MarkedCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionCriterion.MarkedCriterion> {
         InstanceProperties(RomcomSessionCriterion.MarkedCriterion source){super(source);}
        InstanceProperty<RomcomSessionCriterion.MarkedCriterion, BooleanEnum> booleanEnum(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_markedCriterion.booleanEnum);}
        InstanceProperty<RomcomSessionCriterion.MarkedCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_markedCriterion.displayName);}
        InstanceProperty<RomcomSessionCriterion.MarkedCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_markedCriterion.operator);}
        InstanceProperty<RomcomSessionCriterion.MarkedCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_markedCriterion.targetPropertyName);}
        InstanceProperty<RomcomSessionCriterion.MarkedCriterion, BooleanEnum> value(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_markedCriterion.value);}
        InstanceProperty<RomcomSessionCriterion.MarkedCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.romcomSessionCriterion_markedCriterion.withNull);}
      }
      
       InstanceProperties instance(RomcomSessionCriterion.MarkedCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionDetailArea implements TypedProperty.Container {
      TypedProperty<RomcomSessionDetailArea, LoginPage.HeadingArea> heading = new TypedProperty<>(RomcomSessionDetailArea.class, "heading");
      TypedProperty<RomcomSessionDetailArea, ServerConsolePlace> place = new TypedProperty<>(RomcomSessionDetailArea.class, "place");
      TypedProperty<RomcomSessionDetailArea, RomcomSessionDetailArea.ReplayArea> replayArea = new TypedProperty<>(RomcomSessionDetailArea.class, "replayArea");
      TypedProperty<RomcomSessionDetailArea, SequenceComponentEditor> sequence = new TypedProperty<>(RomcomSessionDetailArea.class, "sequence");
      TypedProperty<RomcomSessionDetailArea, RomcomSessionDetailArea.SessionMetadata> sequenceMetadata = new TypedProperty<>(RomcomSessionDetailArea.class, "sequenceMetadata");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionDetailArea> {
         InstanceProperties(RomcomSessionDetailArea source){super(source);}
        InstanceProperty<RomcomSessionDetailArea, LoginPage.HeadingArea> heading(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea.heading);}
        InstanceProperty<RomcomSessionDetailArea, ServerConsolePlace> place(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea.place);}
        InstanceProperty<RomcomSessionDetailArea, RomcomSessionDetailArea.ReplayArea> replayArea(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea.replayArea);}
        InstanceProperty<RomcomSessionDetailArea, SequenceComponentEditor> sequence(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea.sequence);}
        InstanceProperty<RomcomSessionDetailArea, RomcomSessionDetailArea.SessionMetadata> sequenceMetadata(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea.sequenceMetadata);}
      }
      
       InstanceProperties instance(RomcomSessionDetailArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionDetailArea_ReplayArea implements TypedProperty.Container {
      TypedProperty<RomcomSessionDetailArea.ReplayArea, Heading> heading = new TypedProperty<>(RomcomSessionDetailArea.ReplayArea.class, "heading");
      TypedProperty<RomcomSessionDetailArea.ReplayArea, Link> replay = new TypedProperty<>(RomcomSessionDetailArea.ReplayArea.class, "replay");
      TypedProperty<RomcomSessionDetailArea.ReplayArea, SessionReplay.State> state = new TypedProperty<>(RomcomSessionDetailArea.ReplayArea.class, "state");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionDetailArea.ReplayArea> {
         InstanceProperties(RomcomSessionDetailArea.ReplayArea source){super(source);}
        InstanceProperty<RomcomSessionDetailArea.ReplayArea, Heading> heading(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea_replayArea.heading);}
        InstanceProperty<RomcomSessionDetailArea.ReplayArea, Link> replay(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea_replayArea.replay);}
        InstanceProperty<RomcomSessionDetailArea.ReplayArea, SessionReplay.State> state(){return new InstanceProperty<>(source,PackageProperties.romcomSessionDetailArea_replayArea.state);}
      }
      
       InstanceProperties instance(RomcomSessionDetailArea.ReplayArea instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _RomcomSessionEntry implements TypedProperty.Container {
      public TypedProperty<RomcomSessionEntry, Boolean> active = new TypedProperty<>(RomcomSessionEntry.class, "active");
      public TypedProperty<RomcomSessionEntry, Date> end = new TypedProperty<>(RomcomSessionEntry.class, "end");
      public TypedProperty<RomcomSessionEntry, Boolean> exception = new TypedProperty<>(RomcomSessionEntry.class, "exception");
      public TypedProperty<RomcomSessionEntry, Long> folderLastModificationDate = new TypedProperty<>(RomcomSessionEntry.class, "folderLastModificationDate");
      public TypedProperty<RomcomSessionEntry, Integer> largestPacket = new TypedProperty<>(RomcomSessionEntry.class, "largestPacket");
      public TypedProperty<RomcomSessionEntry, Boolean> marked = new TypedProperty<>(RomcomSessionEntry.class, "marked");
      public TypedProperty<RomcomSessionEntry, String> path = new TypedProperty<>(RomcomSessionEntry.class, "path");
      public TypedProperty<RomcomSessionEntry, String> sessionId = new TypedProperty<>(RomcomSessionEntry.class, "sessionId");
      public TypedProperty<RomcomSessionEntry, Date> sessionStartTime = new TypedProperty<>(RomcomSessionEntry.class, "sessionStartTime");
      public TypedProperty<RomcomSessionEntry, Integer> shimBytes = new TypedProperty<>(RomcomSessionEntry.class, "shimBytes");
      public TypedProperty<RomcomSessionEntry, Integer> slowestResponse = new TypedProperty<>(RomcomSessionEntry.class, "slowestResponse");
      public TypedProperty<RomcomSessionEntry, Date> start = new TypedProperty<>(RomcomSessionEntry.class, "start");
      public TypedProperty<RomcomSessionEntry, Integer> startupBytes = new TypedProperty<>(RomcomSessionEntry.class, "startupBytes");
      public TypedProperty<RomcomSessionEntry, String> stringProtocolCache = new TypedProperty<>(RomcomSessionEntry.class, "stringProtocolCache");
      public TypedProperty<RomcomSessionEntry, Long> timeFromSessionStartToStartupMessageEmittedClient = new TypedProperty<>(RomcomSessionEntry.class, "timeFromSessionStartToStartupMessageEmittedClient");
      public TypedProperty<RomcomSessionEntry, Long> timeFromStartupMessageEmittedClientToFirstMutationRendered = new TypedProperty<>(RomcomSessionEntry.class, "timeFromStartupMessageEmittedClientToFirstMutationRendered");
      public static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionEntry> {
        public  InstanceProperties(RomcomSessionEntry source){super(source);}
        public InstanceProperty<RomcomSessionEntry, Boolean> active(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.active);}
        public InstanceProperty<RomcomSessionEntry, Date> end(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.end);}
        public InstanceProperty<RomcomSessionEntry, Boolean> exception(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.exception);}
        public InstanceProperty<RomcomSessionEntry, Long> folderLastModificationDate(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.folderLastModificationDate);}
        public InstanceProperty<RomcomSessionEntry, Integer> largestPacket(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.largestPacket);}
        public InstanceProperty<RomcomSessionEntry, Boolean> marked(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.marked);}
        public InstanceProperty<RomcomSessionEntry, String> path(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.path);}
        public InstanceProperty<RomcomSessionEntry, String> sessionId(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.sessionId);}
        public InstanceProperty<RomcomSessionEntry, Date> sessionStartTime(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.sessionStartTime);}
        public InstanceProperty<RomcomSessionEntry, Integer> shimBytes(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.shimBytes);}
        public InstanceProperty<RomcomSessionEntry, Integer> slowestResponse(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.slowestResponse);}
        public InstanceProperty<RomcomSessionEntry, Date> start(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.start);}
        public InstanceProperty<RomcomSessionEntry, Integer> startupBytes(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.startupBytes);}
        public InstanceProperty<RomcomSessionEntry, String> stringProtocolCache(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.stringProtocolCache);}
        public InstanceProperty<RomcomSessionEntry, Long> timeFromSessionStartToStartupMessageEmittedClient(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.timeFromSessionStartToStartupMessageEmittedClient);}
        public InstanceProperty<RomcomSessionEntry, Long> timeFromStartupMessageEmittedClientToFirstMutationRendered(){return new InstanceProperty<>(source,PackageProperties.romcomSessionEntry.timeFromStartupMessageEmittedClientToFirstMutationRendered);}
      }
      
      public  InstanceProperties instance(RomcomSessionEntry instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionSequence_RomcomSessionDetail implements TypedProperty.Container {
      TypedProperty<RomcomSessionSequence.RomcomSessionDetail, List> actions = new TypedProperty<>(RomcomSessionSequence.RomcomSessionDetail.class, "actions");
      TypedProperty<RomcomSessionSequence.RomcomSessionDetail, Date> end = new TypedProperty<>(RomcomSessionSequence.RomcomSessionDetail.class, "end");
      TypedProperty<RomcomSessionSequence.RomcomSessionDetail, Integer> largestPacket = new TypedProperty<>(RomcomSessionSequence.RomcomSessionDetail.class, "largestPacket");
      TypedProperty<RomcomSessionSequence.RomcomSessionDetail, String> sessionId = new TypedProperty<>(RomcomSessionSequence.RomcomSessionDetail.class, "sessionId");
      TypedProperty<RomcomSessionSequence.RomcomSessionDetail, Integer> slowestResponse = new TypedProperty<>(RomcomSessionSequence.RomcomSessionDetail.class, "slowestResponse");
      TypedProperty<RomcomSessionSequence.RomcomSessionDetail, Date> start = new TypedProperty<>(RomcomSessionSequence.RomcomSessionDetail.class, "start");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionSequence.RomcomSessionDetail> {
         InstanceProperties(RomcomSessionSequence.RomcomSessionDetail source){super(source);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionDetail, List> actions(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionDetail.actions);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionDetail, Date> end(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionDetail.end);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionDetail, Integer> largestPacket(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionDetail.largestPacket);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionDetail, String> sessionId(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionDetail.sessionId);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionDetail, Integer> slowestResponse(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionDetail.slowestResponse);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionDetail, Date> start(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionDetail.start);}
      }
      
       InstanceProperties instance(RomcomSessionSequence.RomcomSessionDetail instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _RomcomSessionSequence_RomcomSessionView implements TypedProperty.Container {
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Date> end = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "end");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Integer> largestPacket = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "largestPacket");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, String> sessionId = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "sessionId");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Integer> slowestResponse = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "slowestResponse");
      TypedProperty<RomcomSessionSequence.RomcomSessionView, Date> start = new TypedProperty<>(RomcomSessionSequence.RomcomSessionView.class, "start");
      static class InstanceProperties extends 	InstanceProperty.Container<RomcomSessionSequence.RomcomSessionView> {
         InstanceProperties(RomcomSessionSequence.RomcomSessionView source){super(source);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Date> end(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.end);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Integer> largestPacket(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.largestPacket);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, String> sessionId(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.sessionId);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Integer> slowestResponse(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.slowestResponse);}
        InstanceProperty<RomcomSessionSequence.RomcomSessionView, Date> start(){return new InstanceProperty<>(source,PackageProperties.romcomSessionSequence_romcomSessionView.start);}
      }
      
       InstanceProperties instance(RomcomSessionSequence.RomcomSessionView instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
