package cj.studio.ecm.container.registry;



public class AnnotationServiceDefinition extends ServiceDefinition {
	private Class<?> annotatedClass;
	//基础类为byte可能位数不够用
	private int annotateForm;
	public Class<?> getAnnotatedClass() {
		return annotatedClass;
	}
	public void setAnnotatedClass(Class<?> annotatedClass) {
		this.annotatedClass = annotatedClass;
	}
	public int getAnnotateForm() {
		return annotateForm|super.getServiceDescribeForm();
	}
	public void setAnnotateForm(int annotateForm) {
		this.annotateForm = annotateForm;
		//只取低8位以兼容超类
		super.setServiceDescribeForm((byte)(annotateForm));
		
	}
@Override
public boolean equals(Object obj) {
	return super.equals(obj);
}
}
