package cj.ultimate.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class ReadOnlyCollectionBase<E> implements Iterable<E>,
		ICollection<E> {
	protected List<E> innerList;

	public ReadOnlyCollectionBase() {
		this.innerList = new ArrayList<E>();
	}

	public ReadOnlyCollectionBase(List<E> list) {
		this.innerList = list;
	}

	public ReadOnlyCollectionBase(E[] array) {
		this.innerList = Arrays.asList(array);
	}

	public ReadOnlyCollectionBase(Collection<E> collection) {
		this.innerList = Collections.list(Collections.enumeration(collection));
	}
	/**
	 * 合并到当前集合并返回当前集合
	 * <pre>
	 *
	 * </pre>
	 * @param col
	 * @return
	 */
	public ICollection<E> combine(ReadOnlyCollectionBase<E> col){
		innerList.addAll(col.innerList);
		return this;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#size()
	 */
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.innerList.size();
	}

	@Override
	public List<E> asList() {
		List<E> list=new ArrayList<E>();
		list.addAll(this.innerList);
		return list;
	}

	@Override
	public E get(int index) {
		return innerList.get(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return innerList.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return innerList.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return innerList.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#toArray()
	 */
	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return innerList.toArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return innerList.toArray(a);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return innerList.containsAll(c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cj.ultimate.ICollection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return innerList.retainAll(c);
	}

}
