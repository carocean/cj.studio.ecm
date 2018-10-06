
关键词 keywords：j2ee,java,nodejs,osgi,jsp,struts,spring,spring mvc,mongodb,radis,zookeeper,netty,mina,jetty,tomcat,weblogic,websphere,orm,cloud,nashron,jdk

	面向连接编程是一个完整的工具体系,从web的mvc到持久层，从开发工具到分布式部署，从本地工具到远程服务容器，一套完整的开发和部署体系，从而可以使用户摆脱那种拼拼补补的开发现状和运维状况。
	
面向连接的工具体系的优势在于，在架构层面，比spring更简单易用，比osgi更省事，比nodejs在服务器端的开发更规范，而且与java共生。它支持搭建像神经网络这样的大型分布式运维环境

	自2008年诞生，至今已被多家公司使用，为了持续发展这项技术，cj宣布正式开源，任何企业和个人均可无偿应用到其产品，仅需要在代码中注明：cj.studio
	
	其中的ecm开发工具包兼有spring、osgi、nodejs的功能。支持面向模块开发与部署，热插拔。 	
	其中的net开发工具包,支持web的开发，并且可以完全使用js开发web应用，它的语法更接近于流行的nodejs，其它功能包含有基于netty的nio也包含有自行开发的nio框架rio,rio叫响应式nio框架，它即有nio的优势，又有同步等待响应的能力。

示例和第三方开发的开放源码托管到 github, codeproject, sourceforge，oschina, csdn，基本每日更新。


web开发使用js作为服务的示例：

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


