package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

 class DomNodeListStatic {
	static  <T extends Node> Stream<T> stream0(DomNodeList<T> ref){
		List<T> list = new ArrayList<>();
		
		for (int idx=0;idx<ref.getLength();idx++) {
			list.add(ref.getItem(idx));
		}
		return list.stream();
	}
}
