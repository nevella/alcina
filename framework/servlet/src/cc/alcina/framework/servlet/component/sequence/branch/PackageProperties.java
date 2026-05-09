package cc.alcina.framework.servlet.component.sequence.branch;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.search.BooleanEnum;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNode;
import cc.alcina.framework.servlet.component.sequence.branch.BranchingParserNodeCriterion;
import java.lang.Boolean;
import java.lang.String;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _BranchingParserNodeCriterion_BranchMinDepthCriterion branchingParserNodeCriterion_branchMinDepthCriterion = new _BranchingParserNodeCriterion_BranchMinDepthCriterion();
    static _BranchingParserNodeCriterion_GroupTypeCriterion branchingParserNodeCriterion_groupTypeCriterion = new _BranchingParserNodeCriterion_GroupTypeCriterion();
    static _BranchingParserNodeCriterion_MatchTypeCriterion branchingParserNodeCriterion_matchTypeCriterion = new _BranchingParserNodeCriterion_MatchTypeCriterion();
    static _BranchingParserNodeCriterion_NameDepthCriterion branchingParserNodeCriterion_nameDepthCriterion = new _BranchingParserNodeCriterion_NameDepthCriterion();
    static _BranchingParserNodeCriterion_OncePerToplevelToken branchingParserNodeCriterion_oncePerToplevelToken = new _BranchingParserNodeCriterion_OncePerToplevelToken();
    static _BranchingParserNodeCriterion_TermDistance branchingParserNodeCriterion_termDistance = new _BranchingParserNodeCriterion_TermDistance();
    static _BranchingParserNodeCriterion_TextDistanceCriterion branchingParserNodeCriterion_textDistanceCriterion = new _BranchingParserNodeCriterion_TextDistanceCriterion();
    static _BranchingParserNodeCriterion_TokenDistanceCriterion branchingParserNodeCriterion_tokenDistanceCriterion = new _BranchingParserNodeCriterion_TokenDistanceCriterion();
    public static _TermDistanceEditor termDistanceEditor = new _TermDistanceEditor();
    
    static class _BranchingParserNodeCriterion_BranchMinDepthCriterion implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.BranchMinDepthCriterion.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.BranchMinDepthCriterion.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.BranchMinDepthCriterion.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, BranchingParserNodeCriterion.Depth> value = new TypedProperty<>(BranchingParserNodeCriterion.BranchMinDepthCriterion.class, "value");
      TypedProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, Boolean> withNull = new TypedProperty<>(BranchingParserNodeCriterion.BranchMinDepthCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.BranchMinDepthCriterion> {
         InstanceProperties(BranchingParserNodeCriterion.BranchMinDepthCriterion source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_branchMinDepthCriterion.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_branchMinDepthCriterion.operator);}
        InstanceProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_branchMinDepthCriterion.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, BranchingParserNodeCriterion.Depth> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_branchMinDepthCriterion.value);}
        InstanceProperty<BranchingParserNodeCriterion.BranchMinDepthCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_branchMinDepthCriterion.withNull);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.BranchMinDepthCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
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
    
    static class _BranchingParserNodeCriterion_NameDepthCriterion implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.NameDepthCriterion, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.NameDepthCriterion.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.NameDepthCriterion, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.NameDepthCriterion.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.NameDepthCriterion, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.NameDepthCriterion.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.NameDepthCriterion, BranchingParserNodeCriterion.Depth> value = new TypedProperty<>(BranchingParserNodeCriterion.NameDepthCriterion.class, "value");
      TypedProperty<BranchingParserNodeCriterion.NameDepthCriterion, Boolean> withNull = new TypedProperty<>(BranchingParserNodeCriterion.NameDepthCriterion.class, "withNull");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.NameDepthCriterion> {
         InstanceProperties(BranchingParserNodeCriterion.NameDepthCriterion source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.NameDepthCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_nameDepthCriterion.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.NameDepthCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_nameDepthCriterion.operator);}
        InstanceProperty<BranchingParserNodeCriterion.NameDepthCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_nameDepthCriterion.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.NameDepthCriterion, BranchingParserNodeCriterion.Depth> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_nameDepthCriterion.value);}
        InstanceProperty<BranchingParserNodeCriterion.NameDepthCriterion, Boolean> withNull(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_nameDepthCriterion.withNull);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.NameDepthCriterion instance) {
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
    
    static class _BranchingParserNodeCriterion_TermDistance implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.TermDistance, BranchingParserNodeCriterion.Depth> distance = new TypedProperty<>(BranchingParserNodeCriterion.TermDistance.class, "distance");
      TypedProperty<BranchingParserNodeCriterion.TermDistance, String> text = new TypedProperty<>(BranchingParserNodeCriterion.TermDistance.class, "text");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.TermDistance> {
         InstanceProperties(BranchingParserNodeCriterion.TermDistance source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.TermDistance, BranchingParserNodeCriterion.Depth> distance(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_termDistance.distance);}
        InstanceProperty<BranchingParserNodeCriterion.TermDistance, String> text(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_termDistance.text);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.TermDistance instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _BranchingParserNodeCriterion_TextDistanceCriterion implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.TextDistanceCriterion, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.TextDistanceCriterion.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.TextDistanceCriterion, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.TextDistanceCriterion.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.TextDistanceCriterion, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.TextDistanceCriterion.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.TextDistanceCriterion, BranchingParserNodeCriterion.TermDistance> value = new TypedProperty<>(BranchingParserNodeCriterion.TextDistanceCriterion.class, "value");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.TextDistanceCriterion> {
         InstanceProperties(BranchingParserNodeCriterion.TextDistanceCriterion source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.TextDistanceCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_textDistanceCriterion.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.TextDistanceCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_textDistanceCriterion.operator);}
        InstanceProperty<BranchingParserNodeCriterion.TextDistanceCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_textDistanceCriterion.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.TextDistanceCriterion, BranchingParserNodeCriterion.TermDistance> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_textDistanceCriterion.value);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.TextDistanceCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    static class _BranchingParserNodeCriterion_TokenDistanceCriterion implements TypedProperty.Container {
      TypedProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, String> displayName = new TypedProperty<>(BranchingParserNodeCriterion.TokenDistanceCriterion.class, "displayName");
      TypedProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, StandardSearchOperator> operator = new TypedProperty<>(BranchingParserNodeCriterion.TokenDistanceCriterion.class, "operator");
      TypedProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, String> targetPropertyName = new TypedProperty<>(BranchingParserNodeCriterion.TokenDistanceCriterion.class, "targetPropertyName");
      TypedProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, BranchingParserNodeCriterion.TermDistance> value = new TypedProperty<>(BranchingParserNodeCriterion.TokenDistanceCriterion.class, "value");
      static class InstanceProperties extends 	InstanceProperty.Container<BranchingParserNodeCriterion.TokenDistanceCriterion> {
         InstanceProperties(BranchingParserNodeCriterion.TokenDistanceCriterion source){super(source);}
        InstanceProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, String> displayName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_tokenDistanceCriterion.displayName);}
        InstanceProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, StandardSearchOperator> operator(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_tokenDistanceCriterion.operator);}
        InstanceProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, String> targetPropertyName(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_tokenDistanceCriterion.targetPropertyName);}
        InstanceProperty<BranchingParserNodeCriterion.TokenDistanceCriterion, BranchingParserNodeCriterion.TermDistance> value(){return new InstanceProperty<>(source,PackageProperties.branchingParserNodeCriterion_tokenDistanceCriterion.value);}
      }
      
       InstanceProperties instance(BranchingParserNodeCriterion.TokenDistanceCriterion instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _TermDistanceEditor implements TypedProperty.Container {
      public TypedProperty<TermDistanceEditor, BranchingParserNodeCriterion.Depth> distance = new TypedProperty<>(TermDistanceEditor.class, "distance");
      public TypedProperty<TermDistanceEditor, String> text = new TypedProperty<>(TermDistanceEditor.class, "text");
      public TypedProperty<TermDistanceEditor, BranchingParserNodeCriterion.TermDistance> value = new TypedProperty<>(TermDistanceEditor.class, "value");
      public static class InstanceProperties extends 	InstanceProperty.Container<TermDistanceEditor> {
        public  InstanceProperties(TermDistanceEditor source){super(source);}
        public InstanceProperty<TermDistanceEditor, BranchingParserNodeCriterion.Depth> distance(){return new InstanceProperty<>(source,PackageProperties.termDistanceEditor.distance);}
        public InstanceProperty<TermDistanceEditor, String> text(){return new InstanceProperty<>(source,PackageProperties.termDistanceEditor.text);}
        public InstanceProperty<TermDistanceEditor, BranchingParserNodeCriterion.TermDistance> value(){return new InstanceProperty<>(source,PackageProperties.termDistanceEditor.value);}
      }
      
      public  InstanceProperties instance(TermDistanceEditor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
