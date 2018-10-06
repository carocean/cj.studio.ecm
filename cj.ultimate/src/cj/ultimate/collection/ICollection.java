package cj.ultimate.collection;

import java.util.Collection;
import java.util.List;

public interface ICollection<E> extends Iterable<E> {

	public abstract int size();

	public abstract boolean isEmpty();

	public abstract boolean contains(Object o);

	public abstract Object[] toArray();

	public abstract <T> T[] toArray(T[] a);
	abstract List<E> asList();
	public abstract boolean containsAll(Collection<?> c);

	public abstract boolean retainAll(Collection<?> c);
	E get(int index);
}