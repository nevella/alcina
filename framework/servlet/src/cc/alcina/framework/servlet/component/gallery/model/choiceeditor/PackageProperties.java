package cc.alcina.framework.servlet.component.gallery.model.choiceeditor;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.gallery.model.choiceeditor.ChoiceEditorGalleryArea;
import cc.alcina.framework.servlet.component.gallery.model.choiceeditor.ChoiceEditorGalleryPlace;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _ChoiceEditorGalleryArea choiceEditorGalleryArea = new _ChoiceEditorGalleryArea();
    public static _ChoiceEditorGalleryPlace_Definition choiceEditorGalleryPlace_definition = new _ChoiceEditorGalleryPlace_Definition();
    
    static class _ChoiceEditorGalleryArea implements TypedProperty.Container {
      TypedProperty<ChoiceEditorGalleryArea, ChoiceEditorGalleryPlace.Definition> definition = new TypedProperty<>(ChoiceEditorGalleryArea.class, "definition");
      TypedProperty<ChoiceEditorGalleryArea, ChoiceEditorGalleryArea.InfoModel> model = new TypedProperty<>(ChoiceEditorGalleryArea.class, "model");
      TypedProperty<ChoiceEditorGalleryArea, GalleryPlace> place = new TypedProperty<>(ChoiceEditorGalleryArea.class, "place");
    }
    
    public static class _ChoiceEditorGalleryPlace_Definition implements TypedProperty.Container {
      public TypedProperty<ChoiceEditorGalleryPlace.Definition, List> users = new TypedProperty<>(ChoiceEditorGalleryPlace.Definition.class, "users");
    }
    
//@formatter:on
}
