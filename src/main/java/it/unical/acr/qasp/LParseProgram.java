package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LParseProgram {

	Map<String, Integer> literalToVar = new HashMap<>();
	Map<Integer, String> varToLiteral = new HashMap<>();
	private ArrayList<ArrayList<Integer>> rules = new ArrayList<>();

	public LParseProgram() {
		super();
	}

	public Map<String, Integer> getLiteralToVar() {
		return literalToVar;
	}

	public Map<Integer, String> getVarToLiteral() {
		return varToLiteral;
	}

	public void addRule(ArrayList<Integer> rule) {
		rules.add(rule);
	}

	@Override
	public String toString() {
		
		
		StringBuilder sb = new StringBuilder();
		//sb.append("LPARSE program:\n");
		for(ArrayList<Integer> rule: rules) {
			for(Integer var: rule) {
				sb.append(var + " ");
			} 
			sb.append("\n");
		}
		sb.append("0\n");
		for(String lit: literalToVar.keySet()) {
			sb.append(literalToVar.get(lit)+" "+lit+"\n");
		}
		sb.append("0\n");
		sb.append("B+\n");
		sb.append("0\n");
		sb.append("B-\n");
		sb.append("1\n");
		sb.append("0\n");
		sb.append("1\n");
	

		return sb.toString();
		
	}

	public void addStrongNegationConstraints() {
		for(String lit: literalToVar.keySet()) {
			if(literalToVar.containsKey("-"+lit)) {
				rules.add(new ArrayList<>(Arrays.asList(1, 1, 2, 0, literalToVar.get(lit), literalToVar.get("-"+lit))));
			}
		}
	}

}
