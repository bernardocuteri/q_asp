package it.unical.acr.qasp;

import java.util.List;

public class GateDefinition {

	private String var;
	private String gateType;
	private List<String> inputs;

	public GateDefinition(String var, String gateType) {
		super();
		this.var = var;
		this.gateType = gateType;
	}

	public GateDefinition(String var, String gateType, List<String> inputs) {
		super();
		this.var = var;
		this.gateType = gateType;
		this.inputs = inputs;
	}

	public String getVar() {
		return var;
	}

	public void setVar(String var) {
		this.var = var;
	}

	public String getGateType() {
		return gateType;
	}

	public void setGateType(String gateType) {
		this.gateType = gateType;
	}

	public List<String> getInputs() {
		return inputs;
	}

	public void setInputs(List<String> inputs) {
		this.inputs = inputs;
	}

	public void addInput(String input) {
		inputs.add(input);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(var+" = "+gateType + "(");
		for (int i = 0; i < inputs.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(inputs.get(i));

		}
		sb.append(")");
		return sb.toString();
	}
}
