package it.unical.acr.qasp;

import java.util.ArrayList;

public class CNFProgramBuilder {

	private CNFProgram program = new CNFProgram();
	
	public void addRow(String [] row)  {
		if(row[0].equals("c")) {
			int var = Integer.parseInt(row[1]);
			int sign = Integer.signum(var);
			String lit = row[2];
			program.getLiteralToVar().put(lit, var*sign);
			program.getVarToLiteral().put(var*sign, lit);
		} 
		else if(row[0].equals("p")) {
			 //do nothing, preamble
		}		
		else {
			ArrayList<Integer> clause = new ArrayList<>();
			for(int i=0;i<row.length-1;i++) {
				clause.add(Integer.parseInt(row[i]));				
			}
			program.addClause(clause);
		}
	}
	
	public void addRow(String row) {
		addRow(row.split(" "));
	}

	public CNFProgram getProgram() {
		return program;
	}
	
}
