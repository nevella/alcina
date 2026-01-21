package cc.alcina.framework.common.client.search;

import java.util.Date;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public class PackageProperties {
	// auto-generated, do not modify
	//@formatter:off
    
    public static _AbstractDateCriterion abstractDateCriterion = new _AbstractDateCriterion();
    public static _AbstractUserCriterion abstractUserCriterion = new _AbstractUserCriterion();
    public static _BaseEnumCriterion baseEnumCriterion = new _BaseEnumCriterion();
    public static _BooleanEnumCriterion booleanEnumCriterion = new _BooleanEnumCriterion();
    public static _DateCriterion dateCriterion = new _DateCriterion();
    public static _DateRangeEnumCriterion dateRangeEnumCriterion = new _DateRangeEnumCriterion();
    public static _DoubleCriterion doubleCriterion = new _DoubleCriterion();
    public static _EntityCriterion entityCriterion = new _EntityCriterion();
    public static _EnumCriterion enumCriterion = new _EnumCriterion();
    public static _EnumMultipleCriterion enumMultipleCriterion = new _EnumMultipleCriterion();
    public static _IdMultipleCriterion idMultipleCriterion = new _IdMultipleCriterion();
    public static _LongCriterion longCriterion = new _LongCriterion();
    public static _OrderCriterion orderCriterion = new _OrderCriterion();
    public static _PersistentObjectCriterion persistentObjectCriterion = new _PersistentObjectCriterion();
    public static _SearchCriterion searchCriterion = new _SearchCriterion();
    public static _TextCriterion textCriterion = new _TextCriterion();
    
    public static class _AbstractDateCriterion implements TypedProperty.Container {
      public TypedProperty<AbstractDateCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(AbstractDateCriterion.class, "direction");
      public TypedProperty<AbstractDateCriterion, String> displayName = new TypedProperty<>(AbstractDateCriterion.class, "displayName");
      public TypedProperty<AbstractDateCriterion, StandardSearchOperator> operator = new TypedProperty<>(AbstractDateCriterion.class, "operator");
      public TypedProperty<AbstractDateCriterion, String> targetPropertyName = new TypedProperty<>(AbstractDateCriterion.class, "targetPropertyName");
      public TypedProperty<AbstractDateCriterion, Date> value = new TypedProperty<>(AbstractDateCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<AbstractDateCriterion> {
        public  InstanceProperties(AbstractDateCriterion source){super(source);}
        public InstanceProperty<AbstractDateCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.abstractDateCriterion.direction);}
        public InstanceProperty<AbstractDateCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.abstractDateCriterion.displayName);}
        public InstanceProperty<AbstractDateCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.abstractDateCriterion.operator);}
        public InstanceProperty<AbstractDateCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.abstractDateCriterion.targetPropertyName);}
        public InstanceProperty<AbstractDateCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.abstractDateCriterion.value);}
      }
      
      public  InstanceProperties instance(AbstractDateCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _AbstractUserCriterion implements TypedProperty.Container {
      public TypedProperty<AbstractUserCriterion, String> displayName = new TypedProperty<>(AbstractUserCriterion.class, "displayName");
      public TypedProperty<AbstractUserCriterion, StandardSearchOperator> operator = new TypedProperty<>(AbstractUserCriterion.class, "operator");
      public TypedProperty<AbstractUserCriterion, String> targetPropertyName = new TypedProperty<>(AbstractUserCriterion.class, "targetPropertyName");
      public TypedProperty<AbstractUserCriterion, Long> userId = new TypedProperty<>(AbstractUserCriterion.class, "userId");
      public TypedProperty<AbstractUserCriterion, Long> value = new TypedProperty<>(AbstractUserCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<AbstractUserCriterion> {
        public  InstanceProperties(AbstractUserCriterion source){super(source);}
        public InstanceProperty<AbstractUserCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.abstractUserCriterion.displayName);}
        public InstanceProperty<AbstractUserCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.abstractUserCriterion.operator);}
        public InstanceProperty<AbstractUserCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.abstractUserCriterion.targetPropertyName);}
        public InstanceProperty<AbstractUserCriterion, Long> userId(){return new InstanceProperty<>(source,PackageProperties.abstractUserCriterion.userId);}
        public InstanceProperty<AbstractUserCriterion, Long> value(){return new InstanceProperty<>(source,PackageProperties.abstractUserCriterion.value);}
      }
      
      public  InstanceProperties instance(AbstractUserCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _BaseEnumCriterion implements TypedProperty.Container {
      public TypedProperty<BaseEnumCriterion, String> displayName = new TypedProperty<>(BaseEnumCriterion.class, "displayName");
      public TypedProperty<BaseEnumCriterion, StandardSearchOperator> operator = new TypedProperty<>(BaseEnumCriterion.class, "operator");
      public TypedProperty<BaseEnumCriterion, String> targetPropertyName = new TypedProperty<>(BaseEnumCriterion.class, "targetPropertyName");
      public TypedProperty<BaseEnumCriterion, Enum> value = new TypedProperty<>(BaseEnumCriterion.class, "value");
      public TypedProperty<BaseEnumCriterion, Boolean> withNull = new TypedProperty<>(BaseEnumCriterion.class, "withNull");
      public static class InstanceProperties extends InstanceProperty.Container<BaseEnumCriterion> {
        public  InstanceProperties(BaseEnumCriterion source){super(source);}
        public InstanceProperty<BaseEnumCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.baseEnumCriterion.displayName);}
        public InstanceProperty<BaseEnumCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.baseEnumCriterion.operator);}
        public InstanceProperty<BaseEnumCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.baseEnumCriterion.targetPropertyName);}
        public InstanceProperty<BaseEnumCriterion, Enum> value(){return new InstanceProperty<>(source,PackageProperties.baseEnumCriterion.value);}
        public InstanceProperty<BaseEnumCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.baseEnumCriterion.withNull);}
      }
      
      public  InstanceProperties instance(BaseEnumCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _BooleanEnumCriterion implements TypedProperty.Container {
      public TypedProperty<BooleanEnumCriterion, BooleanEnum> booleanEnum = new TypedProperty<>(BooleanEnumCriterion.class, "booleanEnum");
      public TypedProperty<BooleanEnumCriterion, String> displayName = new TypedProperty<>(BooleanEnumCriterion.class, "displayName");
      public TypedProperty<BooleanEnumCriterion, StandardSearchOperator> operator = new TypedProperty<>(BooleanEnumCriterion.class, "operator");
      public TypedProperty<BooleanEnumCriterion, String> targetPropertyName = new TypedProperty<>(BooleanEnumCriterion.class, "targetPropertyName");
      public TypedProperty<BooleanEnumCriterion, BooleanEnum> value = new TypedProperty<>(BooleanEnumCriterion.class, "value");
      public TypedProperty<BooleanEnumCriterion, Boolean> withNull = new TypedProperty<>(BooleanEnumCriterion.class, "withNull");
      public static class InstanceProperties extends InstanceProperty.Container<BooleanEnumCriterion> {
        public  InstanceProperties(BooleanEnumCriterion source){super(source);}
        public InstanceProperty<BooleanEnumCriterion, BooleanEnum> booleanEnum(){return new InstanceProperty<>(source,PackageProperties.booleanEnumCriterion.booleanEnum);}
        public InstanceProperty<BooleanEnumCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.booleanEnumCriterion.displayName);}
        public InstanceProperty<BooleanEnumCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.booleanEnumCriterion.operator);}
        public InstanceProperty<BooleanEnumCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.booleanEnumCriterion.targetPropertyName);}
        public InstanceProperty<BooleanEnumCriterion, BooleanEnum> value(){return new InstanceProperty<>(source,PackageProperties.booleanEnumCriterion.value);}
        public InstanceProperty<BooleanEnumCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.booleanEnumCriterion.withNull);}
      }
      
      public  InstanceProperties instance(BooleanEnumCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DateCriterion implements TypedProperty.Container {
      public TypedProperty<DateCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(DateCriterion.class, "direction");
      public TypedProperty<DateCriterion, String> displayName = new TypedProperty<>(DateCriterion.class, "displayName");
      public TypedProperty<DateCriterion, StandardSearchOperator> operator = new TypedProperty<>(DateCriterion.class, "operator");
      public TypedProperty<DateCriterion, String> targetPropertyName = new TypedProperty<>(DateCriterion.class, "targetPropertyName");
      public TypedProperty<DateCriterion, Date> value = new TypedProperty<>(DateCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<DateCriterion> {
        public  InstanceProperties(DateCriterion source){super(source);}
        public InstanceProperty<DateCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.dateCriterion.direction);}
        public InstanceProperty<DateCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.dateCriterion.displayName);}
        public InstanceProperty<DateCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.dateCriterion.operator);}
        public InstanceProperty<DateCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.dateCriterion.targetPropertyName);}
        public InstanceProperty<DateCriterion, Date> value(){return new InstanceProperty<>(source,PackageProperties.dateCriterion.value);}
      }
      
      public  InstanceProperties instance(DateCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DateRangeEnumCriterion implements TypedProperty.Container {
      public TypedProperty<DateRangeEnumCriterion, DateRange> dateRange = new TypedProperty<>(DateRangeEnumCriterion.class, "dateRange");
      public TypedProperty<DateRangeEnumCriterion, String> displayName = new TypedProperty<>(DateRangeEnumCriterion.class, "displayName");
      public TypedProperty<DateRangeEnumCriterion, StandardSearchOperator> operator = new TypedProperty<>(DateRangeEnumCriterion.class, "operator");
      public TypedProperty<DateRangeEnumCriterion, String> targetPropertyName = new TypedProperty<>(DateRangeEnumCriterion.class, "targetPropertyName");
      public TypedProperty<DateRangeEnumCriterion, DateRange> value = new TypedProperty<>(DateRangeEnumCriterion.class, "value");
      public TypedProperty<DateRangeEnumCriterion, Boolean> withNull = new TypedProperty<>(DateRangeEnumCriterion.class, "withNull");
      public static class InstanceProperties extends InstanceProperty.Container<DateRangeEnumCriterion> {
        public  InstanceProperties(DateRangeEnumCriterion source){super(source);}
        public InstanceProperty<DateRangeEnumCriterion, DateRange> dateRange(){return new InstanceProperty<>(source,PackageProperties.dateRangeEnumCriterion.dateRange);}
        public InstanceProperty<DateRangeEnumCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.dateRangeEnumCriterion.displayName);}
        public InstanceProperty<DateRangeEnumCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.dateRangeEnumCriterion.operator);}
        public InstanceProperty<DateRangeEnumCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.dateRangeEnumCriterion.targetPropertyName);}
        public InstanceProperty<DateRangeEnumCriterion, DateRange> value(){return new InstanceProperty<>(source,PackageProperties.dateRangeEnumCriterion.value);}
        public InstanceProperty<DateRangeEnumCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.dateRangeEnumCriterion.withNull);}
      }
      
      public  InstanceProperties instance(DateRangeEnumCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _DoubleCriterion implements TypedProperty.Container {
      public TypedProperty<DoubleCriterion, String> displayName = new TypedProperty<>(DoubleCriterion.class, "displayName");
      public TypedProperty<DoubleCriterion, StandardSearchOperator> operator = new TypedProperty<>(DoubleCriterion.class, "operator");
      public TypedProperty<DoubleCriterion, String> targetPropertyName = new TypedProperty<>(DoubleCriterion.class, "targetPropertyName");
      public TypedProperty<DoubleCriterion, Double> value = new TypedProperty<>(DoubleCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<DoubleCriterion> {
        public  InstanceProperties(DoubleCriterion source){super(source);}
        public InstanceProperty<DoubleCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.doubleCriterion.displayName);}
        public InstanceProperty<DoubleCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.doubleCriterion.operator);}
        public InstanceProperty<DoubleCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.doubleCriterion.targetPropertyName);}
        public InstanceProperty<DoubleCriterion, Double> value(){return new InstanceProperty<>(source,PackageProperties.doubleCriterion.value);}
      }
      
      public  InstanceProperties instance(DoubleCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _EntityCriterion implements TypedProperty.Container {
      public TypedProperty<EntityCriterion, String> displayName = new TypedProperty<>(EntityCriterion.class, "displayName");
      public TypedProperty<EntityCriterion, String> displayText = new TypedProperty<>(EntityCriterion.class, "displayText");
      public TypedProperty<EntityCriterion, Long> id = new TypedProperty<>(EntityCriterion.class, "id");
      public TypedProperty<EntityCriterion, Class> objectClass = new TypedProperty<>(EntityCriterion.class, "objectClass");
      public TypedProperty<EntityCriterion, StandardSearchOperator> operator = new TypedProperty<>(EntityCriterion.class, "operator");
      public TypedProperty<EntityCriterion, String> targetPropertyName = new TypedProperty<>(EntityCriterion.class, "targetPropertyName");
      public TypedProperty<EntityCriterion, HasId> value = new TypedProperty<>(EntityCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<EntityCriterion> {
        public  InstanceProperties(EntityCriterion source){super(source);}
        public InstanceProperty<EntityCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.displayName);}
        public InstanceProperty<EntityCriterion, String> displayText(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.displayText);}
        public InstanceProperty<EntityCriterion, Long> id(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.id);}
        public InstanceProperty<EntityCriterion, Class> objectClass(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.objectClass);}
        public InstanceProperty<EntityCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.operator);}
        public InstanceProperty<EntityCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.targetPropertyName);}
        public InstanceProperty<EntityCriterion, HasId> value(){return new InstanceProperty<>(source,PackageProperties.entityCriterion.value);}
      }
      
      public  InstanceProperties instance(EntityCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _EnumCriterion implements TypedProperty.Container {
      public TypedProperty<EnumCriterion, String> displayName = new TypedProperty<>(EnumCriterion.class, "displayName");
      public TypedProperty<EnumCriterion, StandardSearchOperator> operator = new TypedProperty<>(EnumCriterion.class, "operator");
      public TypedProperty<EnumCriterion, String> targetPropertyName = new TypedProperty<>(EnumCriterion.class, "targetPropertyName");
      public TypedProperty<EnumCriterion, Enum> value = new TypedProperty<>(EnumCriterion.class, "value");
      public TypedProperty<EnumCriterion, Boolean> withNull = new TypedProperty<>(EnumCriterion.class, "withNull");
      public static class InstanceProperties extends InstanceProperty.Container<EnumCriterion> {
        public  InstanceProperties(EnumCriterion source){super(source);}
        public InstanceProperty<EnumCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.enumCriterion.displayName);}
        public InstanceProperty<EnumCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.enumCriterion.operator);}
        public InstanceProperty<EnumCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.enumCriterion.targetPropertyName);}
        public InstanceProperty<EnumCriterion, Enum> value(){return new InstanceProperty<>(source,PackageProperties.enumCriterion.value);}
        public InstanceProperty<EnumCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.enumCriterion.withNull);}
      }
      
      public  InstanceProperties instance(EnumCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _EnumMultipleCriterion implements TypedProperty.Container {
      public TypedProperty<EnumMultipleCriterion, String> displayName = new TypedProperty<>(EnumMultipleCriterion.class, "displayName");
      public TypedProperty<EnumMultipleCriterion, StandardSearchOperator> operator = new TypedProperty<>(EnumMultipleCriterion.class, "operator");
      public TypedProperty<EnumMultipleCriterion, String> targetPropertyName = new TypedProperty<>(EnumMultipleCriterion.class, "targetPropertyName");
      public TypedProperty<EnumMultipleCriterion, Set> value = new TypedProperty<>(EnumMultipleCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<EnumMultipleCriterion> {
        public  InstanceProperties(EnumMultipleCriterion source){super(source);}
        public InstanceProperty<EnumMultipleCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.enumMultipleCriterion.displayName);}
        public InstanceProperty<EnumMultipleCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.enumMultipleCriterion.operator);}
        public InstanceProperty<EnumMultipleCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.enumMultipleCriterion.targetPropertyName);}
        public InstanceProperty<EnumMultipleCriterion, Set> value(){return new InstanceProperty<>(source,PackageProperties.enumMultipleCriterion.value);}
      }
      
      public  InstanceProperties instance(EnumMultipleCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _IdMultipleCriterion implements TypedProperty.Container {
      public TypedProperty<IdMultipleCriterion, String> displayName = new TypedProperty<>(IdMultipleCriterion.class, "displayName");
      public TypedProperty<IdMultipleCriterion, Set> ids = new TypedProperty<>(IdMultipleCriterion.class, "ids");
      public TypedProperty<IdMultipleCriterion, StandardSearchOperator> operator = new TypedProperty<>(IdMultipleCriterion.class, "operator");
      public TypedProperty<IdMultipleCriterion, String> targetPropertyName = new TypedProperty<>(IdMultipleCriterion.class, "targetPropertyName");
      public TypedProperty<IdMultipleCriterion, Set> value = new TypedProperty<>(IdMultipleCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<IdMultipleCriterion> {
        public  InstanceProperties(IdMultipleCriterion source){super(source);}
        public InstanceProperty<IdMultipleCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.idMultipleCriterion.displayName);}
        public InstanceProperty<IdMultipleCriterion, Set> ids(){return new InstanceProperty<>(source,PackageProperties.idMultipleCriterion.ids);}
        public InstanceProperty<IdMultipleCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.idMultipleCriterion.operator);}
        public InstanceProperty<IdMultipleCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.idMultipleCriterion.targetPropertyName);}
        public InstanceProperty<IdMultipleCriterion, Set> value(){return new InstanceProperty<>(source,PackageProperties.idMultipleCriterion.value);}
      }
      
      public  InstanceProperties instance(IdMultipleCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _LongCriterion implements TypedProperty.Container {
      public TypedProperty<LongCriterion, String> displayName = new TypedProperty<>(LongCriterion.class, "displayName");
      public TypedProperty<LongCriterion, StandardSearchOperator> operator = new TypedProperty<>(LongCriterion.class, "operator");
      public TypedProperty<LongCriterion, String> targetPropertyName = new TypedProperty<>(LongCriterion.class, "targetPropertyName");
      public TypedProperty<LongCriterion, Long> value = new TypedProperty<>(LongCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<LongCriterion> {
        public  InstanceProperties(LongCriterion source){super(source);}
        public InstanceProperty<LongCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.longCriterion.displayName);}
        public InstanceProperty<LongCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.longCriterion.operator);}
        public InstanceProperty<LongCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.longCriterion.targetPropertyName);}
        public InstanceProperty<LongCriterion, Long> value(){return new InstanceProperty<>(source,PackageProperties.longCriterion.value);}
      }
      
      public  InstanceProperties instance(LongCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _OrderCriterion implements TypedProperty.Container {
      public TypedProperty<OrderCriterion, SearchCriterion.Direction> direction = new TypedProperty<>(OrderCriterion.class, "direction");
      public TypedProperty<OrderCriterion, String> displayName = new TypedProperty<>(OrderCriterion.class, "displayName");
      public TypedProperty<OrderCriterion, StandardSearchOperator> operator = new TypedProperty<>(OrderCriterion.class, "operator");
      public TypedProperty<OrderCriterion, String> targetPropertyName = new TypedProperty<>(OrderCriterion.class, "targetPropertyName");
      public static class InstanceProperties extends InstanceProperty.Container<OrderCriterion> {
        public  InstanceProperties(OrderCriterion source){super(source);}
        public InstanceProperty<OrderCriterion, SearchCriterion.Direction> direction(){return new InstanceProperty<>(source,PackageProperties.orderCriterion.direction);}
        public InstanceProperty<OrderCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.orderCriterion.displayName);}
        public InstanceProperty<OrderCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.orderCriterion.operator);}
        public InstanceProperty<OrderCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.orderCriterion.targetPropertyName);}
      }
      
      public  InstanceProperties instance(OrderCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _PersistentObjectCriterion implements TypedProperty.Container {
      public TypedProperty<PersistentObjectCriterion, ClassRef> classRef = new TypedProperty<>(PersistentObjectCriterion.class, "classRef");
      public TypedProperty<PersistentObjectCriterion, String> displayName = new TypedProperty<>(PersistentObjectCriterion.class, "displayName");
      public TypedProperty<PersistentObjectCriterion, StandardSearchOperator> operator = new TypedProperty<>(PersistentObjectCriterion.class, "operator");
      public TypedProperty<PersistentObjectCriterion, String> targetPropertyName = new TypedProperty<>(PersistentObjectCriterion.class, "targetPropertyName");
      public static class InstanceProperties extends InstanceProperty.Container<PersistentObjectCriterion> {
        public  InstanceProperties(PersistentObjectCriterion source){super(source);}
        public InstanceProperty<PersistentObjectCriterion, ClassRef> classRef(){return new InstanceProperty<>(source,PackageProperties.persistentObjectCriterion.classRef);}
        public InstanceProperty<PersistentObjectCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.persistentObjectCriterion.displayName);}
        public InstanceProperty<PersistentObjectCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.persistentObjectCriterion.operator);}
        public InstanceProperty<PersistentObjectCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.persistentObjectCriterion.targetPropertyName);}
      }
      
      public  InstanceProperties instance(PersistentObjectCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _SearchCriterion implements TypedProperty.Container {
      public TypedProperty<SearchCriterion, String> displayName = new TypedProperty<>(SearchCriterion.class, "displayName");
      public TypedProperty<SearchCriterion, StandardSearchOperator> operator = new TypedProperty<>(SearchCriterion.class, "operator");
      public TypedProperty<SearchCriterion, String> targetPropertyName = new TypedProperty<>(SearchCriterion.class, "targetPropertyName");
      public static class InstanceProperties extends InstanceProperty.Container<SearchCriterion> {
        public  InstanceProperties(SearchCriterion source){super(source);}
        public InstanceProperty<SearchCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.searchCriterion.displayName);}
        public InstanceProperty<SearchCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.searchCriterion.operator);}
        public InstanceProperty<SearchCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.searchCriterion.targetPropertyName);}
      }
      
      public  InstanceProperties instance(SearchCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _TextCriterion implements TypedProperty.Container {
      public TypedProperty<TextCriterion, String> displayName = new TypedProperty<>(TextCriterion.class, "displayName");
      public TypedProperty<TextCriterion, StandardSearchOperator> operator = new TypedProperty<>(TextCriterion.class, "operator");
      public TypedProperty<TextCriterion, String> targetPropertyName = new TypedProperty<>(TextCriterion.class, "targetPropertyName");
      public TypedProperty<TextCriterion, TextCriterion.TextCriterionType> textCriterionType = new TypedProperty<>(TextCriterion.class, "textCriterionType");
      public TypedProperty<TextCriterion, String> value = new TypedProperty<>(TextCriterion.class, "value");
      public static class InstanceProperties extends InstanceProperty.Container<TextCriterion> {
        public  InstanceProperties(TextCriterion source){super(source);}
        public InstanceProperty<TextCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.textCriterion.displayName);}
        public InstanceProperty<TextCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.textCriterion.operator);}
        public InstanceProperty<TextCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.textCriterion.targetPropertyName);}
        public InstanceProperty<TextCriterion, TextCriterion.TextCriterionType> textCriterionType(){return new InstanceProperty<>(source,PackageProperties.textCriterion.textCriterionType);}
        public InstanceProperty<TextCriterion, String> value(){return new InstanceProperty<>(source,PackageProperties.textCriterion.value);}
      }
      
      public  InstanceProperties instance(TextCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
