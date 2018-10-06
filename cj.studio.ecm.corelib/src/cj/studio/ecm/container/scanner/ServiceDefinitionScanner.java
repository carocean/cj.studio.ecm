package cj.studio.ecm.container.scanner;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cj.studio.ecm.ICombinServiceDefinitionStrategy;
import cj.studio.ecm.IServiceDefinition;
import cj.studio.ecm.IServiceDefinitionRegistry;
import cj.studio.ecm.IServiceDefinitionResolver;
import cj.studio.ecm.IServiceDefinitionScanner;
import cj.studio.ecm.IServiceNameGenerator;
import cj.studio.ecm.IServiceScopeGenerator;
import cj.studio.ecm.Scope;
import cj.studio.ecm.container.describer.ExotericalTypeDescriber;
import cj.studio.ecm.container.describer.TypeDescriber;
import cj.studio.ecm.container.registry.ServiceScopeGenerator;
import cj.studio.ecm.resource.IResource;

public class ServiceDefinitionScanner implements IServiceDefinitionScanner {
	private IResource resource;
	private List<IServiceDefinitionResolver> resolvers;
	private IServiceNameGenerator serviceNameGenerator;
	private IServiceDefinitionRegistry registry;
	private IServiceScopeGenerator serviceScopeGenerator;
	private Map<String, Boolean> exotericalTypeNames;
	private ICombinServiceDefinitionStrategy combinServiceDefinitionStrategy;

	public ServiceDefinitionScanner(IServiceDefinitionRegistry registry,
			IResource resource) {
		this.resource = resource;
		this.registry = registry;
		this.resolvers = new ArrayList<IServiceDefinitionResolver>();
		exotericalTypeNames = new HashMap<String, Boolean>();
	}

	protected IServiceNameGenerator getNameGenerator() {
		return this.serviceNameGenerator;
	}

	protected IServiceScopeGenerator getScopeGenerator() {
		if (this.serviceScopeGenerator == null)
			serviceScopeGenerator = new ServiceScopeGenerator();
		return this.serviceScopeGenerator;
	}

	protected ICombinServiceDefinitionStrategy getCombinServiceDefinitionStrategy() {
		if (this.combinServiceDefinitionStrategy == null)
			combinServiceDefinitionStrategy = new CombinServiceDefinitionStrategy();
		return this.combinServiceDefinitionStrategy;
	}

	@Override
	public void setCombinServiceDefinitionStrategy(
			ICombinServiceDefinitionStrategy strategy) {
		this.combinServiceDefinitionStrategy = strategy;

	}

	@Override
	public void scan(String patternPath) {
		Pattern p = Pattern.compile(patternPath);
		Enumeration<String> names = this.resource.enumResourceNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Matcher m = p.matcher(name);
			boolean yesorno = m.matches();
			if (!yesorno)
				continue;
			for (IServiceDefinitionResolver dr : this.resolvers) {
				IServiceDefinition def = dr.resolve(name, this.resource);
				if (def == null)
					continue;

				// 收集开放类型
				if ((def != null)
						&& ((def.getServiceDescribeForm() & IServiceDefinition.EXOTERICALTYPE_DESCRIBEFORM) == IServiceDefinition.EXOTERICALTYPE_DESCRIBEFORM)) {
					List<TypeDescriber> list = def.getExtraDescribers();
					for (TypeDescriber t : list) {
						if (t instanceof ExotericalTypeDescriber) {
							ExotericalTypeDescriber e = (ExotericalTypeDescriber) t;
							this.exotericalTypeNames.put(
									e.getExotericalTypeName(), e.isPackage());
						}
					}
				}
				// 注册到注册表中的服务定义，必须是可实例化为服务实例的服务定义,所以服务描述属性不能为空
				// && ((def.getServiceDescribeForm() &
				// IServiceDefinition.SERVICE_DESCRIBEFORM) ==
				// IServiceDefinition.SERVICE_DESCRIBEFORM)
				if ((def != null) && (def.getServiceDescriber() != null)) {
					String serviceName = getNameGenerator()
							.generateServiceName(def, registry);
					String serviceScope = getScopeGenerator()
							.generateServiceScope(def, registry);
					def.getServiceDescriber().setScope(
							Scope.valueOf(serviceScope));
					def.getServiceDescriber().setServiceId(serviceName);
					// 合并同一服务的多种定义
					this.getCombinServiceDefinitionStrategy().combinServiceDefinition(
							def, this.registry);
				}
			}
		}
	}

	@Override
	public Map<String, Boolean> getExotericalTypeNames() {
		return this.exotericalTypeNames;
	}

	@Override
	public void setServiceNameGenerator(IServiceNameGenerator generator) {
		this.serviceNameGenerator = generator;
	}

	@Override
	public void addResolver(IServiceDefinitionResolver definitionResolver) {
		this.resolvers.add(definitionResolver);
	}

	@Override
	public void setServiceScopeGenerator(IServiceScopeGenerator generator) {
		// TODO Auto-generated method stub
		this.serviceScopeGenerator = generator;
	}

}
