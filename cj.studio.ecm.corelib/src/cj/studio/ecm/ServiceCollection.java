package cj.studio.ecm;

import java.util.Collection;
import java.util.List;

import cj.ultimate.collection.ReadOnlyCollectionBase;

public class ServiceCollection<T> extends ReadOnlyCollectionBase<T> {

	public ServiceCollection() {
		// TODO Auto-generated constructor stub
	}

	public ServiceCollection(List<T> list) {
		super(list);
		// TODO Auto-generated constructor stub
	}

	public ServiceCollection(T[] array) {
		super(array);
		// TODO Auto-generated constructor stub
	}

	public ServiceCollection(Collection<T> collection) {
		super(collection);
		// TODO Auto-generated constructor stub
	}
}
