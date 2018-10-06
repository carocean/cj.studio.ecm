package cj.studio.ecm.domain;

import cj.studio.ecm.global.ILanguage;
import cj.studio.ecm.logging.ILogging;

//域，一个芯片一个资源，故而也属于芯片的域。
//域属于当前类所在的范围，故而也是一个资源的范围
public interface IDomain {

	ILanguage getLanguage();
	ILogging getLogging();
}
