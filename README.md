
The ECM development toolkit is a Java-based development framework with a core focus on service containers, similar to the Spring framework, but its functionality far surpasses that of Spring’s service container. Functionally, ECM can be seen as the combined capabilities of Spring, Spring Boot, Spring Cloud, OSGi, and Node.js, making it a simpler yet more powerful IoC framework.

ECM supports modular development and uniquely enables Java-based JavaScript service development (referred to as JSS services), achieving seamless injection and interoperability between Java and JavaScript services. As a modular (OSGi) framework, it features hot-plug capabilities with a simple and user-friendly design.

Additionally, the ECM framework includes microservice suites such as Gateway, OpenPorts, and Mic, which facilitate the construction of distributed microservice clusters. It not only supports HTTP protocols but also extends compatibility to TCP, UDP, and UDT protocols, enhancing its application in complex network scenarios.


# ecm&net

keywords：j2ee,java,nodejs,osgi,jsp,struts,spring,spring mvc,mongodb,radis,zookeeper,netty,mina,jetty,tomcat,weblogic,websphere,orm,cloud,nashron,jdk

![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/ecm-design.png)

## ECM: Development Toolkit for Connection-Oriented Electronic Models

I. As a Service Container: Support for Service Definitions

	1.	Supports annotation-based service definitions.
	2.	Supports XML-based service definitions.
	3.	Supports JSON-based service definitions.
	4.	Allows hybrid definitions combining annotations, XML, and JSON.
	5.	Enables reverse injection.
	6.	Supports injection of property values, method parameter values, and even code snippets.
	7.	Allows injection into any class methods (unlike Spring, which only supports factory methods).
	8.	Provides robust Aspect-Oriented Programming (AOP) capabilities with simple structure, including intercepting Java methods using JSS services.
	9.	Supports service search by type.
	10.	Supports service search by external class categories.
	11.	Supports adapter objects, which can convert into any type, thereby implementing a weakly typed service mechanism.

II. As an OSGi Container

Using assemblies (referred to logically as “chips”) as its core concept, the toolkit supports:

	1.	Loading, starting, stopping, and unloading of assemblies.
	2.	Type dependencies and extensions between assemblies.
	3.	Strong dependencies between service instances across assemblies.
	4.	External services and types, allowing access to these components outside the assembly.

III. As a Node.js Container

The toolkit incorporates a Node.js-like syntax structure, referred to as JSS services in CJ Studio products, with the following features:

	1.	Exports: Enables exporting external functions, similar to Node.js.
	2.	Imports: Allows importing assemblies and module environments, including access to the service container.
	3.	Head Object: Declares services using a head object in each JSS service.
	4.	Supports mixing JSS services with Java services.
	5.	Enables multi-threaded JSS services.
	6.	Supports developing web applications using JSS.

A Comprehensive Connection-Oriented Programming System

The connection-oriented programming system provides a complete toolkit for:

	•	Web MVC frameworks.
	•	Persistent layers.
	•	Development tools.
	•	Distributed deployment.
	•	Local tools and remote service containers.

This unified development and deployment framework eliminates the challenges of patchwork development and operational inefficiencies.

Advantages of the Connection-Oriented Toolkit

	1.	Architecturally simpler and more user-friendly than Spring.
	2.	Less cumbersome than OSGi.
	3.	Provides more structured and standardized server-side development compared to Node.js.
	4.	Seamlessly coexists with Java, supporting the creation of large-scale distributed operational environments, such as neural networks.

Open Source and History

Since its inception in 2008, the technology has been adopted by multiple companies. To foster continued development, CJ Studio has officially open-sourced the toolkit. Any enterprise or individual can freely use it in their products, with the only requirement being attribution in the code: cj.studio.

Toolkit Highlights

	1.	ECM Development Toolkit: Combines functionalities of Spring, OSGi, and Node.js.
	•	Supports modular development and deployment with hot-plugging capabilities.
	2.	Net Development Toolkit: Designed for web development.
	•	Fully supports JavaScript-based web applications.
	•	Syntax aligns closely with popular Node.js frameworks.
	•	Includes:
	•	NIO Framework based on Netty.
	•	RIO (Reactive I/O Framework): Combines NIO advantages with synchronous response capabilities.

## Modular Development-Oriented Approach

```java

	IAssambly assambly=Assambly.load("/home/cj/test/helloworld.jar");
	assambly.start();
	
	IWorkbin bin=assambly.workbin();
	IDepartment dept=bin.part('deptment');
	System.out.println(dept.getName());
	
	assambly.close();

```    
- Note:

The assembly context prioritizes loading from the JAR. However, if the properties directory in the assembly’s runtime directory contains any of the following files: Assembly.json, Assembly.yaml, or Assembly.properties, these files take precedence.

Declaring Services Using Annotations

