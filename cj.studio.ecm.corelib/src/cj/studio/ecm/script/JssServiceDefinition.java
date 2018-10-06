package cj.studio.ecm.script;

import java.io.File;

import javax.script.Bindings;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

 class JssServiceDefinition implements IJssDefinition {
		 ScriptObjectMirror head;
		 String ownerModule;
		 String source;
		 JssDecriber decriber;
		 String relateName;
		 Bindings imports;

		public JssServiceDefinition(ScriptObjectMirror head,
				String ownerModule, String relateName, String source,
				 Bindings imports) {
			this.head = head;
			decriber = JssDecriber.parse(head);
			this.ownerModule = ownerModule;
			this.source = source;
			this.relateName = relateName;
			this.imports = imports;
		}
		public JssServiceDefinition() {
		}
		@Override
		public String selectScriptName() {
			return String.format("%s%s['%s']",IJssModule.$_CJ_JSS_DOT, ownerModule,relateName	);
		}
		@Override
		public Bindings importsDomain() {
			// TODO Auto-generated method stub
			return imports;
		}
		public void setHead(ScriptObjectMirror head) {
			this.head = head;
			decriber = JssDecriber.parse(head);
		}
		public void importsDomain(Bindings b){
			this.imports=b;
		}
		public String location(String extName) {
			return String.format("%s%s",relateName.replace(".", File.separator),extName);
		}

		@Override
		public String source() {
			// TODO Auto-generated method stub
			return source;
		}

		@Override
		public ScriptObjectMirror getHead() {
			return head;
		}

		@Override
		public String ownerModule() {
			// TODO Auto-generated method stub
			return ownerModule;
		}
		public String relateName() {
			return relateName;
		}
		@Override
		public String selectName() {
			return String.format("%s%s.%s",IJssModule.$_CJ_JSS_DOT, ownerModule,relateName	);
		}
		@Override
		public JssDecriber getDecriber() {
			return decriber;
		}

	}