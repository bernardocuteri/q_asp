package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.List;

public class QCIRProgram {
	
	private List<List<String> > quantifiedVariables = new ArrayList<>();
	private List<String> quantifiers = new ArrayList<>();;	
	private List<GateDefinition> gateDefinitions = new ArrayList<>();;
	
	public QCIRProgram() {
		super();
	}
	
	void addQuantifiedLayer(List<String> vars, String quantifier) {
		quantifiedVariables.add(vars);
		quantifiers.add(quantifier);
	}
	
	void addGateDefinition(GateDefinition gd) {
		gateDefinitions.add(gd);
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		//use QCIR version 13
		sb.append("#QCIR-13\n");
		
		//append quantifications
		for(int i=0;i<quantifiers.size();i++) {
			sb.append(quantifiers.get(i)+"(");
			for(int j=0;j<quantifiedVariables.get(i).size();j++) {
				if(j>0) {
					sb.append(",");
				}
				sb.append(quantifiedVariables.get(i).get(j));
			}
			
			sb.append(")\n");
		}
		
		sb.append("output(psi_0)\n");
		
		for(GateDefinition gd: gateDefinitions) {
			sb.append(gd.toString()+"\n");
		}
		
		return sb.toString();
	}
	
	

}