```java
	package your.crop.examples.chip2.anno;
	
	import java.util.List;
	import java.util.Map;
	
	import cj.studio.ecm.IServiceAfter;
	import cj.studio.ecm.IServiceSite;
	import cj.studio.ecm.ServiceCollection;
	import cj.studio.ecm.annotation.CjBridge;
	import cj.studio.ecm.annotation.CjJoinpoint;
	import cj.studio.ecm.annotation.CjMethod;
	import cj.studio.ecm.annotation.CjMethodArg;
	import cj.studio.ecm.annotation.CjPropertyValue;
	import cj.studio.ecm.annotation.CjService;
	import cj.studio.ecm.annotation.CjServiceInvertInjection;
	import cj.studio.ecm.annotation.CjServiceRef;
	import cj.studio.ecm.annotation.CjServiceSite;
	import cj.studio.ecm.bridge.UseBridgeMode;
	//aop by bridge
	@CjBridge(aspects = "myAspect1+$.cj.jss.test.JssMyAspect")
	@CjService(name = "myAnnoService", constructor = "newMyAnnotation")
	public class MyAnnoService implements IServiceAfter, IAnnoService {
		@CjServiceRef(refByType = AnnoObject.class)
		ServiceCollection objs;
		@CjServiceInvertInjection()
		@CjServiceRef(refByType = AnnoObject.class)
		AnnoObject obj;
		private int mmm;
		private String contrustText;
		@CjServiceRef(refByName = "myAnnoService", useBridge = UseBridgeMode.forbidden)
		static MyAnnoService the;
		@CjJoinpoint(aspects="myAspect3")
		@CjServiceRef(refByName = "myAnnoService",useBridge=UseBridgeMode.auto)
		static IAnnoService the2;
		@CjServiceSite
		IServiceSite site;
		
		@CjServiceRef(refByMethod="createMyService")
		AnnoObject byMethod;
		
		@CjServiceRef(refByMethod="createStaticMyService")
		AnnoObject byMethod2;
		
		@CjServiceRef(refByMethod="factory",useBridge=UseBridgeMode.forbidden)
		MyAnnoService byMethod3;
		
		@CjPropertyValue(parser = "cj.jsonMap", value = "{'age1':'333','myObject':'$.annoObject'}")
		private Map map;
	
		@CjPropertyValue(parser = "cj.jsonList", value = "['333','$.annoObject']")
		private List List;
		@CjServiceRef(refByName="otherMultitonService")
		OtherMultitonService multition;
		@CjMethod(alias = "newMyAnnotationMMM")
		public MyAnnoService(@CjMethodArg(value = "44") int mmm) {
			this.mmm = mmm;
		}
	
		@CjMethod(alias = "newMyAnnotation")
		public MyAnnoService(@CjMethodArg(value = "这是构造注入的TTTT的值") String tttt) {
			contrustText = tttt;
			System.out.println("这是构造输出：" + tttt);
		}
	
		@CjMethod(alias = "factory")
		public static MyAnnoService get() {
			return the;
		}
	
		@CjMethod(alias = "createMyService", returnDefinitionId = "annoObject")
		public AnnoObject createMyService(@CjMethodArg(value = "ddfddfas") String text,
				@CjMethodArg(ref = "annoObject") Object service) {
			System.out.println("这是方法参数注入的对象：" + service);
			System.out.println("这是方法参数注入的值：" + text);
			AnnoObject o = new AnnoObject();
			return o;
		}
	
		@CjMethod(alias = "createStaticMyService", returnDefinitionType = ".")
		public static AnnoObject createStaticMyService(@CjMethodArg(value="tttt")String text) {
			AnnoObject o = new AnnoObject();
			return o;
		}
	
		@Override
		public void onAfter(IServiceSite site) {
			// TODO Auto-generated method stub
			System.out.println(site);
			the2.toString();
			multition.setMmm("xxx");
			System.out.println("----多例："+multition);
			Object o=site.getService("otherMultitonService");
			System.out.println("----多例："+o);
			Object o2=site.getService("otherMultitonService");
			System.out.println("----多例："+o2);
			System.out.println("----运行时服务："+site.getService("runtime"));
		}
	
	}
```
## Declaring Services Using JSON

	1.java文件MyJsonService.java

