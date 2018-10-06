package cj.studio.ecm;



//加入国际化
public class EcmException extends RuntimeException {

	public EcmException() {
		// TODO Auto-generated constructor stub
	}

	//message中$代表国际化变量:$(name.ddd){arg1,arg2},
	public EcmException(String message) {
		super(message);
//		if (message.contains("$")) {
//			ILanguage lang = null;
//			if (DriverDomain.class.getClassLoader() instanceof JarClassLoader) {
//				lang = DriverDomain.current().getLanguage();
//			} else if (ChipDomain.class.getClassLoader() instanceof JarClassLoader) {
//				lang = ChipDomain.current().getLanguage();
//			} else {
//				lang = CJSystem.current().getEnvironment().getLanguage();
//			}
//			// lang.lookup(message, .)
//		}
	}

	public EcmException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public EcmException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
