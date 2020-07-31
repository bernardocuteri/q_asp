package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.List;

public class QAspResult {

	private boolean sat;
	private List<List<String>> qaspAssignments = new ArrayList<>();
	private List<List<String>> qcirAssignments = new ArrayList<>();

	public QAspResult(boolean sat) {
		super();
		this.sat = sat;
	}

	public QAspResult() {
	}

	public boolean isSat() {
		return sat;
	}

	public void setSat(boolean sat) {
		this.sat = sat;
	}

	public List<List<String>> getQaspAssignments() {
		return qaspAssignments;
	}

	public void setQaspAssignments(List<List<String>> qaspAssignments) {
		this.qaspAssignments = qaspAssignments;
	}

	public List<List<String>> getQcirAssignments() {
		return qcirAssignments;
	}

	public void setQcirAssignments(List<List<String>> qcirAssignments) {
		this.qcirAssignments = qcirAssignments;
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append(sat ? "SAT" : "UNSAT");
		if (!qaspAssignments.isEmpty()) {
			res.append("\nOutermost existential models");
		}
		for (List<String> assignment : qaspAssignments) {
			res.append("\n{");
			for (String atom : assignment) {
				if (atom != assignment.get(0)) {
					res.append(", ");
				}
				res.append(atom);
			}
			res.append("}");
		}
		return res.toString();
	}

	public void addQaspAssignment(ArrayList<String> assignment) {
		qaspAssignments.add(assignment);

	}
	
	public void addQcirAssignment(ArrayList<String> assignment) {
		qcirAssignments.add(assignment);

	}

}
