package cj.studio.ecm.domain;

import java.io.InputStream;

import cj.studio.ecm.EcmException;
import cj.ultimate.org.objectweb.asm.ClassReader;
import cj.ultimate.org.objectweb.asm.ClassVisitor;
import cj.ultimate.org.objectweb.asm.ClassWriter;
import cj.ultimate.org.objectweb.asm.Opcodes;


//用于生成域
public class DomainTypeFactory {

	//生成域
	public static byte[] generate(String domainClassName){
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassReader cr;
		try {
			InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(domainClassName.replace('.', '/')
                    + ".class");
			cr = new ClassReader(is);
			ClassVisitor ca = new ClassVisitor(Opcodes.ASM5,cw){};
			cr.accept(ca, 0);
			ca.visitEnd();
			return cw.toByteArray();
		} catch (Exception e) {
			throw new EcmException(e+":"+domainClassName+" classload:"+Thread.currentThread().getContextClassLoader());
		}
	}
}
