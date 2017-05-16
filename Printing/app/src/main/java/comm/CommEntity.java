package comm;

import comm.entity.BasePrinter;
import comm.entity.PrinterSP_EU58IIIExCommEntity;

import java.util.HashMap;

public class CommEntity {
	private final static HashMap<Integer,BasePrinter> hsmpBasePrinter = new HashMap<Integer,BasePrinter>();
	public static BasePrinter getPrinter(int id) {
		synchronized(hsmpBasePrinter) {
			BasePrinter basePrinter = hsmpBasePrinter.get(id);
			if(basePrinter == null) {
				basePrinter = new PrinterSP_EU58IIIExCommEntity(id,null);
				hsmpBasePrinter.put(id, basePrinter);
			}
			return basePrinter;
		}
	}
	public static void init() {
		synchronized(hsmpBasePrinter) {
			PrinterSP_EU58IIIExCommEntity printer = new PrinterSP_EU58IIIExCommEntity(1,null);
			printer.init();
			hsmpBasePrinter.put(1, printer);
		}
	}
}
