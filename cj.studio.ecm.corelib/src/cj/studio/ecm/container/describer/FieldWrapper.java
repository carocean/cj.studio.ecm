package cj.studio.ecm.container.describer;

import java.lang.reflect.Field;
//此包装目的是为了将来扩展需要。
public class FieldWrapper {
	private Field field;
	private Class<?> refType;
	public Field getField() {
		return field;
	}
	public Class<?> getRefType() {
		return refType;
	}
	public void setField(Field field) {
		this.field = field;
	}
	public void setRefType(Class<?> refType) {
		this.refType = refType;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FieldWrapper){
			FieldWrapper w=(FieldWrapper)obj;
			return w.field.equals(this.field);
		}
		return super.equals(obj);
	}
}
