package cj.ultimate.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class CloneUtil {

	public static Object clone(Object obj) throws CloneException {
		java.io.ByteArrayOutputStream bsout = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		try {
			out = new ObjectOutputStream(bsout);
			out.writeObject(obj);
			out.close();
			ByteArrayInputStream bsin = new ByteArrayInputStream(
					bsout.toByteArray());
			in = new ObjectInputStream(bsin);
			// bsin.reset();
			Object o = in.readObject();
			in.close();
			return o;
		} catch (IOException e) {
			throw new CloneException(e);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			throw new CloneException(e);
		} finally {

		}

	}
}
