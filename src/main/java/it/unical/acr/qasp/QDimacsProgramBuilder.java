package it.unical.acr.qasp;

import java.util.ArrayList;

public class QDimacsProgramBuilder {

	private QDimacsProgram program = new QDimacsProgram();
	
	private ArrayList<Integer> getList(String [] row, int start) {
		ArrayList<Integer> res = new ArrayList<>();
		for(int i=start;i<row.length-1;i++) {
			res.add(Integer.parseInt(row[i]));				
		}
		return res;
		
	}
	public void addRow(String [] row)  {
		if(row[0].equals("c")) {
			//do nothing, comment
		} 
		else if(row[0].equals("p")) {
			 //do nothing, preamble
		}
		else if(row[0].equals("e")) {
			program.addQuantifiedLayer(getList(row, 1));
			program.addQuantifier("e");
		}
		else if(row[0].equals("a")) {
			program.addQuantifiedLayer(getList(row, 1));
			program.addQuantifier("a");
		}
		else {
			program.addClause(getList(row, 0));
		}
	}
	
	public void addRow(String row) {
		addRow(row.split(" "));
	}

	public QDimacsProgram getProgram() {
		return program;
	}
	
}
