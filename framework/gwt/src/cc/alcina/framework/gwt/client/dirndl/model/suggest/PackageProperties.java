package cc.alcina.framework.gwt.client.dirndl.model.suggest;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;
import java.lang.Object;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _SuggestionChoices_Results suggestionChoices_results = new _SuggestionChoices_Results();
    public static _Suggestor suggestor = new _Suggestor();
    
    public static class _SuggestionChoices_Results implements TypedProperty.Container {
      public TypedProperty<SuggestionChoices.Results, Object> model = new TypedProperty<>(SuggestionChoices.Results.class, "model");
      public static class InstanceProperties extends 	InstanceProperty.Container<SuggestionChoices.Results> {
        public  InstanceProperties(SuggestionChoices.Results source){super(source);}
        public InstanceProperty<SuggestionChoices.Results, Object> model(){return new InstanceProperty<>(source,PackageProperties.suggestionChoices_results.model);}
      }
      
      public  InstanceProperties instance(SuggestionChoices.Results instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Suggestor implements TypedProperty.Container {
      public TypedProperty<Suggestor, Object> chosenSuggestions = new TypedProperty<>(Suggestor.class, "chosenSuggestions");
      public TypedProperty<Suggestor, Suggestor.Editor> editor = new TypedProperty<>(Suggestor.class, "editor");
      public TypedProperty<Suggestor, Object> nonOverlaySuggestionResults = new TypedProperty<>(Suggestor.class, "nonOverlaySuggestionResults");
      public TypedProperty<Suggestor, Suggestor.Suggestions> suggestions = new TypedProperty<>(Suggestor.class, "suggestions");
      public TypedProperty<Suggestor, Object> value = new TypedProperty<>(Suggestor.class, "value");
      public static class InstanceProperties extends 	InstanceProperty.Container<Suggestor> {
        public  InstanceProperties(Suggestor source){super(source);}
        public InstanceProperty<Suggestor, Object> chosenSuggestions(){return new InstanceProperty<>(source,PackageProperties.suggestor.chosenSuggestions);}
        public InstanceProperty<Suggestor, Suggestor.Editor> editor(){return new InstanceProperty<>(source,PackageProperties.suggestor.editor);}
        public InstanceProperty<Suggestor, Object> nonOverlaySuggestionResults(){return new InstanceProperty<>(source,PackageProperties.suggestor.nonOverlaySuggestionResults);}
        public InstanceProperty<Suggestor, Suggestor.Suggestions> suggestions(){return new InstanceProperty<>(source,PackageProperties.suggestor.suggestions);}
        public InstanceProperty<Suggestor, Object> value(){return new InstanceProperty<>(source,PackageProperties.suggestor.value);}
      }
      
      public  InstanceProperties instance(Suggestor instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
