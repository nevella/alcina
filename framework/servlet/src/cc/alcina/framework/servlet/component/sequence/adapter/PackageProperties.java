package cc.alcina.framework.servlet.component.sequence.adapter;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import java.lang.Boolean;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _FlightEventCriterion_IsMutationsCriterion flightEventCriterion_isMutationsCriterion = new _FlightEventCriterion_IsMutationsCriterion();
    
    public static class _FlightEventCriterion_IsMutationsCriterion implements TypedProperty.Container {
      public TypedProperty<FlightEventCriterion.IsMutationsCriterion, BooleanEnum> booleanEnum = new TypedProperty<>(FlightEventCriterion.IsMutationsCriterion.class, "booleanEnum");
      public TypedProperty<FlightEventCriterion.IsMutationsCriterion, String> displayName = new TypedProperty<>(FlightEventCriterion.IsMutationsCriterion.class, "displayName");
      public TypedProperty<FlightEventCriterion.IsMutationsCriterion, StandardSearchOperator> operator = new TypedProperty<>(FlightEventCriterion.IsMutationsCriterion.class, "operator");
      public TypedProperty<FlightEventCriterion.IsMutationsCriterion, String> targetPropertyName = new TypedProperty<>(FlightEventCriterion.IsMutationsCriterion.class, "targetPropertyName");
      public TypedProperty<FlightEventCriterion.IsMutationsCriterion, BooleanEnum> value = new TypedProperty<>(FlightEventCriterion.IsMutationsCriterion.class, "value");
      public TypedProperty<FlightEventCriterion.IsMutationsCriterion, Boolean> withNull = new TypedProperty<>(FlightEventCriterion.IsMutationsCriterion.class, "withNull");
      public static class InstanceProperties extends InstanceProperty.Container<FlightEventCriterion.IsMutationsCriterion> {
        public  InstanceProperties(FlightEventCriterion.IsMutationsCriterion source){super(source);}
        public InstanceProperty<FlightEventCriterion.IsMutationsCriterion, BooleanEnum> booleanEnum(){return new InstanceProperty<>(source,PackageProperties.flightEventCriterion_isMutationsCriterion.booleanEnum);}
        public InstanceProperty<FlightEventCriterion.IsMutationsCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.flightEventCriterion_isMutationsCriterion.displayName);}
        public InstanceProperty<FlightEventCriterion.IsMutationsCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.flightEventCriterion_isMutationsCriterion.operator);}
        public InstanceProperty<FlightEventCriterion.IsMutationsCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.flightEventCriterion_isMutationsCriterion.targetPropertyName);}
        public InstanceProperty<FlightEventCriterion.IsMutationsCriterion, BooleanEnum> value(){return new InstanceProperty<>(source,PackageProperties.flightEventCriterion_isMutationsCriterion.value);}
        public InstanceProperty<FlightEventCriterion.IsMutationsCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.flightEventCriterion_isMutationsCriterion.withNull);}
      }
      
      public  InstanceProperties instance(FlightEventCriterion.IsMutationsCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
