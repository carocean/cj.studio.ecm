package cj.studio.ecm.frame;

/*
 * Content-Disposition: form-data; name="faceImg"; filename="4.jpg"
Content-Type: image/jpeg

 states follow NOTSTARTED PREAMBLE ( (HEADERDELIMITER DISPOSITION (FIELD |
 FILEUPLOAD))* (HEADERDELIMITER DISPOSITION MIXEDPREAMBLE (MIXEDDELIMITER
 MIXEDDISPOSITION MIXEDFILEUPLOAD)+ MIXEDCLOSEDELIMITER)* CLOSEDELIMITER)+
 EPILOGUE
规范：
--d
crlf
attr
crlf
attr
crlf
attr
crlf
crlf
data
crlf
--d
crlf
attr
crlf
crlf
data
crlf
--d--



侦头
 Content-type: multipart/form-data, boundary=AaB03x

侦内容：以分隔符分隔，在每个域，key=value之后一个\r\n在所有头域的最后再加一个\r\n即在正文前有两个\r\n其中一个是最后一个头的\r\n
multipart/form-data 即html提交表单的格式，是在侦头中才出现的，在数据内容中不出现，它用于文件上传和表单域的提交，因此该对像即可处理文件，也可处理表单设为multipart/form-data的解析。除此之外，在下面的数据内容中可能还会出现：multipart/mixed，它在此专指表单属性的数据，文件的Content-type一般是文件格式
multipart/mixed一般是form潜逃导致，在一个form中使用了multipart/form-data提交，而在此form中又使用form且又指定为multipart/form-data


--AaB03x 
content-disposition: form-data; name="field1"

 Joe Blow  
--AaB03x 
content-disposition: form-data; name="pics" 
Content-type: multipart/mixed, boundary=BbC04y

kiridkdk
--BbC04y 
Content-disposition: attachment; filename="file1.txt" 
Content-Type: text/plain

xkjslkdjflke
--BbC04y
Content-disposition: file; filename="file2.gif"  
Content-type: image/gif 
Content-Transfer-Encoding:binary

lrooifjkjlkfjlkf
--BbC04y--
--AaB03x
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cj.studio.ecm.EcmException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/*
 * 实际上各浏览器上的formData请求格式是这样的：
 * Content-Type=multipart/form-data; boundary=----WebKitFormBoundaryHYBypLEFfp98EMF2
 * 上面的boundary分隔串指定的是以-号开头，各浏览器都是以多个－号开头，但这个分隔串虽然是用于指定form-data数据域中的分隔，但是：
 * 在开始位置，它比数据域中少了两个连续的--号，比结束时也少了两个--号，但是在结束时都多添加了两个--
 * 
 * 因此，在使用httpform侦时，取到分隔符应在前面加两个--号，这个问题除了ie其它浏览器均已测过，都是这个规则。
 * 
 * ------WebKitFormBoundaryFWSTsCnoOkljFCsb
Content-Disposition: form-data; name="faceImg"; filename="2.jpg"
Content-Type: image/jpeg

同水
------WebKitFormBoundaryFWSTsCnoOkljFCsb--
 */
/**
 * 表单对象，在html中的表单写作：multipart/form-data提交时，对其侦的内容必须使用FormData
 * 
 * <pre>
 * 用法：
 * 假如有一个multipart/form-data侦请求frame1
 * FormData fd=new FormData(frame1.content().readFully());
 * 然后便可以使用表单的值或者取出文件数据。
 * 
 * 如果要支挂巨大文件的接收的话，将来得把根据此类实现流式解析类关在{@link UploadSiteHttpRequest}类中实现大文件的解析
 * </pre>
 * 
 * @author carocean
 *
 */
public class FormData {
	List<FieldData> fields;

	FieldParser parser;

	public FormData() {
		fields = new ArrayList<>();
		parser = new FieldParser();
	}

	/**
	 * 注意：在分boundary符前加上--号，否则由于分隔符不正确，导致表单解析为空。原因如下：
	 * 
	 * <pre>
	  * 实际上各浏览器上的formData请求格式是这样的：
	* Content-Type=multipart/form-data; boundary=----WebKitFormBoundaryHYBypLEFfp98EMF2
	* 上面的boundary分隔串指定的是以-号开头，各浏览器都是以多个－号开头，但这个分隔串虽然是用于指定form-data数据域中的分隔，但是：
	* 在开始位置，它比数据域中少了两个连续的--号，比结束时也少了两个--号，但是在结束时都多添加了两个--
	* 
	* 因此，在使用httpform侦时，取到分隔符应在前面加两个--号，这个问题除了ie其它浏览器均已测过，都是这个规则。
	* 
	* ------WebKitFormBoundaryFWSTsCnoOkljFCsb
	Content-Disposition: form-data; name="faceImg"; filename="2.jpg"
	Content-Type: image/jpeg
	
	同水
	 * ------WebKitFormBoundaryFWSTsCnoOkljFCsb--
	 * 
	 * 
	 * 这几个header的意思分别为服务器返回的数据需要使用gzip压缩、请求的内容长度为225873、内容的类型为"multipart/form-data"、请求参数分隔符(boundary)为OCqxMF6-JxtxoMDHmoG5W5eY9MGRsTBp、请求的根域名为www.myhost.com、HTTP连接方式为持久连接( Keep-Alive)。
	其中这里需要注意的一点是分隔符，即boundary。 boundary用于作为请求参数之间的界限标识，例如参数1和参数2之间需要有一个明确的界限，这样服务器才能正确的解析到参数1和参数2。但是分隔符并不仅仅是boundary，而是下面这样的格式：-- + boundary。例如这里的boundary为 OCqxMF6-JxtxoMDHmoG5W5eY9MGRsTBp，那么参数分隔符则为:
	
	--OCqxMF6-JxtxoMDHmoG5W5eY9MGRsTBp
	不管boundary本身有没有这个"--"，这个"--"都是不能省略的。
	 * </pre>
	 */
	public FormData(byte[] b, String boundary) {
		this();
		Boundary bd = new Boundary(boundary);
		for (int i = 0; i < b.length; i++) {
			write(b[i], bd);
		}
	}

