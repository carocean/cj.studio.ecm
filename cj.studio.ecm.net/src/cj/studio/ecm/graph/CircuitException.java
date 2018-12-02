package cj.studio.ecm.graph;

import java.util.Stack;

import org.slf4j.Logger;

import cj.ultimate.util.StringUtil;

/**
 * 回路异常。
 * 
 */
// 因为端子和sink均是一个flow方法，因此实际上等同于同一段代码的异常嵌套，据其特性，只要中间不是静默异常，
// 也就是均在catch中用了throw 关键字，该关键字说明异常向上层传递，因此只有最顶的调用者的catch中的throw 也会被抛出异常信息。
// 中间只要不显示打出日志或e.print，均不会在控制台打印错误信息，只有最顶的调用者可。
// 嵌套异常每层的catch只要不是静默异常，每层的catch,finally代码均会执行
// 最内层堆栈最全，最顶层少，这是因为，堆栈表示异常发生处，因此在最内层的异常其到最外层最远，因此其集合最大。
// 每次在catch中throw时，如果没有将捕获的e抛向上层，则底层的e将会替换而采用catch中新定义的e，如果传入了e参数或直接抛了e则上层能见之
// 如果每层均抛出了e，则每个e的实例在各层中是可见的且相同的。因此可将最内层的e作为共享对象
// cause是由最外层到最里层的一个异常实例连，即最里层可作为共享对象,因此，最外层的原因最多。cause是从构造中新建时传入了其它异常实例，则这个其它实例就视这原因。
// 堆栈由内到外描述发生处，原因由外到内表明原诿。
// 如果中间断连则最外层只有显示到断连处
// 如果catch中没有不是新建异常向上传，而是直接throw e，则最外层原因会跳过中间这些直接为事件发生处
public class CircuitException extends Exception {
	public final static CircuitException EXCEPTION_503 = new CircuitException(
			"503", "未定义的错误。");
	public final static CircuitException EXCEPTION_200 = new CircuitException(
			"200", "ok");
	/**
	 * <pre>
	 *
	 * </pre>
	 */
	private static final long serialVersionUID = 1L;
	private boolean isSystemException;
	protected String status;
	private StackTraceElement[] dirty = new StackTraceElement[0];

	public CircuitException(String status, Throwable e) {
		super(e);
		this.status = status;
		this.isSystemException = false;
	}

	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param status
	 * @param isSystemException
	 *            作用是在屏蔽graph的使用者定义的异常，而使graph的设计者能规范异常的代码。
	 *            在异常抛出的栈连中，当遇到了isSystemException＝true则不在往后查找，而以此作为共享的异常
	 * @param e
	 */
	public CircuitException(String status, boolean isSystemException,
			Throwable e) {
		super(e);
		this.status = status;
		this.isSystemException = isSystemException;
	}

	public CircuitException(String status, String e) {
		super(e);
		this.status = status;
		this.isSystemException = false;
	}
	/**
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param status
	 * @param isSystemException
	 *            作用是在屏蔽graph的使用者定义的异常，而使graph的设计者能规范异常的代码。
	 *            在异常抛出的栈连中，当遇到了isSystemException＝true则不在往后查找，而以此作为共享的异常
	 * @param e
	 */
	public CircuitException(String status, boolean isSystemException, String e) {
		super(e);
		this.status = status;
		this.isSystemException = isSystemException;
	}

	public String getStatus() {
		return status;
	}

	/**
	 * 系统异常
	 * 
	 * <pre>
	 * 作用是在屏蔽graph的使用者定义的异常，而使graph的设计者能规范异常的代码。
	 * 在异常抛出的栈连中，当遇到了isSystemException＝true则不在往后查找，而以此作为共享的异常
	 * </pre>
	 * 
	 * @return
	 */
	public boolean isSystemException() {
		return isSystemException;
	}

