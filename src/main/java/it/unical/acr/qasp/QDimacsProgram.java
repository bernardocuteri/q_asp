package it.unical.acr.qasp;

import java.util.ArrayList;

public class QDimacsProgram {

	private ArrayList<ArrayList<Integer>> quantifiedLayers = new ArrayList<>();
	private ArrayList<String> quantifiers = new ArrayList<>();
	private ArrayList<ArrayList<Integer>> clauses = new ArrayList<>();

	public ArrayList<ArrayList<Integer>> getClauses() {
		return clauses;
	}

	public void setClauses(ArrayList<ArrayList<Integer>> clauses) {
		this.clauses = clauses;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int nVars = quantifiedLayers.stream().mapToInt(e -> e.size()).reduce(0, (subtotal, element) -> subtotal + element);
		sb.append("p cnf "+ nVars+" "+clauses.size()+"\n");
		for(int i=0;i<quantifiedLayers.size();i++) {
			sb.append(quantifiers.get(i));
			for(int j=0;j<quantifiedLayers.get(i).size();j++) {
				sb.append(" "+quantifiedLayers.get(i).get(j));
			}
			sb.append(" 0\n");
		}
		for(ArrayList<Integer> clause:clauses) {
			for(Integer var:clause)  {
				sb.append(var+" ");
			}
			sb.append("0\n");
		}
		return sb.toString();
	}

	public void addClause(ArrayList<Integer> clause) {
		clauses.add(clause);
		
	}

	public void addQuantifiedLayer(ArrayList<Integer> list) {
		quantifiedLayers.add(list);
		
	}

	public void addQuantifier(String q) {
		quantifiers.add(q);
		
	}

	public ArrayList<ArrayList<Integer>> getQuantifiedLayers() {
		return quantifiedLayers;
	}

	public void setQuantifiedLayers(ArrayList<ArrayList<Integer>> quantifiedLayers) {
		this.quantifiedLayers = quantifiedLayers;
	}

	public ArrayList<String> getQuantifiers() {
		return quantifiers;
	}

	public void setQuantifiers(ArrayList<String> quantifiers) {
		this.quantifiers = quantifiers;
	} 
	
	

}
