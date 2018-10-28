注入的三种方式：
	1.java类的方式
	2.json注入
	3.XML方式
	支持1，2，废除第3
	
JSON表示方法和引用服务的方式,表示类、属性和方法，并可以在别的json中使用
	{
		name:'myClass',
		class:'Cj.MyClass',
		constructor:['arg1','$car1'],
		properties:{car:'$car1.name',
					dog:'#cj.Dog',
					name:'ddd',
					processMethod:'$myClass.process'
					}
					//默认为空构建，如果定义了构造则使用此构造创建实例
		//花括号内的对象以hashMap表示
		methods:{
					process:['cj.MyClass2']
				}
	}
	
	$表示按名称引用，#表示按类型引用,构造器的意思是初始化这个服务需要调用此构造器
	方法定义定义方法模板，即可在其它的Json中注入，如此例定义了方法process，
	并在本类中引用为属性：processMethod，方法在其它类中只能作为属性引用
	
	class与注解的类名有校验关系，如果省略将在当前包中找类
类的注解有以下几种组合形式
CjService

CjService
CjServiceProvider

CjService
CjObjectWrapper

