package cj.studio.ecm.domain;

import cj.studio.ecm.global.ILanguage;
import cj.studio.ecm.logging.ILogging;
import cj.studio.ecm.util.ObjectmMedium;

public abstract class AbstractDomain implements IDomain {
	protected final ObjectmMedium medium;

	public AbstractDomain(ObjectmMedium medium) {
		this.medium = medium;
	}

	@Override
	public ILanguage getLanguage() {
		ILanguage lang = (ILanguage) medium.get(ILanguage.class.getName());
		return lang;
	}
	@Override
	public ILogging getLogging() {
		ILogging log=(ILogging)medium.get(ILogging.class.getName());
		return log;
	}
}
