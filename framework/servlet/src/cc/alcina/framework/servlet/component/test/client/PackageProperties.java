package cc.alcina.framework.servlet.component.test.client;

import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.CollectionDeltaModel;
import cc.alcina.framework.servlet.component.test.client.TestChubbyTree;
import java.lang.String;
import java.util.List;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    static _TestChubbyTree_TestContainer testChubbyTree_testContainer = new _TestChubbyTree_TestContainer();
    
    static class _TestChubbyTree_TestContainer implements TypedProperty.Container {
      TypedProperty<TestChubbyTree.TestContainer, List> collection = new TypedProperty<>(TestChubbyTree.TestContainer.class, "collection");
      TypedProperty<TestChubbyTree.TestContainer, CollectionDeltaModel> collectionRepresentation = new TypedProperty<>(TestChubbyTree.TestContainer.class, "collectionRepresentation");
      TypedProperty<TestChubbyTree.TestContainer, String> heading = new TypedProperty<>(TestChubbyTree.TestContainer.class, "heading");
      TypedProperty<TestChubbyTree.TestContainer, TestChubbyTree.TestContainer.Style> style = new TypedProperty<>(TestChubbyTree.TestContainer.class, "style");
    }
    
//@formatter:on
}
