package comm;

import comm.entity.PrinterSP_EU58IIIExCommEntity;
import comm.entity.PrinterTPM80CommEntity;

import java.util.HashMap;

public class CommEntity {

    /*private final static HashMap<Integer, PrinterTPM80CommEntity> hsmpBasePrinter = new HashMap<Integer, PrinterTPM80CommEntity>();

    public static PrinterTPM80CommEntity getPrinter(int id) {
        synchronized (hsmpBasePrinter) {
            PrinterTPM80CommEntity basePrinter = hsmpBasePrinter.get(id);
            if (basePrinter == null) {
                basePrinter = new PrinterTPM80CommEntity(id, null);
                basePrinter.init();
                hsmpBasePrinter.put(id, basePrinter);
            }
            return basePrinter;
        }
    }

    public static void init() {
        //try {
        synchronized (hsmpBasePrinter) {
            PrinterTPM80CommEntity printer = new PrinterTPM80CommEntity(1, null);
            printer.init();
            hsmpBasePrinter.put(1, printer);
        }
        //} catch(Exception e) {
        //	Log.e("init: ", "Error", e);
        //}
    }
    */

    private  final static HashMap<Integer, PrinterSP_EU58IIIExCommEntity> hsmpBasePrinter = new HashMap<>();

    public static PrinterSP_EU58IIIExCommEntity getPrinter(int id) {
        synchronized (hsmpBasePrinter) {
            PrinterSP_EU58IIIExCommEntity basePrinter = hsmpBasePrinter.get(id);
            if(basePrinter == null) {
                basePrinter = new PrinterSP_EU58IIIExCommEntity(id, null);
                basePrinter.init();;
                hsmpBasePrinter.put(id, basePrinter);
            }
            return basePrinter;
        }
    }

    public static void  init() {
        synchronized (hsmpBasePrinter) {
            PrinterSP_EU58IIIExCommEntity printer = new PrinterSP_EU58IIIExCommEntity(1, null);
            printer.init();
            hsmpBasePrinter.put(1, printer);
        }
    }
}
