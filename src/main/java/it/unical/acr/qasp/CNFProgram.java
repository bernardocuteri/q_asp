package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CNFProgram {

	Map<String, Integer> literalToVar = new HashMap<>();
	Map<Integer, String> varToLiteral = new HashMap<>();
	
	private ArrayList<ArrayList<Integer>> clauses = new ArrayList<>();

	public Map<String, Integer> getLiteralToVar() {
		return literalToVar;
	}

	public void setLiteralToVar(Map<String, Integer> literalToVar) {
		this.literalToVar = literalToVar;
	}

	public Map<Integer, String> getVarToLiteral() {
		return varToLiteral;
	}

	public void setVarToLiteral(Map<Integer, String> varToLiteral) {
		this.varToLiteral = varToLiteral;
	}

	public ArrayList<ArrayList<Integer>> getClauses() {
		return clauses;
	}

	public void setClauses(ArrayList<ArrayList<Integer>> clauses) {
		this.clauses = clauses;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(ArrayList<Integer> clause:clauses) {
			for(Integer var:clause)  {
				sb.append(var+" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void addClause(ArrayList<Integer> clause) {
		clauses.add(clause);
		
	} 

}
