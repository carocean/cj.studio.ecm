# ecm&net

关键词 keywords：j2ee,java,nodejs,osgi,jsp,struts,spring,spring mvc,mongodb,radis,zookeeper,netty,mina,jetty,tomcat,weblogic,websphere,orm,cloud,nashron,jdk

##ecm 面向连接电子模型开发工具包

一、作为服务容器，对服务定义的支持： 1.支持注解方式 2.支持xml方式 3.支持json方式 4.支持以上混合定义方式 5.支持反向注入 6.支持属性值、方法参数值注入，即便是注入代码段也可 7.支持任意类方法注入（spring仅支持工厂方法，无聊） 8.支持面向方面编程，切面拦截，功能强大，结构简单，甚至支持使用jss服务拦截java方法。简单活用 9.支持按类型搜索服务 10.支持按外部类的类别搜索服务 11.支持适配器对象。适配器对象可转换为任意类型，从而实现了服务的弱类型机制。

二、作为osgi容器，它以程序集（逻辑上叫芯片）作为支点，支持： 1.程序集的加载、启动、停止、卸载。 2.程序集之间的类型依赖及扩展 3.程序集之间的服务实例的强依赖 4.支持外部服务、外部类型，可在程序集外部获取到这些外部组件

三、作为nodejs容器，它拥有类似于nodejs的语法结构，在cj studio产品中均称之为jss服务，它具有： 1.exports导出外部函数（仿nodejs） 2.imports导入程序集及模块环境（能得到服务容器） 3.head对象，每个jss服务以该头对象声明服务 4.支持jss服务与java服务混用 5.支持多线程的jss服务 6.支持以jss开发web程序

	面向连接编程是一个完整的工具体系,从web的mvc到持久层，从开发工具到分布式部署，从本地工具到远程服务容器，一套完整的开发和部署体系，从而可以使用户摆脱那种拼拼补补的开发现状和运维状况。
	
面向连接的工具体系的优势在于，在架构层面，比spring更简单易用，比osgi更省事，比nodejs在服务器端的开发更规范，而且与java共生。它支持搭建像神经网络这样的大型分布式运维环境

	自2008年诞生，至今已被多家公司使用，为了持续发展这项技术，cj宣布正式开源，任何企业和个人均可无偿应用到其产品，仅需要在代码中注明：cj.studio
	
	其中的ecm开发工具包兼有spring、osgi、nodejs的功能。支持面向模块开发与部署，热插拔。 	
	其中的net开发工具包,支持web的开发，并且可以完全使用js开发web应用，它的语法更接近于流行的nodejs，其它功能包含有基于netty的nio也包含有自行开发的nio框架rio,rio叫响应式nio框架，它即有nio的优势，又有同步等待响应的能力。

##面向模块开发

	IAssambly assambly=Assambly.load("/home/cj/test/helloworld.jar");
	assambly.start();
	
	IWorkbin bin=assambly.workbin();
	IDepartment dept=bin.part('deptment');
	System.out.println(dept.getName());
	
	assambly.close();

## 使用注解声明服务

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
## 使用json声明服务

	1.java文件MyJsonService.java
	
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
	
## 使用xml声明服务

1.MyXmlService.java

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
	
	2.MyXmlService.xml
	
	
	
	
		
		
		
		
		
			
			fuck you
		
		
			
			
		
		
			
			new your.crop.examples.chip2.xml.ShowCreateObjectByValueParser();
		
		
			
			
			{"age1":"333","myObject":"$.myAnnoService"}
		
		
			
			
			["333","$.myAnnoService"]	
	 


