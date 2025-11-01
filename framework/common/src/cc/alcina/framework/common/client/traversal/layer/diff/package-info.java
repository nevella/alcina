/**
 * Create a union tree of two dom subtrees, with correspondences merged
 */
/*
 * @formatter:off
 
notes on diff

* if a text node is being appended to a container, the previous text -identity- structure must match - e.g.

div.a0
  p.a1		p.a3
    $text.a2  		$text.a4
			
  -->
div.b0
  p.b1		p.b3
    $text.b2  		$text.b4. because $text.a4 [parent] != $text.a2 [parent]


how this affects table merges


table.a0
  tr.a1                
    td.a2              td.a4
        $t[bruce].a3            $t[cat].a5

table.b0
  tr.b1                
    td.b2              td.b4
        $t[bruce].b3.            $t[dog].b5

table.a0
  tr.a1                
    td.a2              td.a4
        $t[cat].a3            $t[bruce].a5

table.b0
  tr.b1                
    td.b2              td.b4
        $t[dog].b3.            $t[bruce].b5




 * @formatter:on
 */
package cc.alcina.framework.common.client.traversal.layer.diff;
