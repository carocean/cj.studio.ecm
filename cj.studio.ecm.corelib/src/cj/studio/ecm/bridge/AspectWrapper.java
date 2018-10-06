package cj.studio.ecm.bridge;

class AspectWrapper {
	private String name;
	private IAspect aspect;
	public AspectWrapper(String name,IAspect aspect) {
		this.name=name;
		this.aspect=aspect;
	}
	public IAspect getAspect() {
		return aspect;
	}
	public String getName() {
		return name;
	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return name.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return name.equals(((AspectWrapper)obj).name);
	}
}