	/**
	 * 查找可共享的异常，它可穿索于整个出错的回路中
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param e
	 * @return
	 */
	public final static CircuitException search(Throwable e) {
		Stack<Throwable> stack = new Stack<Throwable>();
		CircuitException result = null;
		Throwable tmp = e;
		do {// 正序搜系统异常
			if (tmp instanceof CircuitException) {
				CircuitException d = (CircuitException) tmp;
				if (d.isSystemException) {
					result = d;
					break;
				}
			}
			stack.push(tmp);
		} while ((tmp = tmp.getCause()) != null);
		// 如果不存在系统异常则反序找回路异常
		while (result == null && !stack.isEmpty()) {
			tmp = stack.pop();
			if (tmp instanceof CircuitException) {
				result = (CircuitException) tmp;
				break;

			}
		}
		return result;
	}

	@Override
	public String getMessage() {
//		if (getCause() == null)
//			return status + " " + super.getMessage();
//		Throwable e = getCause();
//		Stack<Throwable> stack = new Stack<Throwable>();
//		do {// 正序搜系统异常
//			stack.push(e);
//		} while ((e.getCause()) != null);
//		while(!stack.isEmpty()){
//			e=stack.pop();
//			if (e instanceof CircuitException) {
//				return ((CircuitException) e).getStatus()+" "+e.getMessage();
//			}
//		}
		return /*status + " " +*/ super.getMessage();
	}

	public String messageCause() {
		StringBuffer sb = new StringBuffer();
		sb.append(getMessage() + "\r\n");
		for (StackTraceElement st : dirty) {
			sb.append("\tat " + st.getClassName() + "." + st.getMethodName()
					+ "(" + st.getFileName() + ":" + st.getLineNumber()
					+ ")\r\n");
		}
		Throwable e = getCause();
		Throwable prev = null;
		while (e != null) {
			prev = e;
			e = e.getCause();
		}
		if (prev != null) {
			sb.append("Caused by :\r\n");
			for (StackTraceElement st : prev.getStackTrace()) {
				sb.append("\tat " + st.getClassName() + "."
						+ st.getMethodName() + "(" + st.getFileName() + ":"
						+ st.getLineNumber() + ")\r\n");
			}
		}
		return sb.toString();
	}

	


	/**
	 * 打印错误信息
	 * 
	 * <pre>
	 *
	 * </pre>
	 * 
	 * @param e
	 * @param logger
	 */
	public static String print(Throwable e, Logger logger) {
		CircuitException ce = search(e);
		String cause = ce == null ? e.getMessage() : ce.messageCause();
		if(logger!=null)
		logger.error(String.format("%s %s", ce!=null?ce.status:"",cause));
		return cause;
	}
	/**
	 * 在此位置发生的异常，并从协议工厂返回具体错误信息
	 * <pre>
	 * 如果工厂中未定义指定的错误代码，则返回503错误，表明为未定义的异常
	 * </pre>
	 * @param factory
	 * @param status
	 * @param info
	 * @return
	 */
	public static CircuitException throwAt(IProtocolFactory factory,String status,Object... info){
		String msg=factory.get(status);
		if(StringUtil.isEmpty(msg)){
			return EXCEPTION_503; 
		}
		if(info==null){
			return new CircuitException(status, "");
		}
		msg=String.format(msg, info);
		return new CircuitException(status, msg);
	}
	public static Exception throwExceptionIt(Throwable e, Logger logger) {
		CircuitException ce = search(e);
		if (ce == null)
			throw new RuntimeException(e);
		String cause = ce.messageCause();
		if (logger != null)
			logger.error(cause);
		return ce;
	}
	public static RuntimeException throwRuntimeIt(Throwable e, Logger logger) {
		CircuitException ce = search(e);
		if (ce == null)
			throw new RuntimeException(e);
		String cause = ce.messageCause();
		if (logger != null)
			logger.error(cause);
		return new RuntimeException(ce.status+" "+cause);
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("%s %s", status,getMessage());
	}
}
