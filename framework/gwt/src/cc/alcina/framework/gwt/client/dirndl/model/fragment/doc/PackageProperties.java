package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;

public class PackageProperties {
    // auto-generated, do not modify
    //@formatter:off
    
    public static _Block_Para block_para = new _Block_Para();
    public static _Pagination_PageBreakBlock pagination_pageBreakBlock = new _Pagination_PageBreakBlock();
    public static _Pagination_PageBreakBlock_PageBreakNumber pagination_pageBreakBlock_pageBreakNumber = new _Pagination_PageBreakBlock_PageBreakNumber();
    
    public static class _Block_Para implements TypedProperty.Container {
      public TypedProperty<Block.Para, String> className = new TypedProperty<>(Block.Para.class, "className");
      public TypedProperty<Block.Para, Boolean> coverPageContainer = new TypedProperty<>(Block.Para.class, "coverPageContainer");
      public TypedProperty<Block.Para, FragmentDocument> document = new TypedProperty<>(Block.Para.class, "document");
      public TypedProperty<Block.Para, Boolean> split = new TypedProperty<>(Block.Para.class, "split");
      public TypedProperty<Block.Para, FragmentDocumentNode> splitReplicatedFrom = new TypedProperty<>(Block.Para.class, "splitReplicatedFrom");
      public TypedProperty<Block.Para, String> style = new TypedProperty<>(Block.Para.class, "style");
      public static class InstanceProperties extends 	InstanceProperty.Container<Block.Para> {
        public  InstanceProperties(Block.Para source){super(source);}
        public InstanceProperty<Block.Para, String> className(){return new InstanceProperty<>(source,PackageProperties.block_para.className);}
        public InstanceProperty<Block.Para, Boolean> coverPageContainer(){return new InstanceProperty<>(source,PackageProperties.block_para.coverPageContainer);}
        public InstanceProperty<Block.Para, FragmentDocument> document(){return new InstanceProperty<>(source,PackageProperties.block_para.document);}
        public InstanceProperty<Block.Para, Boolean> split(){return new InstanceProperty<>(source,PackageProperties.block_para.split);}
        public InstanceProperty<Block.Para, FragmentDocumentNode> splitReplicatedFrom(){return new InstanceProperty<>(source,PackageProperties.block_para.splitReplicatedFrom);}
        public InstanceProperty<Block.Para, String> style(){return new InstanceProperty<>(source,PackageProperties.block_para.style);}
      }
      
      public  InstanceProperties instance(Block.Para instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Pagination_PageBreakBlock implements TypedProperty.Container {
      public TypedProperty<Pagination.PageBreakBlock, FragmentDocument> document = new TypedProperty<>(Pagination.PageBreakBlock.class, "document");
      public TypedProperty<Pagination.PageBreakBlock, Boolean> firstPage = new TypedProperty<>(Pagination.PageBreakBlock.class, "firstPage");
      public TypedProperty<Pagination.PageBreakBlock, Boolean> split = new TypedProperty<>(Pagination.PageBreakBlock.class, "split");
      public TypedProperty<Pagination.PageBreakBlock, FragmentDocumentNode> splitReplicatedFrom = new TypedProperty<>(Pagination.PageBreakBlock.class, "splitReplicatedFrom");
      public TypedProperty<Pagination.PageBreakBlock, String> text = new TypedProperty<>(Pagination.PageBreakBlock.class, "text");
      public static class InstanceProperties extends 	InstanceProperty.Container<Pagination.PageBreakBlock> {
        public  InstanceProperties(Pagination.PageBreakBlock source){super(source);}
        public InstanceProperty<Pagination.PageBreakBlock, FragmentDocument> document(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock.document);}
        public InstanceProperty<Pagination.PageBreakBlock, Boolean> firstPage(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock.firstPage);}
        public InstanceProperty<Pagination.PageBreakBlock, Boolean> split(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock.split);}
        public InstanceProperty<Pagination.PageBreakBlock, FragmentDocumentNode> splitReplicatedFrom(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock.splitReplicatedFrom);}
        public InstanceProperty<Pagination.PageBreakBlock, String> text(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock.text);}
      }
      
      public  InstanceProperties instance(Pagination.PageBreakBlock instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
    public static class _Pagination_PageBreakBlock_PageBreakNumber implements TypedProperty.Container {
      public TypedProperty<Pagination.PageBreakBlock.PageBreakNumber, FragmentDocument> document = new TypedProperty<>(Pagination.PageBreakBlock.PageBreakNumber.class, "document");
      public TypedProperty<Pagination.PageBreakBlock.PageBreakNumber, Boolean> split = new TypedProperty<>(Pagination.PageBreakBlock.PageBreakNumber.class, "split");
      public TypedProperty<Pagination.PageBreakBlock.PageBreakNumber, FragmentDocumentNode> splitReplicatedFrom = new TypedProperty<>(Pagination.PageBreakBlock.PageBreakNumber.class, "splitReplicatedFrom");
      public TypedProperty<Pagination.PageBreakBlock.PageBreakNumber, String> text = new TypedProperty<>(Pagination.PageBreakBlock.PageBreakNumber.class, "text");
      public static class InstanceProperties extends 	InstanceProperty.Container<Pagination.PageBreakBlock.PageBreakNumber> {
        public  InstanceProperties(Pagination.PageBreakBlock.PageBreakNumber source){super(source);}
        public InstanceProperty<Pagination.PageBreakBlock.PageBreakNumber, FragmentDocument> document(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock_pageBreakNumber.document);}
        public InstanceProperty<Pagination.PageBreakBlock.PageBreakNumber, Boolean> split(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock_pageBreakNumber.split);}
        public InstanceProperty<Pagination.PageBreakBlock.PageBreakNumber, FragmentDocumentNode> splitReplicatedFrom(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock_pageBreakNumber.splitReplicatedFrom);}
        public InstanceProperty<Pagination.PageBreakBlock.PageBreakNumber, String> text(){return new InstanceProperty<>(source,PackageProperties.pagination_pageBreakBlock_pageBreakNumber.text);}
      }
      
      public  InstanceProperties instance(Pagination.PageBreakBlock.PageBreakNumber instance) {
        return new InstanceProperties( instance);
      }
      
    }
    
//@formatter:on
}
