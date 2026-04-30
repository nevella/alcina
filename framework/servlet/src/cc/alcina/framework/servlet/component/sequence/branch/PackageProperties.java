package cc.alcina.framework.servlet.component.sequence.branch;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNode;
import java.lang.Boolean;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _BranchingParserNodeCriterion_GroupTypeCriterion branchingParserNodeCriterion_groupTypeCriterion = new _BranchingParserNodeCriterion_GroupTypeCriterion();
    static _BranchingParserNodeCriterion_MatchTypeCriterion branchingParserNodeCriterion_matchTypeCriterion = new _BranchingParserNodeCriterion_MatchTypeCriterion();
    static _BranchingParserNodeCriterion_OncePerToplevelToken branchingParserNodeCriterion_oncePerToplevelToken = new _BranchingParserNodeCriterion_OncePerToplevelToken();
    
    static class _BranchingParserNodeCriterion_GroupTypeCriterion implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.GroupTypeCriterion, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.GroupTypeCriterion.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.GroupTypeCriterion, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.GroupTypeCriterion.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.GroupTypeCriterion, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.GroupTypeCriterion.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.GroupTypeCriterion, BranchingParserNode.GroupType> value = new TypedProperty<>(BranchingParserNodeCriterion.GroupTypeCriterion.class, "value");
      TypedProperty<BranchingParserNodeCriterion.GroupTypeCriterion, Boolean> withNull = new TypedProperty<>(BranchingParserNodeCriterion.GroupTypeCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.GroupTypeCriterion> {
         InstanceProperties(BranchingParserNodeCriterion.GroupTypeCriterion source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.GroupTypeCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_groupTypeCriterion.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.GroupTypeCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_groupTypeCriterion.operator);}
        InstanceProperty<BranchingParserNodeCriterion.GroupTypeCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_groupTypeCriterion.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.GroupTypeCriterion, BranchingParserNode.GroupType> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_groupTypeCriterion.value);}
        InstanceProperty<BranchingParserNodeCriterion.GroupTypeCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_groupTypeCriterion.withNull);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.GroupTypeCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _BranchingParserNodeCriterion_MatchTypeCriterion implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.MatchTypeCriterion, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.MatchTypeCriterion.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.MatchTypeCriterion, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.MatchTypeCriterion.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.MatchTypeCriterion, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.MatchTypeCriterion.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.MatchTypeCriterion, BranchingParserNode.MatchType> value = new TypedProperty<>(BranchingParserNodeCriterion.MatchTypeCriterion.class, "value");
      TypedProperty<BranchingParserNodeCriterion.MatchTypeCriterion, Boolean> withNull = new TypedProperty<>(BranchingParserNodeCriterion.MatchTypeCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.MatchTypeCriterion> {
         InstanceProperties(BranchingParserNodeCriterion.MatchTypeCriterion source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.MatchTypeCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_matchTypeCriterion.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.MatchTypeCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_matchTypeCriterion.operator);}
        InstanceProperty<BranchingParserNodeCriterion.MatchTypeCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_matchTypeCriterion.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.MatchTypeCriterion, BranchingParserNode.MatchType> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_matchTypeCriterion.value);}
        InstanceProperty<BranchingParserNodeCriterion.MatchTypeCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_matchTypeCriterion.withNull);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.MatchTypeCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _BranchingParserNodeCriterion_OncePerToplevelToken implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.OncePerToplevelToken, BooleanEnum> booleanEnum = new TypedProperty<>(BranchingParserNodeCriterion.OncePerToplevelToken.class, "booleanEnum");
      TypedProperty<BranchingParserNodeCriterion.OncePerToplevelToken, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.OncePerToplevelToken.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.OncePerToplevelToken, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.OncePerToplevelToken.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.OncePerToplevelToken, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.OncePerToplevelToken.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.OncePerToplevelToken, BooleanEnum> value = new TypedProperty<>(BranchingParserNodeCriterion.OncePerToplevelToken.class, "value");
      TypedProperty<BranchingParserNodeCriterion.OncePerToplevelToken, Boolean> withNull = new TypedProperty<>(BranchingParserNodeCriterion.OncePerToplevelToken.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.OncePerToplevelToken> {
         InstanceProperties(BranchingParserNodeCriterion.OncePerToplevelToken source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.OncePerToplevelToken, BooleanEnum> booleanEnum(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_oncePerToplevelToken.booleanEnum);}
        InstanceProperty<BranchingParserNodeCriterion.OncePerToplevelToken, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_oncePerToplevelToken.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.OncePerToplevelToken, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_oncePerToplevelToken.operator);}
        InstanceProperty<BranchingParserNodeCriterion.OncePerToplevelToken, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_oncePerToplevelToken.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.OncePerToplevelToken, BooleanEnum> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_oncePerToplevelToken.value);}
        InstanceProperty<BranchingParserNodeCriterion.OncePerToplevelToken, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_oncePerToplevelToken.withNull);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.OncePerToplevelToken instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
