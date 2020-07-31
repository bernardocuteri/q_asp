package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.List;

import it.unical.mat.dlv.program.Program;

public class AspQProgram {

	List<Program> programs = new ArrayList<>();
	List<String> quantifiers = new ArrayList<>();
	
	

	public AspQProgram(List<Program> programs, List<String> quantifiers) {
		super();
		this.programs = programs;
		this.quantifiers = quantifiers;
	}

	public AspQProgram() {
		super();
	}

	public List<Program> getPrograms() {
		return programs;
	}

	public void setPrograms(List<Program> programs) {
		this.programs = programs;
	}

	public List<String> getQuantifiers() {
		return quantifiers;
	}

	public void setQuantifiers(List<String> quantifiers) {
		this.quantifiers = quantifiers;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<programs.size();i++) {
			sb.append("%"+quantifiers.get(i)+"\n");
			sb.append(programs.get(i).toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	

}
