package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.List;

public class QCIRProgram {

	public enum QCIRFormatType {
		STANDARD, CLEANSED
	}
	
	public static final String OR = "or";
	public static final String AND = "and";

	private QCIRFormatType format;
	private List<List<String>> quantifiedVariables = new ArrayList<>();
	private List<String> quantifiers = new ArrayList<>();
	private List<GateDefinition> gateDefinitions = new ArrayList<>();
	private String output;

	public QCIRProgram(QCIRFormatType format) {
		super();
		this.format = format;
	}

	public List<List<String>> getQuantifiedVariables() {
		return quantifiedVariables;
	}

	public List<String> getQuantifiers() {
		return quantifiers;
	}

	public List<GateDefinition> getGateDefinitions() {
		return gateDefinitions;
	}
	
	public QCIRFormatType getFormat() {
		return format;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}
	
	

}
