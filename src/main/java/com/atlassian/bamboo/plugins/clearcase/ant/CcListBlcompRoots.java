package com.atlassian.bamboo.plugins.clearcase.ant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.atlassian.bamboo.plugins.clearcase.utils.StringSplitter;

/**
 * Given a baseline get a List of all component directories
 * that would need to be loaded.
 */
public class CcListBlcompRoots extends Task {

	private CcLsbl lsbl = new CcLsbl();
	private CcLscomp lscomp = new CcLscomp();
	private String baseline;
	
	private String outputProp;
	private Set<String> loadRules = new HashSet<String>();
	
	@Override
	public void execute() throws BuildException {
		lsbl.setProject(getProject());
		lscomp.setProject(getProject());
		lscomp.setFormat("%[root_dir]p");
		loadRules.clear();
		processBaseline(getBaseline(), loadRules);
		if(StringUtils.isNotBlank(outputProp))
		{
			getProject().setProperty(outputProp, StringUtils.join(loadRules.iterator(),","));
		}
		System.out.println("### component roots: "+loadRules);
	}
	
	/**
	 * Get hold of list of components that are required for the given baselines.
	 * @return the set of load rules @NotNull
	 */
	public Set<String> getLoadRules()
	{
		return this.loadRules;
	}
	
	
	/**
	 * Recursiveliy get the baseline componet root directories.
	 * 
	 * @param baseline the baseline name
	 * @param loadRules the list to add component to.
	 */
	private void processBaseline(String baseline, Set<String> loadRules)
	{
		String comp = getBaselineComponent(baseline);
		lscomp.setObjSelect2(comp);
		lscomp.execute();
		String compRoot = lscomp.getCommandOutput();
		if(StringUtils.isNotBlank(compRoot))
		{
			loadRules.add(compRoot);
		}
		
		List<String> children = getDependsBaselines(baseline);
		for(String child : children)
		{
			processBaseline(child, loadRules);
		}
	}

	private List<String> getDependsBaselines(String baseline) {
		lsbl.setFormat("%[depends_on]Xp");
		lsbl.setObjSelect2(baseline);
		lsbl.execute();
		StringSplitter sp = new StringSplitter(lsbl.getCommandOutput(),"\\s",false);
		sp.setIgnoreEmptyToken(true);
		sp.setIgnoreEmptyToken(true);
		return sp.getItems();
	}

	private String getBaselineComponent(String baseline2) {
		lsbl.setFormat("%[component]Xp");
		lsbl.setObjSelect2(baseline2);
		lsbl.execute();
		return lsbl.getCommandOutput();
	}

	/**
	 * @return the baseline
	 */
	public String getBaseline() {
		return baseline;
	}

	/**
	 * @param baseline the baseline to set
	 */
	public void setBaseline(String baseline) {
		this.baseline = baseline;
	}

	/**
	 * @return the outputProp
	 */
	public String getOutputProp() {
		return outputProp;
	}

	/**
	 * @param outputProp the outputProp to set
	 */
	public void setOutputProp(String outputProp) {
		this.outputProp = outputProp;
	}
	
	

}