	/**
	 * 此方法会在boundary参数前自动加上--号
	 * 
	 * <pre>
	 * 以空构造一个FormData，并将侦内容传入，将侦的请求中的分隔符：
	 * Content-Type=multipart/form-data; boundary=----WebKitFormBoundaryHYBypLEFfp98EMF2
	 * 解析出来
	 * 
	 * 
	 * 注意：
	 * 此方法会在boundary参数前加上--号
	 * </pre>
	 * 
	 * @param b
	 * @param boundary
	 */
	public void load(byte[] b, String boundary) {
		Boundary bd = new Boundary(String.format("--%s", boundary));
		for (int i = 0; i < b.length; i++) {
			write(b[i], bd);
		}
	}

	public void add(FieldData fd) {
		fields.add(fd);
	}

	public Iterator<FieldData> iterator() {
		return fields.iterator();
	}

	public FieldData get(int index) {
		return fields.get(index);
	}

	public int size() {
		return fields.size();
	}
	/**
	 * 该方法支挂流式写
	 * <pre>
	 * 注意：
	 * 如果直接调用该方法需要在boundary参数前加上--号
	 * </pre>
	 * @param b
	 * @param bd
	 */
	public synchronized void write(byte b, Boundary bd) {
		int d = bd.write(b);

		byte[] buf = null;
		switch (d) {
		case -1:// 被分隔符类接收.什么也不做，数据在缓冲只有等待d=正数时接收缓存数据
			break;
		case -2:// 发现分隔符，但不确定是域开始还是表单结束,无缓存数据
			// System.out.println("发现分隔符,但不确定是域开始还是域结束,无缓存数据");
			break;
		case -3:// 整个formData结束,无缓存数据
			parser.formDone();
			break;
		case -4:// 新域开始，从分隔符缓存中取数,该数是新域的最前的一个或两个字节
			// 1.前域生成完
			// 2.构建新域
			parser.createOne();
			buf = new byte[bd.cache.readableBytes()];
			bd.cache.readBytes(buf);
			// write the buf of next field data
			for (int i = 0; i < buf.length; i++) {
				parser.write(buf[i]);
			}
			break;
		default:// 纯数据，分隔符无缓冲
			if (bd.cache.readableBytes() > 0) {
				buf = new byte[bd.cache.readableBytes()];
				bd.cache.readBytes(buf);
				// write the buf
				for (int i = 0; i < buf.length; i++) {
					parser.write(buf[i]);
				}
			}
			// write the b
			parser.write(b);
			break;
		}

	}

	class FieldParser {
		Crcf crcf;
		ByteBuf head;
		ByteBuf data;
		FieldData current;
		ByteBuf buf = Unpooled.buffer();

		public FieldParser() {
			crcf = new Crcf();
			head = Unpooled.buffer();
			data = Unpooled.buffer();
		}

		public void createOne() {
			if (current == null) {
				current = new FieldData();
//				System.out.println("表单开始第1个域");
				return;
			}
			// 前一个完成，放入fields中
			parseField();
			// 新域
			current = new FieldData();
//			System.out.println("开始新域");
		}

		private void parseField() {
			int begin = crcf.getBeginNewLine(buf);
			int end=crcf.getEndNewLine(buf);
			int next1 = begin;
			int next2 = 0;
			int prev=0;
			while (next2 < end) {
				prev=next1;
				next1 = crcf.nextNewLine(next1, buf);
				if (next1 >=end) {
					break;
				}
				
				byte[] b=new byte[next1-prev];
				buf.getBytes(prev, b, 0, b.length);
//				String head=new String(b);
				current.parseKeyPair(b);
//				System.out.println(head);
				// next1-prev=head size
				
				
				next2 = crcf.nextNewLine(next1+2, buf);
				if (next2 - next1 == 2) {// 两个连续的换行为数据
					// copyData
					int len=end-(next2+2);
					if(len<0){
						throw new EcmException("不符合规范：在数据后分隔串前应加上换行符，那怕数据为空:"+current.getDisposition());
					}
					 b=new byte[len];
					buf.getBytes(next2+2, b, 0, b.length);
					current.data(b);
//					System.out.println(b.length);
//					System.out.println("\t考数");
					break;
				} else{
					next1+=2;
				}
				
			}
//			System.out.println("\t将一个域生成完放入表单");
			fields.add(current);
			buf.clear();
		}

		public void formDone() {
			// 前一个完成，放入fields中
			parseField();
//			System.out.println("表单结束");
		}

		public void write(byte b) {
			buf.writeByte(b);
		}

	}
}