## web开发使用js作为服务的示例：

	/*
	 * 说明：
	 * 作者：extends可以实现一种类型，此类型将可在java中通过调用服务提供器的.getServices(type)获取到。
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


## net 通讯开发工具包

摘要：
   net工具包支持非触接连接基础包，它提供了Graph基类，并提供了WebsiteGraph这种支持web应用的graph。它还提供了通讯能力，实现有：
- 以netty为基础的nio，支持协议有：udt/tcp/http 
- 直接以java nio为基础的rio，支持同步和异步，协议有：rio-udt/rio-tcp/rio-http 
正文： 
- Graph基类，它是非接触连接的基础类，实现了流式处理 涉及的类有：GraphCreate,IPin,ISink,IPlug等 Graph使用GraphCreate组装一个图，一个图有输入端子和输出端子，这两类端子在Graph外是可见的；还有一种内部端子，在Graph之外不可见。 流的方向性：
graph.in('input')->graph.sinks->graph.out('output') 
每个sink对流的处理接口是： 
public void flow(frame,circuit,plug){ //TODO } 
其中frame是请求侦，circuit代表一个正在执行的当前回路，即当前执行序。
plug是将sink插入到pin上时产生的插头，插头具有调度能力，可以主动回馈、跳跃、分支或顺行。插头还有伺服能力，能通过plug.site()获取芯片中的服务和所在graph的相关内容。 
- WebsiteGraph，它实现了全部的web应用协议，放弃了jsp这种拉圾,对于页面代码再也不用编译。对于页面的逻辑实现，开发者可以用java类开发，也可按jss(是js文件)来开发，也可混合使用。 因此，该组件实现了可以按nodejs语法来写页面。 在使用时，开发者可需从WebsiteGraph派生你的graph，而后声明为cj服务即可。 我们看一个websiteGraph的上下文配置，拿平台的website实现作为案例，其下是其根程序集的上下文：


	{
		entryPoint: {
			activators: [{
				name: "服务操作系统启动器",
				class: "cj.lns.chip.sos.website.ServiceOSActivator",
				parameters: {}
			}]
		},
		assemblyInfo: {
			assemblyTitle: "serviceos",
			assemblyResource: "site=/site;http.root=$(site)/framework;http.jss=$(site)/jss/http;ws.jss=$(site)/jss/ws;resource=/resources",
			assemblyDescription: "服务操作系统",
			assemblyConfiguration: "",
			assemblyCompany: "开发者：cj;研发机构：lns平台部",
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
			desc: "如果系统环境要求各芯片采用的语言本芯片没有，则芯片可采用默认语言"
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

	我们看到，serviceContainer.jss一项声明了jss服务模块。
	assemblyInfo.assemblyResource指明了web资源位置，其中的ws代表的是http5的websocket的支持
	
- nio net 所有nio基于NettyServer和NettyClient类 比如tcp协议，以下是为代码，在使用时根据实际的api来实现：

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
- rio net 所有rio基于BaseClientRIO,BaseServerRIO 比如tcp协议，以下是为代码，在使用时根据实际的api来实现：

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
	//发送请求，默认是异步发送，如果要同步返回，可使用：
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC,'true');
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT,'3600');//同步超时时间
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_SYNC_TIMEOUT,'3600');//同步超时时间
	//frame.head(NetConstans.FRAME_HEADKEY_CIRCUIT_ASYNC_TIMEOUT,'1200');//同步等待超时后异步通知超时时间,使用此功能需要在回路中设置回馈点来接收
	client.buildNetGraph().netinput().flow(frame,circuit);

# 面向连接编程思想


 关键词：
	面向连接编程 面向模块编程 面向协议编程 编程思想 结构化 面向对象
java j2ee spring dotnet osgi netty tomcat jetty nodejs jsp php aspx zookeeper docker

摘要：
	“芝麻开门，门于是就开了”，自从发现逻辑能够控制实体以来， 人类就迈进了信息控制的大时代。为了更好的组织逻辑，全球的精英为之发明了一代又一代的思想， 从断片式的汇编，到穿连成函数的结构化编程，再到穿连函数成对象的对象化编程，一波比一波先进，一浪逐一浪更高，这符合进化论。 没错，世界受观测者影响，被认为是由一个个的被隔离对象组成，他们从世界本相当中找出差异，分出共性和异性，因此，对于观测者来说，只要实体边界清楚了，逻辑也就清晰了。然而， 编程思想止乎此已有数年之久，我就有些犹虑：“难道编程思想就止步于此吗，面向对象编程就是终极理论？”
    	我的回答是：“非也！”面向对象编程有个致命的缺陷——高耦合。现象界的实事总是纷纷扰扰，互为苟合，当思考一个比较大的系统时， 面临着与断片式编程一样的问题，引用缠着引用，麻绳混成团终始不易解。而随着软件规模越做越大的今天，正是需要新的编程思想之际，面向连接编程的推出为之提供了一个不错的选项
    	回顾编程史，断片式编程让开发者关注在事物自身；结构化编程让开发者关注在过程之中，对象化编程，让开发者关注在事物的属性关系之内，因此， 编程思想的发展，实际上就是如何引导开发者分析事物的方法论的发展，关注的方面不断的升级，从事物－过程－概念（对象）。而面向连接编程，关注重点在事象之间的连接结构， 有静态连接结构（对象化）和动态连接结构（运行时）之用，思考的是信息在一个静态或动态逻辑网格中的如何流动，如何控制，输入什么格式、输出什么格式。因此， 开发者不再关注于静态结构的接口，而是关注在它的信息流径和格式。参考上帝的“编世”思想，自然界中到处都是网格化的，小如神经网络，大如星系云河，无一不是在一个大网格中产生效用，并且非以彼此触及而发生作用， 实事上，即便是手指按在键盘上，也并没有真实的接触，而是通过“力”作用于彼此，而力就是信息，这就是上帝解藕现象的办法。因此， 颇具有借鉴意义。由此来看，面向连接编程，也是一门力学。

正文： 
一、概念定义
	事象的定义：对于中国山寨得来的计算机科学来讲，通常以直译而来的“对象”表述英文中的Object，以“类”来表述Class，从修辞学上来讲，这是不准确的。比如，“我捡起这本书”这一句话，假如我以三个类来表述，分别是：I，Pick Up，The Book，分别对应三个对象：i,pick,book，当我把pick up当成一类时，放到这个语子中，我会说“拾起”是个对象，对象“我”，用对象“拾起”作用于对象“书”，意思就有些别扭了，“拾起”怎么会是对象？因为，对象在汉语的修辞中一般用于指六感所能感知的实在物，对于动态的、时态中的过程它并无此语境，因此必须正本清源，不再引用这类毫无准确性可言的“翻译性术语”。
	对于人类而言，六感所能感知的，命名为“象”，意识到的：动态的、时态的过程，皆命名为“事”，因此，事象代表了一个“存在”的结构本质和运动的生命期。而在一个事象之中，往往即有又有象又有事，当去分析其中的一个象时，发现又有小象和小事，且事、象互转，无止无息，象中有事、事中有象，类此不穷。
	接着前面的例子，当我说：我（这个象），拾起（这个事），作用于书（这个象）时，语境就合乎逻辑了。

	连接的定义：连接也是一种事件，具体指联系不同事物的事物。事物之间总是通过连接发生作用。连接可分为接触性连接和非接触性连接。接触性连接往往固化在事物的先天结构之中；非接触连接就象“流体”和“力”那样与彼事物发生作用。面向连接编程，就是一门分析事物之间的连接科学。
	ecm：电子芯片连接模型。它是面向连接编程思想的得以落地的工具，是非接触连接工具(neuron,netsite)的基础事象连接工具。
	
二、概述面向连接分析的作用
	为了让读者对面向连接编程有个初步的概念，故而先行在此段举个例子说明。
	比如：jack转账给tom 100元
	在面向对象的分析方法中，抽象类:User代表jack和tom，抽象类：Transfer代表转账T1
	按面向对象的分析规范，先做类静态结构分析，再做活动图分析。在静态结构分析时，我们发现Transfer应该依赖User，在活动分析时Transfer的实例T1要持有jack和tom的引用。面向对象分析法只是引导了开发者如何思考静态结构和活动以及它们的依赖关系。
	而在面向连接的分析方法中，jack这个事物与tom这个事物之间生成了一个新事物：转账，这是有方向性的，表述为：jack—>T1—>tom，再接下来分析，当事物T1发生转账时，它要向jack扣除款项100元，并使tom入账100元，则进一步表述为：
T1—>减除100—>jack
且
T1—>增加100—>tom
	这里，T1是事，jack/tom是象，如果将减、增看成是T1的子事，那么Ｔ1对于jack与tom就发生了两个有向连接。在接触连接中，T1通过jack和tom的引用进行操作，在非接触中，T1通过获得jack传入的“流”来做出处理，并将结果向tom“流”出。
	因此，面向对象编程的关注点在静态结构分析和活动方法上下功夫，而面向连接的分析方法中关注在事象间的作用与接触方式上。
	因此，在面向连接编程分析时，需要习惯性的这样发问：“这些象是什么？象与象之间发生了什么事？如何接触？”

三、事象之内
	事象工厂
	在生活中我们经常会这样做，在我们需要白菜时，会到冰箱拿，当需要萝卜时，也会到冰箱里拿，当我们需要醋和盐时，会到放调料的厨柜里拿。推而广之，在做一个“事”时，总会到某个地方拿出“事象”，这看起来就像个箱子，对所有的事象共享。事象工厂对于面向连接编程尤其重要，它不仅仅是解耦事象与事象之间接触连接的手段，更为重要的是它为事象提供了“原料和原动力”。
	事象在需要其它事象时从事象工厂里取出，在取出时，根据构建的方式不同，有单例事象和多例事象之分，根据生命期的不同，有编译期这种固定结构的事象，也有动态可变性的事象之别。	
	事象工厂负责管理这些共享事象的生命周期。
	在一些情况下，事象工厂设有拦载事象请求链路的逻辑，而且提供在链路连接上依需插入用于拦截的事象的能力。

	先天结构
	先天结构是指在设计期既确定下来的事象的结构，它与其它事象的连接被编译和固化在结构之中，它是事象的本质表达，这种结构性的连接，我们称之为“静连接”。静连接，具有高内聚形态，因此在一些引用层次之中，导致了较高的复杂度。因此在需要解藕时，自然要借助于事象工厂。

	事象时态
	将事象工厂引入系统内之后，事象就具有了时态特性。事象的时态分为：构建时态和运行时态两种；构建时态又分为单例构建、多例构建以及按需构建；在运行时态中，应该支持将任意一个在运行的事象放入事象工厂，并由事象工厂将事象用于别处。
	
	构建时态是在系统启动时，将事象的一些属性和方法连接到工厂的某些事象中，此时，工厂得到事象的请求后，将构建事象或将现有事象返回。
	还有一种情况，一个事象在构建时主动把自己连接到另一事相的方法或属性中，这种情况叫“反向连接”。
	这种在构建或运行时才连接到事象的连接，我们统称为“动连接”。
	静连接是面向对象编程的特性，动连接是面向连接编程的特性。
	
四、事象之间
	本节讨论一个拥有事象工厂的事象与另一个拥有事象工厂的事象之间的连接。为了简便期间，我们假定三个事象，一个是主事象M，在主事象M内，将事象A连接到事象B，我们把A称为B的外事项，反之，B就是A的外事象。
	接触
	1.事象实例的接触
	接触就是通过引用或指针连接到事象。一个拥有事象工厂的事象将其内的事象按对外的可见性分为两类，分别是内事象与外事象。故名思义，内事象在外部是访问不到的，它是事象内的逻辑单元；而外事象，用于外部的连接，它告诉外事象它应有的行为，其行为由内部的事象来处理，外部不必关心。这就类似于面向对象编程中一个类的public方法与private方法的区别。
	外事象对内事象的连接形式，即可在构建时连接，也可在运行时按需连接。
	2.事象结构的接触
	除了事象的实例可以被外部事象连接之外，还有一个重要的方面就是，事象内的类型（类或接口）也可被外部的事象以派生（或实现）的方式连接，它提供了一种外部事象作为所依赖事象的扩展手段。这种连接的用途也是非常广泛，首先它就是一种适配模式，其次它支持了插件式扩展方案。
	事象默认情况下类型是封闭的，只有被声明为开放的类型才可被外事象连接。以此保证一个事象的边界。
	其下演示ecm事象接触的例子，在本网的开发平台中找ecm相关文章，此处略。
	
	非接触
	事象之间非接触是指不直接通过获取引用或指针而达到与另一事象的连接，一般是通过“流”的形式，并通过公开流的协议而达到连接的目的，双方并不持有对方事象的实例。通常web service就是一种以net作为介质的非接触的事象，同样像web container也是一种以net作为处理http流的事象。非接触事象具有以下几种形式：
	1.完全非接触，往往以net作为隔离， 用于服务的事象在得到请求后交由流事象处理，在这种情况下，流事象属于所在事象的内部事象，这种应用最为常见。
	2.仅接触对方事象中的用于处理“流”的事象，这种应用也常见，但是事先能够预知获通过事象的外连接口发现其内包含的流事象。

	非接触引导开发者导向了面向协议编程，使得系统扩展性更好，更为松散。非接触使得事项模块化，又引导开发者导向了面向模块编程方向的思考。开发者在设计一个事象时，自然而然的要考虑这个事象接受什么、输出什么协议，有什么外部事象，要第三方怎么使用它，等等。

	总结：事象之间连接起来就是一个大事象的功能，变化一下连接就可能变易了大事象的形为，因此，连接是非常重要的，特别是对于大型分布式系统，前面的事象可能要与其后的多个事象连接，实际上也有一定的复杂度。像面向对象编程对类的分析一样，面向连接编程对连接的分析也是最灵活、最难的而且至为关键，连接规化的不合理，很可能就导致网络的滥用或性能的瓶颈。
	其下演示ecm事象非接触的例子，在本网的开发平台中找神经元相关文章，此处略。
	

	
六、面向连接编程的应用

	1.插件体系的连接实现
	图中是lns.com的平台网站的内部结构，每个圆角矩形实际上是一个程序集，程序集是是ecm的基本程序单元，中间的大距形是主程序集，它用于连接了各个插件。在图中可以看程序集被插件化，并有层级之别，在portal插件之下又扩展了几个插件。插件与插件之间以非接触的流事象将请求链路连接起来。
	
![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/1.png)

下图说是插件连接的结构及流向。

![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/2.png)


	



2.分布式体系的连接实现
![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/3.png)
图中是两个netsite之间的连接，它们通过输入与输出端子彼此以与接触的net方式相连
![Image text](https://github.com/carocean/cj.studio.ecm/blob/master/document/img/4.png)	
上图是一个非接触事象StationGraph的内部连接结构分析图。