```java	

	package your.crop.examples.chip2.json;
	
	import java.util.List;
	import java.util.Map;
	
	import cj.studio.ecm.IServiceSite;
	import cj.studio.ecm.adapter.IActuator;
	import cj.studio.ecm.adapter.IAdaptable;
	
	public class MyJsonService implements IJsonService {
		private Object byBridge;
		private Object deptment;
		IServiceSite site;
		List list;
		Map map;
		Object iocByMyValueParser;
		Object byMethod2;
		Object byMethod1;
		public MyJsonService() {
			// TODO Auto-generated constructor stub
		}
		public MyJsonService(int t,IMyJsonBridge b) {
			System.out.println("----这是json service构造:"+t+"  "+b);
		}
		public void test() {
			IAdaptable a = (IAdaptable) deptment;
			IActuator act = a.getAdapter(IActuator.class);
			Object result = act.exeCommand("getUserId", "我吊");
			System.out.println("deptment.getUserId.." + result);
			System.out.println("----"+byBridge+" 这是演示json服务调用桥服务");
			Object obj=newService();
			System.out.println(obj);
		}
	
		public static IMyJsonBridge newService() {
			return new MyJsonBridge();
		}
	
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.json.IJsonService#newService(java.lang.String)
		 */
		@Override
		public Object newService(String tt) {
			return new MyJsonBridge();
		}
	
	}
	MyJsonService.json文件放在同一目录下：
	//默认:isExoteric=false
	{
		"description": "本服务用于XXX，作者",
		"serviceId": "myJsonService",
		"class": "",
		"scope": "singleon",
		"constructor":"newJsonService",
		"bridge": {
			"aspects": "",
			"isValid": "false"
		},
		"properties": [
			{
				"name": "deptment",
				"refByName": "deptment"
			},
			{
				"name": "byBridge",
				"refByName": "myJsonBridge",
				"refByBridge": {
					"useBridge": "auto",
					"joinpoint": {
						"aspects": "+myAspect3"
					}
				},
				"invertInjection":"true"
			},
			{
				"name":"list",
				"parser":"cj.jsonList",
				"value":["333","$.myAnnoService"]
			},
			{
				"name":"map",
				"parser":"cj.jsonMap",
				"value":{"age1":"333","myObject":"$.myAnnoService"}
			},
			{
				"name":"iocByMyValueParser",
				"parser":"my.objectParser",
				"value":"new your.crop.examples.chip2.xml.ShowCreateObjectByValueParser();"
			},
			{
				"name":"site",
				"serviceSite":"true"
			},{
				"name":"byMethod2",
				"refByMethod":"newService2"
			},{
				"name":"byMethod1",
				"refByMethod":"newService"
			}
		],
		"methods": [
			{
	    		"alias":"newJsonService",
	    		"bind":"",
	    		"argTypes":"int,your.crop.examples.chip2.json.IMyJsonBridge",
	    		"args":[
	                {
	                    "value":"6666"
	               	},
	                {
	                	"ref":"myJsonBridge"
	               	}
	           	]
	    	},{
	    		"alias":"newService",
	    		"bind":"newService",
	    		"argTypes":"",
	    		"args":[
	           	],
	           	"result":{
	           		"byDefinitionId":"",
	           		"byDefinitionType":""
	           	}
	    	},{
	    		"alias":"newService2",
	    		"bind":"newService",
	    		"argTypes":"java.lang.String",
	    		"args":[
	    			{"value":"kiviv"}
	           	],
	           	"result":{
	           		"byDefinitionId":"myJsonBridge",
	           		"byDefinitionType":""
	           	}
	    	},{
	    		"alias":"newService",
	    		"bind":"newService",
	    		"argTypes":"",
	    		"result":{
	           		"byDefinitionId":"",
	           		"byDefinitionType":""
	           	},
	    		"args":[
	           	]
	    	}
		]
	}
```
## Declaring Services Using XML

1.MyXmlService.java
```java
	package your.crop.examples.chip2.xml;
	
	import java.util.List;
	import java.util.Map;
	
	import cj.studio.ecm.IServiceAfter;
	import cj.studio.ecm.IServiceSite;
	import your.crop.examples.chip2.anno.InvertInjectionAnnoService;
	import your.crop.examples.chip2.json.IJsonService;
	import your.crop.examples.chip2.json.ITest;
	import your.crop.examples.chip2.json.MyJsonService;
	
	public class MyXmlService implements IServiceAfter,ITest/*在服务容器初始化后触发*/, IXmlService{
		private String name;
		private Map map;
		Listlist;
		ITest service;
		IJsonService service2;
		IJsonService service3;
		InvertInjectionAnnoService invertInjectionAnnoService;
		Object	iocByMyValueParser;
		IServiceSite site;
		public MyXmlService() {
			// TODO Auto-generated constructor stub
		}
		public MyXmlService(int v) {
			System.out.println(this+" 这是构造注入："+v);
		}
		@Override
		public void onAfter(IServiceSite arg0) {
			System.out.println("MyXmlService .... 服务容器执行后被触发");
		}
		public void test1(String n) {
			System.out.println("MyXmlService ....演示不必实现ITest接口，只在myaspect1中切入接口即可执行myxmlService.test方法："+n);
		}
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.xml.IXmlService#getIocByMyValueParser()
		 */
		@Override
		public Object getIocByMyValueParser() {
			return iocByMyValueParser;
		}
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.xml.IXmlService#getService()
		 */
		@Override
		public ITest getService() {
			return service;
		}
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.xml.IXmlService#getMap()
		 */
		@Override
		public Map getMap() {
			return map;
		}
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.xml.IXmlService#getName()
		 */
		@Override
		public String getName() {
			return name;
		}
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.xml.IXmlService#getList()
		 */
		@Override
		public List getList() {
			return list;
		}
		/**
		 * 只能通过属性引用到它时，参数是声明的参数，当直接调用时，参数是调用时指定的参数，但返回的服务的处理是一致的
		 * @param tt
		 * @param mm
		 * @param obj
		 * @return
		 */
		public static Object newService(String tt,String mm,MyJsonService obj) {
			return new MyJsonService();
		}
		//演示非静态方法，静态非静态都一样的
		/* (non-Javadoc)
		 * @see your.crop.examples.chip2.xml.IXmlService#newService()
		 */
		@Override
		public IJsonService newService() {
			return new MyJsonService();
		}
	}
```	
	2.MyXmlService.xml
```xml

```


