package comm;

import comm.entity.PrinterSP_EU58IIIExCommEntity;

import java.util.HashMap;

public class CommEntity {
    private final static HashMap<Integer, PrinterSP_EU58IIIExCommEntity> hsmpBasePrinter = new HashMap<Integer, PrinterSP_EU58IIIExCommEntity>();

    public static PrinterSP_EU58IIIExCommEntity getPrinter(int id) {
        synchronized (hsmpBasePrinter) {
            PrinterSP_EU58IIIExCommEntity basePrinter = hsmpBasePrinter.get(id);
            if (basePrinter == null) {
                basePrinter = new PrinterSP_EU58IIIExCommEntity(id, null);
                basePrinter.init();
                hsmpBasePrinter.put(id, basePrinter);
            }
            return basePrinter;
        }
    }

    public static void init() {
        //try {
        synchronized (hsmpBasePrinter) {
            PrinterSP_EU58IIIExCommEntity printer = new PrinterSP_EU58IIIExCommEntity(1, null);
            printer.init();
            hsmpBasePrinter.put(1, printer);
        }
        //} catch(Exception e) {
        //	Log.e("init: ", "Error", e);
        //}
    }
}
