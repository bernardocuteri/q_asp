package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.List;

import it.unical.mat.dlv.program.Program;

public class QuantifiedProgram {

	List<Program> programs = new ArrayList<>();
	List<String> quantifiers = new ArrayList<>();
	
	

	public QuantifiedProgram(List<Program> programs, List<String> quantifiers) {
		super();
		this.programs = programs;
		this.quantifiers = quantifiers;
	}

	public QuantifiedProgram() {
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
	
	

}