## Example of Using JavaScript as a Java-base Service in Web Development:
```javascript
	/*
	 * note：
	 * author：The extends keyword can define a type, which can then be accessed in Java by calling the service provider’s .getServices(type).
	 * <![jss:{
			scope:'runtime'
	 	}
	 ]>
	 <![desc:{
		ttt:'2323',
		obj:{
			name:'09skdkdk'
			}
	 * }]>
	 */
	//var imports = new JavaImporter(java.io, java.lang)导入类型的范围，单个用Java.type
	var Frame = Java.type('cj.studio.ecm.frame.Frame');
	var FormData = Java.type('cj.studio.ecm.frame.FormData');
	var FieldData = Java.type('cj.studio.ecm.frame.FieldData');
	var Circuit = Java.type('cj.studio.ecm.frame.Circuit');
	var ISubject = Java.type('cj.lns.chip.sos.website.framework.ISubject');
	var String = Java.type('java.lang.String');
	var CircuitException = Java.type('cj.studio.ecm.graph.CircuitException');
	var ServiceosWebsiteModule = Java
			.type('cj.lns.common.sos.website.moduleable.ServiceosWebsiteModule');
	var Gson = Java.type('cj.ultimate.gson2.com.google.gson.Gson');
	var StringUtil = Java.type('cj.ultimate.util.StringUtil');
	var Document = Java.type('org.jsoup.nodes.Document');
	var Jsoup = Java.type('org.jsoup.Jsoup');
	var WebUtil = Java.type('cj.studio.ecm.net.web.WebUtil');
	var IServicewsContext = Java.type('cj.lns.chip.sos.website.framework.IServicewsContext');
	var TupleDocument = Java.type('cj.lns.chip.sos.cube.framework.TupleDocument');
	var System = Java.type('java.lang.System');
	var colName='article.relatives';
	var HashMap = Java.type('java.util.HashMap');
	var SimpleDateFormat = Java.type('java.text.SimpleDateFormat');
	var Date = Java.type('java.util.Date');
	
	function createComment(map,cube,sws,circuit){
		var artid=map.get('artid');
		var to=map.get('to');
		var comment=map.get('comment');
		var thread=map.get('thread');
		var author='';//文章作者
		var reviewerface=sws.visitor().face();
		var reviewer=sws.visitor().principal();//评论者
		var ctime=System.currentTimeMillis();//评论时间
		//求作者
		var cjql="select {'tuple.creator':1} from tuple article.entities java.util.HashMap where {'_id':ObjectId('?(id)')}";
		var q=cube.createQuery(cjql);
		q.setParameter('id',artid);
		var doc=q.getSingleResult();
		author=doc.tuple().get('creator');
		if('@'==to){//评论给文章作者
			to=author;
		}else{
			to=to.substring(1,to.length);
		}
		
		var tuple=new HashMap();
		tuple.put('artid',artid);
		tuple.put('thread',thread);//所属的主贴
		tuple.put('reviewer',reviewer);
		tuple.put('reviewerFace',reviewerface);
		tuple.put('to',to);
		tuple.put('prefix','@');
		tuple.put('kind','comment');//评论类型
		tuple.put('comment',comment);
		tuple.put('ctime',ctime);
		var newdoc=new TupleDocument(tuple);
		var commentid=cube.saveDoc(colName,newdoc);
		
		var retmap=new HashMap();
		retmap.put('commentid',commentid);
		retmap.put('reviewerId',reviewer);
		retmap.put('to',to);
		retmap.put('reviewerFace',reviewerface);
		var format=new SimpleDateFormat("hh:mm MM月dd日");
		var timeDisplay=format.format(new Date(ctime));
		retmap.put('ctime',timeDisplay);
		circuit.content().writeBytes(new Gson().toJson(retmap).getBytes());
	}
	function delComment(map,cube,sws,circuit){
		var commentid=map.get('commentid');
		cube.deleteDoc(colName,commentid);
	}
	function toggleGreat(map,cube,sws,circuit){
		var artid=map.get('artid');
		//返回评论号，评论者的face
		var reviewerface=sws.visitor().face();
		var reviewer=sws.visitor().principal();//评论者
		//看看是否点了赞，如果是已点则取消
		var cjql="select {'tuple':'*'}.count() from tuple ?(colName) java.lang.Long where {'tuple.kind':'great','tuple.artid':'?(artid)','tuple.reviewer':'?(reviewer)'}";
		var q=cube.createQuery(cjql);
		q.setParameter('artid',artid);
		q.setParameter('colName',colName);
		q.setParameter('reviewer',reviewer);
		var count=q.count();
		if(count>0){//取消
			cube.deleteDocOne(colName,String.format("{'tuple.artid':'%s','tuple.reviewer':'%s','tuple.kind':'great'}",artid,reviewer));
			var retmap=new HashMap();
			retmap.put('reviewerId',reviewer);
			retmap.put('cancel','true');//是点赞还是取消
			retmap.put('reviewerFace',reviewerface);
			circuit.content().writeBytes(new Gson().toJson(retmap).getBytes());
		}else{
			var ctime=System.currentTimeMillis();//评论时间
			
			var tuple=new HashMap();
			tuple.put('artid',artid);
			tuple.put('reviewer',reviewer);
			tuple.put('reviewerFace',reviewerface);
			tuple.put('kind','great');//评论类型
			tuple.put('ctime',ctime);
			var newdoc=new TupleDocument(tuple);
			var greatid=cube.saveDoc(colName,newdoc);
			var retmap=new HashMap();
			retmap.put('reviewerId',reviewer);
			retmap.put('cancel','false');//是点赞还是取消
			retmap.put('reviewerFace',reviewerface);
			var format=new SimpleDateFormat("hh:mm MM月dd日");
			var timeDisplay=format.format(new Date(ctime));
			retmap.put('ctime',timeDisplay);
			circuit.content().writeBytes(new Gson().toJson(retmap).getBytes());
		}
	}
	function toggleFollow(map,cube,sws,circuit){
		var artid=map.get('artid');
		//返回评论号，评论者的face
		var reviewerface=sws.visitor().face();
		var reviewer=sws.visitor().principal();//评论者
		//看看是否关注了，如果是已点则取消
		var cjql="select {'tuple':'*'}.count() from tuple ?(colName) java.lang.Long where {'tuple.kind':'follow','tuple.artid':'?(artid)','tuple.reviewer':'?(reviewer)'}";
		var q=cube.createQuery(cjql);
		q.setParameter('artid',artid);
		q.setParameter('colName',colName);
		q.setParameter('reviewer',reviewer);
		var count=q.count();
		if(count>0){//取消
			cube.deleteDocOne(colName,String.format("{'tuple.artid':'%s','tuple.reviewer':'%s','tuple.kind':'follow'}",artid,reviewer));
			var retmap=new HashMap();
			retmap.put('cancel','true');//是关注还是取消
			circuit.content().writeBytes(new Gson().toJson(retmap).getBytes());
		}else{
			var ctime=System.currentTimeMillis();//评论时间
			var tuple=new HashMap();
			tuple.put('artid',artid);
			tuple.put('reviewer',reviewer);
			tuple.put('reviewerFace',reviewerface);
			tuple.put('kind','follow');//评论类型
			tuple.put('ctime',ctime);
			var newdoc=new TupleDocument(tuple);
			var greatid=cube.saveDoc(colName,newdoc);
			var retmap=new HashMap();
			retmap.put('cancel','false');//是点赞还是取消
			circuit.content().writeBytes(new Gson().toJson(retmap).getBytes());
		}
	}
	function doShare(map,cube,sws,circuit){
		
	}
	exports.flow = function(frame, circuit, plug, ctx) {
		var m = ServiceosWebsiteModule.get();
		var map=WebUtil.parserParam(new String(frame.content().readFully()));
		var sws=IServicewsContext.context(frame);
		var disk=m.site().diskOwner(sws.owner());
		if(!disk.existsCube(sws.swsid())){
			throw new CircuitException('404','视窗空间不存在');
		}
		var cube=disk.cube(sws.swsid());
		var action=map.get('action');
		switch(action){
		case 'comment':
			createComment(map,cube,sws,circuit);
			break;
		case 'great':
			toggleGreat(map,cube,sws,circuit);
			break;
		case 'follow':
			toggleFollow(map,cube,sws,circuit);
			break;
		case 'share':
			doShare(map,cube,sws,circuit);
			break;
		case 'delComment':
			delComment(map,cube,sws,circuit);
			break;
		}
		//circuit.content().writeBytes(String.format("{'id':'%s'}",phyId).getBytes())	;
	}

```
## Net Communication Development Toolkit

