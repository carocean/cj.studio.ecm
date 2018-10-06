package cj.studio.ecm.memory;

public interface ISharedMemory {
	IAllocator getCache(String point);
}
