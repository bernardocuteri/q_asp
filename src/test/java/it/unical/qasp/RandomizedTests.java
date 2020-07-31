package it.unical.qasp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.IntStream;

import org.junit.Test;

import it.unical.acr.qasp.AspQProgram;
import it.unical.acr.qasp.Conversions;
import it.unical.acr.qasp.QAsp;
import it.unical.acr.qasp.QAspResult;
import it.unical.acr.qasp.QDimacsProgram;
import it.unical.acr.qasp.Utilities;
import it.unical.mat.random.MainGenerator;

public class RandomizedTests {

	
	//@Test
	public void executeRandomTestsWithMatUnicalRandom() {
		List<String> args = new ArrayList<>();
		//args.add("-h");
		args.add("-o=temp/random.asp");
		args.add("-generator=CIGenerator");
		args.add("-out=MultiOutput");
		args.add("-formats=PrintQBF,PrintQCIR,PrintAspQ");
		args.add("-E=2");
		args.add("-A=2");
		args.add("-e=1");
		args.add("-a=1");
		args.add("-c=1");
		args.add("-w=2");
		MainGenerator.main(args.toArray(new String[0]));
		QAsp.DEBUG_LEVEL = Level.FINEST;
		QAsp solver = new QAsp();		
		solver.setInputFile(new File("temp/random.asp.aspq"));
		boolean aspqResult = solver.solve().isSat();
		boolean qcirResult = QAsp.solveQcirProgram(new File("temp/random.asp.qcir"));
		System.out.println(aspqResult ? "SAT" : "UNSAT"); 
		assertEquals(qcirResult, aspqResult);		
	}
	
	public synchronized void updateResult(TestResults res, boolean sat, boolean fail, long aspQElapsed, long caqeElapsed) {
		res.tests++;
		res.satisfiables +=  sat ? 1: 0;
		res.failed += fail ? 1 : 0;
		res.aspQElapsed += aspQElapsed;
		res.caqeElapsed += caqeElapsed;
		if(res.tests % 1 == 0)  {
			long threadId = Thread.currentThread().getId();
	        System.out.println("Thread # " + threadId);
			System.out.println(res);
		}
	}
	
	public void randomTestWithBlocksqbf(TestResults res, boolean stopOnFail) {
		QAsp.DEBUG_LEVEL = Level.FINEST;
		List<String> randomQDimacs = Utilities.generateRandomQDimacsInstance();
		
		
		File randomQDimacsFile = Utilities.writeToTempFile(randomQDimacs);
		long start = System.nanoTime();
		boolean caqeResult = Utilities.caqe(randomQDimacsFile);		
		randomQDimacsFile.delete();
		long caqeElapsed = System.nanoTime() - start;
		
		QDimacsProgram qDimacsProgram = Utilities.parseQDimacs(randomQDimacs);
		AspQProgram program = Conversions.qDimacsToAspQ(qDimacsProgram);
		
		start = System.nanoTime();
		QAspResult qAspREsult = new QAsp().solve(program);
		long qAspElapsed = System.nanoTime() - start;
		
		updateResult(res, caqeResult, caqeResult != qAspREsult.isSat(), qAspElapsed, caqeElapsed);
		if(stopOnFail) {
			assertEquals(caqeResult, qAspREsult.isSat());
		}		
	}
	
	@Test
	public void executeSingleRandomTestsWithBlocksqbf() {
		randomTestWithBlocksqbf(new TestResults(), true);
	}
	
	public void executeRandomTestsWithBlocksqbf(TestResults res) {
		
		while(true) {
			randomTestWithBlocksqbf(res, false);			
		}		
	}
	
	@Test
	public void parallelRandomTests() {
		int paralleFactor = 1;//Math.min(10, Runtime.getRuntime().availableProcessors()/2);
		IntStream stream = IntStream.range(0, paralleFactor);
		
		TestResults res = new TestResults();
		stream.parallel().forEach(i -> executeRandomTestsWithBlocksqbf(res));
	}
	
	private class TestResults {
		int tests;
		int satisfiables;
		int failed;
		long aspQElapsed;
		long caqeElapsed;
		@Override
		public String toString() {
			return "TestResults [tests=" + tests + ", satisfiables=" + satisfiables + ", failed=" + failed
					+ ", aspQElapsed=" + TimeUnit.NANOSECONDS.toMillis(aspQElapsed) + " ms, caqeElapsed=" + TimeUnit.NANOSECONDS.toMillis(caqeElapsed) + " ms]";
		}
	}
	
}
