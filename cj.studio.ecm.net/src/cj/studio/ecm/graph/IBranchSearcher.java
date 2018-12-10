package cj.studio.ecm.graph;

import java.util.HashMap;

import cj.ultimate.IDisposable;

public interface IBranchSearcher extends IDisposable {
	IBranchKey found();
	IPin search(String input, HashMap<IBranchKey, IPin> branches);

}
