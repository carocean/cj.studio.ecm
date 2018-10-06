package cj.studio.ecm.script;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public interface IJssModuleCallback {
	void newJss(IJssDefinition def, ScriptObjectMirror m);
}
