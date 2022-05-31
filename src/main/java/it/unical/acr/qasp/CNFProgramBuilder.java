package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class CNFProgramBuilder {

	private CNFProgram program = new CNFProgram();
	private HashSet<Integer> externals = new HashSet<>();
	
	private HashSet<int[]> clausesUnique = new HashSet<>();
	
	public void addRow(String [] row)  {
		if(row[0].equals("c")) {
			int var = Integer.parseInt(row[1]);
			int sign = Integer.signum(var);
			String lit = row[2];
			program.getLiteralToVar().put(lit, var*sign);
			program.getVarToLiteral().put(var*sign, lit);
			externals.add(var*sign);
		} 
		else if(row[0].equals("p")) {
			 //do nothing, preamble
		}		
		else {
			ArrayList<Integer> clause = new ArrayList<>();
			int[] cla = new int[row.length-1];
			for(int i=0;i<row.length-1;i++) {
				int rowI = Integer.parseInt(row[i]);
				cla[i] = rowI;
				clause.add(rowI);
				externals.remove(rowI);
			}
			Arrays.sort(cla);
			if(!clausesUnique.contains(cla))
			{
				clausesUnique.add(cla);
				program.addClause(clause);
			}
		}
	}
	
	private ArrayList<Integer> buildTautologyForExternals() {
		ArrayList<Integer> clause = new ArrayList<>();
		int tonegate=0;
		for (int i : externals)
		{
			clause.add(i);
			tonegate=i;
		}
		clause.add(-1*tonegate);
		return clause;
	}

	
	public void addRow(String row) {
		addRow(row.split(" "));
	}

	public CNFProgram getProgram() {
		program.addClause(buildTautologyForExternals());
		return program;
	}
	
}
