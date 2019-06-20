package br.uff.ic.merge.logicalcoupling;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Cristiane
 */

public class Dependency_Information {


	//private List<Float> values;
	
	private float values;
	private int coupling;

	private Map<EditedMethod, Set<EditedMethod>> dependencyMap;

	public Dependency_Information(Map<EditedMethod, Set<EditedMethod>> dependencyMap, float values, int coupling) {
		this.dependencyMap = dependencyMap;
		this.values = values;
		this.coupling = coupling;
	}

	public Map<EditedMethod, Set<EditedMethod>> getDependencyMap() {
		return dependencyMap;
	}

	public void setDependencyMap(Map<EditedMethod, Set<EditedMethod>> dependencyMap) {
		this.dependencyMap = dependencyMap;
	}  
	
	public float getValues() {
		return this.values;
	}

	public void setValues(float values) {
		this.values = values;
	}
	
	public int getCoupling() {
		return this.coupling;
	}

	public void setCoupling(int coupling) {
		this.coupling = coupling;
	}



}
