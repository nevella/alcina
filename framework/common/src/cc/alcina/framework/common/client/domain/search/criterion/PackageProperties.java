package cc.alcina.framework.common.client.domain.search.criterion;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import java.lang.String;
import java.util.Date;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _CreatedFromCriterion createdFromCriterion = new _CreatedFromCriterion();
    public static _CreatedToCriterion createdToCriterion = new _CreatedToCriterion();
    public static _EndDateCriterion endDateCriterion = new _EndDateCriterion();
    public static _FinishedFromCriterion finishedFromCriterion = new _FinishedFromCriterion();
    public static _FinishedToCriterion finishedToCriterion = new _FinishedToCriterion();
    public static _ModifiedFromCriterion modifiedFromCriterion = new _ModifiedFromCriterion();
    public static _ModifiedToCriterion modifiedToCriterion = new _ModifiedToCriterion();
    public static _StartDateCriterion startDateCriterion = new _StartDateCriterion();
    
    public static class _CreatedFromCriterion implements TypedProperty.Container {
      public TypedProperty<CreatedFromCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(CreatedFromCriterion.class, "direction");
      public TypedProperty<CreatedFromCriterion, String> displayName = new TypedProperty<>(CreatedFromCriterion.class, "displayName");
      public TypedProperty<CreatedFromCriterion, StandardSearchOperator> operator = new TypedProperty<>(CreatedFromCriterion.class, "operator");
      public TypedProperty<CreatedFromCriterion, String> targetPropertyName = new TypedProperty<>(CreatedFromCriterion.class, "targetPropertyName");
      public TypedProperty<CreatedFromCriterion, Date> value = new TypedProperty<>(CreatedFromCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<CreatedFromCriterion> {
        public  InstanceProperties(CreatedFromCriterion source){super(source);}
        public InstanceProperty<CreatedFromCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.createdFromCriterion.direction);}
        public InstanceProperty<CreatedFromCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.createdFromCriterion.displayName);}
        public InstanceProperty<CreatedFromCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.createdFromCriterion.operator);}
        public InstanceProperty<CreatedFromCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.createdFromCriterion.targetPropertyName);}
        public InstanceProperty<CreatedFromCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.createdFromCriterion.value);}
      }
      
      public  InstanceProperties instance(CreatedFromCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _CreatedToCriterion implements TypedProperty.Container {
      public TypedProperty<CreatedToCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(CreatedToCriterion.class, "direction");
      public TypedProperty<CreatedToCriterion, String> displayName = new TypedProperty<>(CreatedToCriterion.class, "displayName");
      public TypedProperty<CreatedToCriterion, StandardSearchOperator> operator = new TypedProperty<>(CreatedToCriterion.class, "operator");
      public TypedProperty<CreatedToCriterion, String> targetPropertyName = new TypedProperty<>(CreatedToCriterion.class, "targetPropertyName");
      public TypedProperty<CreatedToCriterion, Date> value = new TypedProperty<>(CreatedToCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<CreatedToCriterion> {
        public  InstanceProperties(CreatedToCriterion source){super(source);}
        public InstanceProperty<CreatedToCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.createdToCriterion.direction);}
        public InstanceProperty<CreatedToCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.createdToCriterion.displayName);}
        public InstanceProperty<CreatedToCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.createdToCriterion.operator);}
        public InstanceProperty<CreatedToCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.createdToCriterion.targetPropertyName);}
        public InstanceProperty<CreatedToCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.createdToCriterion.value);}
      }
      
      public  InstanceProperties instance(CreatedToCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _EndDateCriterion implements TypedProperty.Container {
      public TypedProperty<EndDateCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(EndDateCriterion.class, "direction");
      public TypedProperty<EndDateCriterion, String> displayName = new TypedProperty<>(EndDateCriterion.class, "displayName");
      public TypedProperty<EndDateCriterion, StandardSearchOperator> operator = new TypedProperty<>(EndDateCriterion.class, "operator");
      public TypedProperty<EndDateCriterion, String> targetPropertyName = new TypedProperty<>(EndDateCriterion.class, "targetPropertyName");
      public TypedProperty<EndDateCriterion, Date> value = new TypedProperty<>(EndDateCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<EndDateCriterion> {
        public  InstanceProperties(EndDateCriterion source){super(source);}
        public InstanceProperty<EndDateCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.endDateCriterion.direction);}
        public InstanceProperty<EndDateCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.endDateCriterion.displayName);}
        public InstanceProperty<EndDateCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.endDateCriterion.operator);}
        public InstanceProperty<EndDateCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.endDateCriterion.targetPropertyName);}
        public InstanceProperty<EndDateCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.endDateCriterion.value);}
      }
      
      public  InstanceProperties instance(EndDateCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _FinishedFromCriterion implements TypedProperty.Container {
      public TypedProperty<FinishedFromCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(FinishedFromCriterion.class, "direction");
      public TypedProperty<FinishedFromCriterion, String> displayName = new TypedProperty<>(FinishedFromCriterion.class, "displayName");
      public TypedProperty<FinishedFromCriterion, StandardSearchOperator> operator = new TypedProperty<>(FinishedFromCriterion.class, "operator");
      public TypedProperty<FinishedFromCriterion, String> targetPropertyName = new TypedProperty<>(FinishedFromCriterion.class, "targetPropertyName");
      public TypedProperty<FinishedFromCriterion, Date> value = new TypedProperty<>(FinishedFromCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<FinishedFromCriterion> {
        public  InstanceProperties(FinishedFromCriterion source){super(source);}
        public InstanceProperty<FinishedFromCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.finishedFromCriterion.direction);}
        public InstanceProperty<FinishedFromCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.finishedFromCriterion.displayName);}
        public InstanceProperty<FinishedFromCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.finishedFromCriterion.operator);}
        public InstanceProperty<FinishedFromCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.finishedFromCriterion.targetPropertyName);}
        public InstanceProperty<FinishedFromCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.finishedFromCriterion.value);}
      }
      
      public  InstanceProperties instance(FinishedFromCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _FinishedToCriterion implements TypedProperty.Container {
      public TypedProperty<FinishedToCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(FinishedToCriterion.class, "direction");
      public TypedProperty<FinishedToCriterion, String> displayName = new TypedProperty<>(FinishedToCriterion.class, "displayName");
      public TypedProperty<FinishedToCriterion, StandardSearchOperator> operator = new TypedProperty<>(FinishedToCriterion.class, "operator");
      public TypedProperty<FinishedToCriterion, String> targetPropertyName = new TypedProperty<>(FinishedToCriterion.class, "targetPropertyName");
      public TypedProperty<FinishedToCriterion, Date> value = new TypedProperty<>(FinishedToCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<FinishedToCriterion> {
        public  InstanceProperties(FinishedToCriterion source){super(source);}
        public InstanceProperty<FinishedToCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.finishedToCriterion.direction);}
        public InstanceProperty<FinishedToCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.finishedToCriterion.displayName);}
        public InstanceProperty<FinishedToCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.finishedToCriterion.operator);}
        public InstanceProperty<FinishedToCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.finishedToCriterion.targetPropertyName);}
        public InstanceProperty<FinishedToCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.finishedToCriterion.value);}
      }
      
      public  InstanceProperties instance(FinishedToCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _ModifiedFromCriterion implements TypedProperty.Container {
      public TypedProperty<ModifiedFromCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(ModifiedFromCriterion.class, "direction");
      public TypedProperty<ModifiedFromCriterion, String> displayName = new TypedProperty<>(ModifiedFromCriterion.class, "displayName");
      public TypedProperty<ModifiedFromCriterion, StandardSearchOperator> operator = new TypedProperty<>(ModifiedFromCriterion.class, "operator");
      public TypedProperty<ModifiedFromCriterion, String> targetPropertyName = new TypedProperty<>(ModifiedFromCriterion.class, "targetPropertyName");
      public TypedProperty<ModifiedFromCriterion, Date> value = new TypedProperty<>(ModifiedFromCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<ModifiedFromCriterion> {
        public  InstanceProperties(ModifiedFromCriterion source){super(source);}
        public InstanceProperty<ModifiedFromCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.modifiedFromCriterion.direction);}
        public InstanceProperty<ModifiedFromCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.modifiedFromCriterion.displayName);}
        public InstanceProperty<ModifiedFromCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.modifiedFromCriterion.operator);}
        public InstanceProperty<ModifiedFromCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.modifiedFromCriterion.targetPropertyName);}
        public InstanceProperty<ModifiedFromCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.modifiedFromCriterion.value);}
      }
      
      public  InstanceProperties instance(ModifiedFromCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _ModifiedToCriterion implements TypedProperty.Container {
      public TypedProperty<ModifiedToCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(ModifiedToCriterion.class, "direction");
      public TypedProperty<ModifiedToCriterion, String> displayName = new TypedProperty<>(ModifiedToCriterion.class, "displayName");
      public TypedProperty<ModifiedToCriterion, StandardSearchOperator> operator = new TypedProperty<>(ModifiedToCriterion.class, "operator");
      public TypedProperty<ModifiedToCriterion, String> targetPropertyName = new TypedProperty<>(ModifiedToCriterion.class, "targetPropertyName");
      public TypedProperty<ModifiedToCriterion, Date> value = new TypedProperty<>(ModifiedToCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<ModifiedToCriterion> {
        public  InstanceProperties(ModifiedToCriterion source){super(source);}
        public InstanceProperty<ModifiedToCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.modifiedToCriterion.direction);}
        public InstanceProperty<ModifiedToCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.modifiedToCriterion.displayName);}
        public InstanceProperty<ModifiedToCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.modifiedToCriterion.operator);}
        public InstanceProperty<ModifiedToCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.modifiedToCriterion.targetPropertyName);}
        public InstanceProperty<ModifiedToCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.modifiedToCriterion.value);}
      }
      
      public  InstanceProperties instance(ModifiedToCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _StartDateCriterion implements TypedProperty.Container {
      public TypedProperty<StartDateCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(StartDateCriterion.class, "direction");
      public TypedProperty<StartDateCriterion, String> displayName = new TypedProperty<>(StartDateCriterion.class, "displayName");
      public TypedProperty<StartDateCriterion, StandardSearchOperator> operator = new TypedProperty<>(StartDateCriterion.class, "operator");
      public TypedProperty<StartDateCriterion, String> targetPropertyName = new TypedProperty<>(StartDateCriterion.class, "targetPropertyName");
      public TypedProperty<StartDateCriterion, Date> value = new TypedProperty<>(StartDateCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<StartDateCriterion> {
        public  InstanceProperties(StartDateCriterion source){super(source);}
        public InstanceProperty<StartDateCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.startDateCriterion.direction);}
        public InstanceProperty<StartDateCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.startDateCriterion.displayName);}
        public InstanceProperty<StartDateCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.startDateCriterion.operator);}
        public InstanceProperty<StartDateCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.startDateCriterion.targetPropertyName);}
        public InstanceProperty<StartDateCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.startDateCriterion.value);}
      }
      
      public  InstanceProperties instance(StartDateCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