Summary:

The Net Toolkit supports a non-contact connection base package. It provides a Graph base class and a WebsiteGraph that supports web applications. It also offers communication capabilities, implemented as:

	•	Netty-based NIO with supported protocols: UDT/TCP/HTTP.
	•	RIO, based on Java NIO, supporting both synchronous and asynchronous operations with protocols: RIO-UDT/RIO-TCP/RIO-HTTP.

Main Body:

	•	Graph Base Class: This is the foundation for non-contact connections and implements stream processing. The related classes include GraphCreate, IPin, ISink, and IPlug. The Graph is assembled using GraphCreate, and a graph has both input and output terminals, which are visible outside the graph. There is also an internal terminal that is not visible outside the graph.

Stream Direction:
graph.in('input') -> graph.sinks -> graph.out('output')

Each Sink has a flow processing interface:

public void flow(frame, circuit, plug) { //TODO }

Where:

	•	frame represents the request detection,
	•	circuit refers to the currently executing loop or sequence,
	•	plug is the connector produced when the sink is inserted into a pin. The plug has scheduling capabilities, allowing for feedback, jumps, branching, or sequential processing. It also has servo capabilities, enabling access to the services in the chip and related content in the graph via plug.site().
	•	WebsiteGraph: It implements all web application protocols, abandoning the use of JSP. No more compilation is required for page code. Developers can implement page logic using Java classes, JSS (JavaScript files), or a mix of both. This component enables page development with Node.js-like syntax.

When using WebsiteGraph, developers should derive their graph from WebsiteGraph and declare it as a CJ service. Here’s an example of the context configuration for a WebsiteGraph, using the platform’s website implementation as a case study. Below is the context of its root assembly:

```json

	{
		entryPoint: {
			activators: [{
				name: "Service Operating System Launcher",
				class: "cj.lns.chip.sos.website.ServiceOSActivator",
				parameters: {}
			}]
		},
		assemblyInfo: {
			assemblyTitle: "serviceos",
			assemblyResource: "site=/site;http.root=$(site)/framework;http.jss=$(site)/jss/http;ws.jss=$(site)/jss/ws;resource=/resources",
			assemblyDescription: "Service Operating System",
			assemblyConfiguration: "",
			assemblyCompany: "Developer: CJ Research Institution: LNS Platform Department",
			assemblyProduct: "cj.lns.chip.sos.website",
			assemblyCopyright: "Copyright 2011",
			assemblyTrademark: "",
			assemblyCulture: "",
			guid: "serviceos",
			assemblyVersion: "1.0.0.0",
			assemblyFileVersion: "1.0.0.0",
			readme: "/readme.txt",
			assemblyIcon: "plugin-noicon-default.icon"
		},
		global: {
			default: "zh_CN",
			desc: "If the system environment requires a language that the chip does not support, the chip can use the default language."
		},
		serviceContainer: {
			name: "netContainer",
			switchFilter: "off",
			jss: [{
				module: "portlets",
				package: "site.framework.portlets",
				runtimeHome: "/work/",
				unzip: "true",
				searchMode: "link",
				extName: ".jss.js"
			}],
			scans: [{
				package: "cj.lns.chip.sos.website",
				extName: ".class|.json|.xml",
				exoterical: "true"
			}, {
				package: "cj.lns.common.sos.website.moduleable",
				extName: ".class|.json|.xml",
				exoterical: "true"
			}]
	
		}
	}
```
	We can see that serviceContainer.jss declares the JSS service module.
assemblyInfo.assemblyResource specifies the location of web resources, where ws represents the support for HTTP5 WebSocket.
	
- NIO Net: All NIO is based on the NettyServer and NettyClient classes. For example, for the TCP protocol, the following is the code, which should be implemented according to the actual API in use:

```java
	TcpNettyServer server=new TcpNettyServer();
	server.start('localhost','8080');
	server.buildNetGraph().netoutput().plug('testsink',new ISink(){
		function flow(frame,circuit,plug){
			print(frame);
		}
	})
	TcpNettyClient client=new TcpNettyClient();
	client.connect('localhost','8080',new ICallback(){
		void build(g){
		//构建sink链
		}
	})
	//发送请求
	client.buildNetGraph().netinput().flow(frame,circuit);
- RIO Net: All RIO is based on the BaseClientRIO and BaseServerRIO classes. For example, for the TCP protocol, the following is the code, which should be implemented according to the actual API in use:

	rio net的输入输出端子是电缆线，多通道，其客户端支持连接池，即向同一远程目标可以建立多个连接，默认是1个
	TcpCjNetServer server=new TcpCjNetServer();
	server.start('localhost','8080');
	server.buildNetGraph().netoutput().plug('testsink',new ISink(){
		function flow(frame,circuit,plug){
			print(frame);
		}
	})
	TcpCjNetClient client=new TcpCjNetClient();
	client.connect('localhost','8080',new ICallback(){
		void build(g){
		//构建sink链
		}
	})
	//To send a request, it is asynchronous by default. If you want a synchronous response, you can use:
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC,'true');
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT,'3600');//同步超时时间
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT,'3600');//同步超时时间
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_ASYNC_TIMEOUT,'1200');//同步等待超时后异步通知超时时间,使用此功能需要在回路中设置回馈点来接收
	client.buildNetGraph().netinput().flow(frame,circuit);
```
# The concept of connection-oriented programming.


 Keywords:

Connection-oriented programming, Modular programming, Protocol-oriented programming, Programming philosophy, Structured programming, Object-oriented programming, Java, J2EE, Spring, .NET, OSGi, Netty, Tomcat, Jetty, Node.js, JSP, PHP, ASPX, Zookeeper, Docker

Abstract:

“Open Sesame, and the door opened.” Since the discovery that logic can control entities, humanity has entered a new era of information control. To better organize logic, global elites have invented successive generations of philosophies, from fragmented assembly language to structured programming, connecting functions, to object-oriented programming, where functions are connected to form objects. Each wave of thought has been more advanced than the last, and evolution has followed this progression. Indeed, the world, as observed, is seen as a collection of isolated objects. These objects identify differences from the essence of the world and classify commonalities and differences. For observers, once the boundaries of entities are clear, logic also becomes clear. However, programming philosophy has stagnated in this state for many years, raising the question: “Has programming philosophy reached its limit? Is object-oriented programming the ultimate theory?”

My answer is: “No!” Object-oriented programming has a fatal flaw—high coupling. In the real world, things are always intertwined, interconnected, and when considering a larger system, one faces the same problem as fragmented programming: references are tangled, like a ball of string that is difficult to untangle. As software scale increases, the need for new programming philosophies grows, and the introduction of connection-oriented programming provides an excellent alternative.

Looking back at the history of programming, fragmented programming made developers focus on the entities themselves; structured programming made developers focus on the processes; object-oriented programming made developers focus on the relationships and attributes of entities. The development of programming philosophy is essentially the evolution of the methodology that guides developers in analyzing entities, with the focus gradually shifting from the entities themselves to the processes and, finally, to the concepts (objects). Connection-oriented programming, however, focuses on the connections between phenomena, using both static (object-oriented) and dynamic (runtime) connection structures. It contemplates how information flows within a static or dynamic logical grid, how it is controlled, and what formats are input and output. Thus, developers no longer focus on static structural interfaces but on the flow and format of information.

Referring to the “Creation” philosophy of God, the natural world is full of grids—small like neural networks, large like galaxies and cloud systems. All of these function within a vast grid, without direct contact, but rather through forces acting upon each other. In fact, even when a finger presses a key, there is no direct contact; the force that influences each other is the information. This is God’s way of decoupling phenomena. Therefore, connection-oriented programming can also be seen as a field of mechanics, offering significant insight and value for reference.

Main Text:

1. Concept Definition

Definition of Event:
In Chinese computing terminology, which has often borrowed directly from foreign terms, “Object” is usually translated as “对象” and “Class” is translated as “类.” From a rhetorical perspective, these translations are not entirely accurate. For example, in the sentence “I pick up this book,” if I were to represent it with three classes: “I,” “Pick Up,” and “The Book,” corresponding to three objects: “i,” “pick,” and “book,” the sentence would read awkwardly if we treat “Pick Up” as an object. Saying “Pick Up” is an object and the object “I” acts on the object “book” is strange. How can “Pick Up” be considered an object? In Chinese rhetoric, the term “object” typically refers to tangible entities that can be sensed by the five senses. It does not capture dynamic processes or actions in a temporal context. Therefore, these kinds of inaccurate “translation terms” should be avoided.

For humans, what is perceptible by the five senses is termed as “象” (Xiàng, or “image”), and dynamic, temporal processes are termed as “事” (Shì, or “event”). Thus, the term “事象” (Shìxiàng, or “phenomenon”) represents the essential structure of an “existence” and the life cycle of movement. In a phenomenon, both images and events coexist. When analyzing one image, one discovers that there are smaller images and events within it, and events and images transform endlessly, with events existing within images and vice versa. This process is infinite.

Continuing with the previous example, when I say: “I (this image), pick up (this event), and act on the book (this image),” the context becomes logically coherent.

Definition of Connection:
Connection itself is an event, specifically referring to the relationship between different entities. Things always interact with each other through connections. Connections can be divided into contact connections and non-contact connections. Contact connections are often embedded within the inherent structure of the entities, while non-contact connections, like “fluids” and “forces,” interact with entities without physical contact. Connection-oriented programming is a science that analyzes the connections between entities.

ECM:
Electronic Chip Connection Model. It is a tool that brings the idea of connection-oriented programming to life. ECM serves as a foundational event connection tool for non-contact connection tools (neuron, netsite), enabling analysis of the connections between entities.
	
2. Overview of the Role of Connection-Oriented Analysis

To give readers an initial understanding of connection-oriented programming, let’s begin with an example:

Example:
Jack transfers 100 yuan to Tom.

In an object-oriented analysis approach, abstract classes like User represent Jack and Tom, while the abstract class Transfer represents the transfer event (T1).
According to object-oriented analysis principles, we first perform a static structure analysis and then an activity diagram analysis. During the static structure analysis, we find that Transfer should depend on User. In the activity diagram, the instance of Transfer, T1, must hold references to Jack and Tom. Object-oriented analysis primarily guides developers in how to think about static structures, activities, and their dependencies.

In connection-oriented analysis, however, a new entity emerges between Jack and Tom: the transfer. This has directionality and can be expressed as:
Jack → T1 → Tom.

Next, when analyzing the transfer event (T1), we observe that when the transfer occurs, it deducts 100 yuan from Jack’s account and deposits 100 yuan into Tom’s account. This can be further described as:
T1 → Deduct 100 → Jack
and
T1 → Add 100 → Tom.

Here, T1 is the event (事), while Jack and Tom are the entities (象). If we consider the deduction and addition as sub-events of T1, then T1 creates two directed connections between Jack and Tom.

In contact-based connections, T1 operates via the references to Jack and Tom. In non-contact connections, T1 processes the “flow” passed by Jack and outputs the result to Tom.

Thus, while object-oriented programming focuses on static structure analysis and activity methods, connection-oriented analysis centers on the interactions and modes of contact between entities.

Therefore, when performing connection-oriented programming analysis, it is important to habitually ask:

	•	“What are these entities (象)?
	•	What events (事) are occurring between the entities?
	•	How do they connect?”

3. Within the Event-Entity (事象)

Event-Entity Factory

In daily life, we often do the following: when we need cabbage, we go to the fridge to take it out; when we need radish, we also go to the fridge; when we need vinegar and salt, we go to the kitchen cabinet where spices are kept. Extending this idea, when we need to handle an “event” (事), we always retrieve the “event-entity” from somewhere. This process resembles a box shared by all event-entities. The event-entity factory is crucial in connection-oriented programming. It is not only a means of decoupling the contact connections between event-entities but also more importantly, it provides “raw materials and driving force” for event-entities.

When event-entities require other event-entities, they retrieve them from the event-entity factory. Depending on the construction method, there are singleton and multi-instance event-entities. Depending on the lifecycle, there are compile-time event-entities (which have a fixed structure) and dynamically mutable event-entities. The event-entity factory manages the lifecycle of these shared event-entities.

In some cases, the event-entity factory includes logic that intercepts event requests. It also provides the ability to insert event-entities into the connection chain for interception when needed.

Inherent Structure

Inherent structure refers to the event-entity structure that is determined during the design phase. The connections between event-entities are compiled and solidified into the structure. This is the essential expression of the event-entity, and such structural connections are called “static connections.” Static connections tend to have a high degree of cohesion, which can lead to high complexity in some levels of referencing. Therefore, when decoupling is needed, the event-entity factory is naturally used to facilitate this process.

Temporal Characteristics of Event-Entities

After introducing the event-entity factory into the system, event-entities gain temporal characteristics. The temporal characteristics of event-entities are divided into two phases: construction-time and runtime.

	1.	Construction-Time: This phase involves connecting some properties and methods of the event-entity to certain event-entities in the factory at system startup. After the factory receives a request for an event-entity, it either constructs the event-entity or returns an existing one.
	2.	Runtime: During runtime, the system should support placing any event-entity into the factory and having the factory use the event-entity elsewhere.

There is another case where an event-entity actively connects itself to the methods or properties of another event-entity during construction. This situation is called a “reverse connection.”

The connections made during construction or runtime are generally referred to as “dynamic connections.” Static connections are a feature of object-oriented programming, while dynamic connections are a feature of connection-oriented programming.
	
4. Event-Entities Between Each Other

This section discusses the connection between one event-entity with an event-entity factory and another event-entity with its own event-entity factory. For simplicity, we assume there are three event-entities: one is the main event-entity (M). Within the main event-entity (M), event-entity A is connected to event-entity B. We refer to A as the external event-entity of B, and conversely, B is the external event-entity of A.

Contact

	1.	Event-Entity Instance Contact
Contact refers to connecting to an event-entity via references or pointers. An event-entity that has an event-entity factory divides its internal event-entities into two categories based on their external visibility: internal event-entities and external event-entities. As the name suggests, internal event-entities are inaccessible externally; they are the logical units within the event-entity. On the other hand, external event-entities are used for external connections, informing the external event-entity of the behaviors it should exhibit, which are processed by internal event-entities. The external event-entity does not need to be concerned with the internal details. This is similar to the distinction between public and private methods in object-oriented programming.
The connection between external event-entities and internal event-entities can either occur during construction or be connected as needed during runtime.
	2.	Event-Entity Structure Contact
In addition to connecting external event-entities to the instances of event-entities, another important aspect is that the types (classes or interfaces) within an event-entity can also be connected externally by being derived from or implemented by external event-entities. This provides an extension mechanism for external event-entities as dependent event-entities. This type of connection is widely used as an adaptation pattern and also supports plugin-based extension solutions.
By default, event-entity types are closed, and only types declared as open can be connected by external event-entities, ensuring the boundary of an event-entity is maintained.
An example of ECM event-entity contact is demonstrated in the development platform, which can be found in ECM-related articles. (This is omitted here.)

Non-Contact

Non-contact between event-entities refers to connecting to another event-entity without directly obtaining references or pointers, usually achieved through the form of “streams” and the public protocols of these streams. In this case, neither side holds an instance of the other event-entity. A typical example of a non-contact event-entity is a web service, where the net serves as the medium for connection. Similarly, a web container is a non-contact event-entity that handles HTTP streams. Non-contact event-entities exist in the following forms:

	1.	Complete Non-Contact: Often, the net acts as an isolator, where a service event-entity hands over the request to a flow event-entity for processing. In this case, the flow event-entity is an internal event-entity of the service event-entity, and this is the most common application.
	2.	Contact Only for Flow Event-Entities: This scenario involves only contacting event-entities that handle “streams” in the other event-entity. This is also common, but the external connection interface of the event-entity can predict the flow event-entity it contains.

Non-contact encourages developers to adopt a protocol-oriented programming approach, improving system scalability and promoting loose coupling. Non-contact modularizes event-entities and guides developers to think in terms of modular programming. When designing an event-entity, developers naturally need to consider what protocols it accepts, what it outputs, what external event-entities it interacts with, and how third parties can use it.

Summary

When event-entities are connected, they form a larger event-entity’s functionality. Changing the connections can alter the behavior of the larger event-entity, making connections crucial. This is particularly important for large distributed systems, where previous event-entities may need to connect to multiple subsequent event-entities. In practice, this introduces a certain level of complexity. Similar to how object-oriented programming analyzes classes, connection-oriented programming focuses on analyzing connections, which is highly flexible, challenging, and critical. Improper connection planning can lead to network misuse or performance bottlenecks.

An example of ECM event-entity non-contact can be found in the development platform, along with related articles on neurons (this is omitted here).
	

	
IV. Application of Connection-Oriented Programming

	1.	Plugin System Connection Implementation
In the diagram, we see the internal structure of the platform website of lns.com. Each rounded rectangle represents an assembly, which is the basic program unit of ECM. The large rectangle in the center is the main assembly, which connects various plugins. In the diagram, assemblies are modularized into plugins with hierarchical levels. Beneath the portal plugin, several other plugins are further extended. The plugins are interconnected through non-contact flow events, linking the request pathways between them.
	
![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/1.png)

The following diagram illustrates the structure and flow of plugin connections.

![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/2.png)


	



	2.	Distributed System Connection Implementation
![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/3.png)
The diagram shows the connection between two netsites, which are linked through their input and output terminals using a connected net method.
![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/4.png)	
The above diagram is an internal connection structure analysis of a non-contact event, StationGraph.
