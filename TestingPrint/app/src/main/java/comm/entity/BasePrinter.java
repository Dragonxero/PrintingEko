package comm.entity;

import android.util.Log;

import com.google.code.microlog4android.Logger;

import common.utils.IOUtils;
import common.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class BasePrinter {
    private final static Logger logger = Logger.getLogger("PrinterCommEntity");
    private static final String FORMAT_MARK = "[FORMAT]";

    private static class PrinterCmd {
        private static final String MODEL = "[MODEL]";
        private static final String RESET = "[RESET]";
        private static final String SETTINGS = "[SETTINGS]";
        private static final String BARCODE = "[BARCODE]";
        private static final String QRCODE = "[QRCODE]";
        private static final String LINE = "[LINE]";
        private static final String IMAGE = "[IMAGE]";
        private static final String TEXTIMAGE = "[TEXTIMAGE]";
        private static final String CODE = "[CODE]";
        private static final String TEXT = "[TEXT]";
        private static final String DATA = "[DATA]";
        private static final String CUT = "[CUT]";
    }

    protected static class PrinterOptions {
        public static final String SETTINGS_STRONG = "STRONG";
        public static final String SETTINGS_UNDERLINE = "UNDERLINE";
        public static final String SETTINGS_HIGHSIZE = "HIGHSIZE";
        public static final String SETTINGS_WIDESIZE = "WIDESIZE";
        public static final String SETTINGS_COLOR = "COLOR";
        public static final String SETTINGS_CHARSET = "CHARSET";
        public static final String SETTINGS_ALIGN = "ALIGN";

        public static final String ALIGN_LEFT = "LEFT";
        public static final String ALIGN_CENTER = "CENTER";
        public static final String ALIGN_RIGHT = "RIGHT";

        public static final String COLOR_WHITE = "WHITE";
        public static final String COLOR_BLACK = "BLACK";

        public static final String CUT_ALL = "ALL";
        public static final String CUT_HALF = "HALF";
    }

    public static interface PrinterCmdExecutor {
        boolean execute(String str, HashMap<String, String> hsmpParam);
    }

    ;
    private HashMap<String, PrinterCmdExecutor> hsmpPrinterCmdExecutor = new HashMap<String, PrinterCmdExecutor>();
    private static final PrinterCmdExecutor PrinterCmdExecutorNULL = new PrinterCmdExecutor() {
        @Override
        public boolean execute(String str, HashMap<String, String> hsmpParam) {
            return true;
        }
    };

    public BasePrinter() {
        addPrinterCmdExecutor(FORMAT_MARK, PrinterCmdExecutorNULL);
        addPrinterCmdExecutor(PrinterCmd.MODEL, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdModel(str, hsmpParam);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.RESET, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdReset(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.SETTINGS, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdSetting(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.BARCODE, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdBarcode(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.QRCODE, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdQRCode(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.LINE, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdLine(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.IMAGE, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdImage(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.TEXTIMAGE, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdTextImage(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.CODE, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdCode(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.TEXT, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdText(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.DATA, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdData(str);
            }
        });
        addPrinterCmdExecutor(PrinterCmd.CUT, new PrinterCmdExecutor() {
            @Override
            public boolean execute(String str, HashMap<String, String> hsmpParam) {
                return cmdCut(str);
            }
        });
    }

    protected void addPrinterCmdExecutor(String cmd, PrinterCmdExecutor printerCmdExecutor) {
        hsmpPrinterCmdExecutor.put(cmd, printerCmdExecutor);
    }

    private String previewCmd(String cmd, String str, HashMap<String, String> hsmpParam) {
        if (cmd.equals(PrinterCmd.TEXT)) {
            return str;
        }
        if (cmd.equals(PrinterCmd.BARCODE)) {
            return str;
        }
        if (cmd.equals(PrinterCmd.LINE)) {
            return "\n";
        }
        if (cmd.equals(PrinterCmd.MODEL)) {
            if (!StringUtils.isBlank(str)) {
                HashMap<String, String> hsmpValue = StringUtils.toHashMap(str, ";", "=");
                String filename = hsmpValue.get("FILE");
                String resource = hsmpValue.get("RESOURCE");
                if (filename != null || resource != null) {
                    byte[] data = readFile(filename, resource);
                    return preview(new String(data), hsmpParam);
                }
            }
        }
        return "";
    }

    public String preview(String str, HashMap<String, String> hsmpParam) {
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isBlank(str))
            return sb.toString();
        if (hsmpParam != null) {
            if (hsmpParam.size() > 0) {
                String[][] convert = new String[hsmpParam.size()][];
                int count = 0;
                Iterator<String> iter = hsmpParam.keySet().iterator();
                while (iter.hasNext()) {
                    String key = iter.next();
                    String value = hsmpParam.get(key);
                    convert[count] = new String[]{key, value};
                    count++;
                }
                str = StringUtils.convert(str, convert);
            }
        }
        str = StringUtils.replace(StringUtils.replace(str, "\r\n", "\n"), "\r", "\n");
        int strLen = str.length();
        List<Integer> listPStart = new ArrayList<Integer>();
        List<String> listPCmd = new ArrayList<String>();
        int pos = 0;
        while (pos < strLen) {
            int idx = str.indexOf('[', pos);
            if (idx == -1) {
                break;
            } else {
                int nextIdx = str.indexOf('[', idx + 1);
                int endIdx = str.indexOf(']', idx + 1);
                if (endIdx != -1) {
                    if (nextIdx == -1 || endIdx < nextIdx) {
                        String cmd = str.substring(idx, endIdx + 1);
                        PrinterCmdExecutor printerCmdExecutor = hsmpPrinterCmdExecutor.get(cmd);
                        if (printerCmdExecutor != null) {
                            listPStart.add(idx);
                            listPCmd.add(cmd);
                        }
                        pos = endIdx + 1;
                        continue;
                    }
                    pos = nextIdx;
                    continue;
                }
                break;
            }
        }
        if (listPStart.size() > 0) {
            for (int i = 0; i < listPStart.size(); i++) {
                int start = listPStart.get(i);
                String cmd = listPCmd.get(i);
                String text = null;
                if ((i + 1) < listPStart.size()) {
                    text = str.substring(start + cmd.length(), listPStart.get(i + 1));
                } else {
                    text = str.substring(start + cmd.length());
                }
                if (text.endsWith("\n")) {
                    text = text.substring(0, text.length() - 1);
                }
                text = StringUtils.convert(text, convert);
                sb.append(previewCmd(cmd, text, hsmpParam));
            }
        }
        return sb.toString();
    }

    public boolean print(String str, HashMap<String, String> hsmpParam) {
        //try {
            if (StringUtils.isBlank(str)) {
                return true;
            }
            int idx = str.indexOf(FORMAT_MARK);
            if (idx != -1) {
                if (str.substring(0, idx).trim().length() == 0) {
                    return execPrinterCmdExecutor(printerFormatExecutor, str.substring(idx + FORMAT_MARK.length()), hsmpParam);
                }
            }
        //}catch(Exception e){
        //    Log.e("Print: ", "Error", e);
        //}
        return execPrinterCmdExecutor(printerNormalExecutor, str, hsmpParam);
    }

    private final PrinterCmdExecutor printerFormatExecutor = new PrinterCmdExecutor() {
        @Override
        public boolean execute(String str, HashMap<String, String> hsmpParam) {
            return formatPrint(str, hsmpParam);
        }
    };
    private final PrinterCmdExecutor printerNormalExecutor = new PrinterCmdExecutor() {
        @Override
        public boolean execute(String str, HashMap<String, String> hsmpParam) {
            return normalPrint(str, hsmpParam);
        }
    };

    protected boolean execPrinterCmdExecutor(PrinterCmdExecutor printerCmdExecutor, String str, HashMap<String, String> hsmpParam) {
        return printerCmdExecutor.execute(str, hsmpParam);
    }

    private final static String[][] convert = {
            {"\\\\", "\\"},
            {"\\r", "\r"},
            {"\\n", "\n"},
            {"\\t", "\t"},
    };

    protected boolean normalPrint(String str, HashMap<String, String> hsmpParam) {
        return formatPrint("[FORMAT][RESET][TEXT]" + str, hsmpParam);
    }

    private boolean formatPrint(String str, HashMap<String, String> hsmpParam) {
        if (StringUtils.isBlank(str))
            return true;
        if (hsmpParam != null) {
            if (hsmpParam.size() > 0) {
                String[][] convert = new String[hsmpParam.size()][];
                int count = 0;
                Iterator<String> iter = hsmpParam.keySet().iterator();
                while (iter.hasNext()) {
                    String key = iter.next();
                    String value = hsmpParam.get(key);
                    convert[count] = new String[]{key, value};
                    count++;
                }
                str = StringUtils.convert(str, convert);
            }
        }
        str = StringUtils.replace(StringUtils.replace(str, "\r\n", "\n"), "\r", "\n");
        int strLen = str.length();
        List<Integer> listPStart = new ArrayList<Integer>();
        List<String> listPCmd = new ArrayList<String>();
        int pos = 0;
        while (pos < strLen) {
            int idx = str.indexOf('[', pos);
            if (idx == -1) {
                break;
            } else {
                int nextIdx = str.indexOf('[', idx + 1);
                int endIdx = str.indexOf(']', idx + 1);
                if (endIdx != -1) {
                    if (nextIdx == -1 || endIdx < nextIdx) {
                        String cmd = str.substring(idx, endIdx + 1);
                        PrinterCmdExecutor printerCmdExecutor = hsmpPrinterCmdExecutor.get(cmd);
                        if (printerCmdExecutor != null) {
                            listPStart.add(idx);
                            listPCmd.add(cmd);
                        }
                        pos = endIdx + 1;
                        continue;
                    }
                    pos = nextIdx;
                    continue;
                }
                break;
            }
        }
        if (listPStart.size() > 0) {
            for (int i = 0; i < listPStart.size(); i++) {
                int start = listPStart.get(i);
                String cmd = listPCmd.get(i);
                PrinterCmdExecutor printerCmdExecutor = hsmpPrinterCmdExecutor.get(cmd);
                String text = null;
                if ((i + 1) < listPStart.size()) {
                    text = str.substring(start + cmd.length(), listPStart.get(i + 1));
                } else {
                    text = str.substring(start + cmd.length());
                }
                if (text.endsWith("\n")) {
                    text = text.substring(0, text.length() - 1);
                }
                text = StringUtils.convert(text, convert);
                logger.debug(printerCmdExecutor.getClass().getName() + ":[" + text + "]");
                if (!execPrinterCmdExecutor(printerCmdExecutor, text, hsmpParam))
                    return false;
            }
            return true;
        } else {
            PrinterCmdExecutor printerCmdExecutor = hsmpPrinterCmdExecutor.get(PrinterCmd.TEXT);
            return execPrinterCmdExecutor(printerCmdExecutor, str, hsmpParam);
        }
    }

    protected abstract boolean reset();

    protected abstract boolean setSettings(HashMap<String, String> hsmpSettings);

    protected abstract boolean printLine();

    protected abstract boolean printText(String text);

    protected abstract boolean printQRCode(String mode, String qrCode);

    protected abstract boolean printBarcode(String mode, String barcode);

    protected abstract boolean printData(byte[] data, int offset, int count);

    protected abstract boolean printImage(String filename, String resource, String width, String height);

    protected abstract boolean printTextImage(String str);

    protected abstract String execPrintCode(HashMap<String, String> hsmpValue);

    protected abstract boolean cutPaper(String mode);

    private boolean cmdModel(String str, HashMap<String, String> hsmpParam) {
        if (StringUtils.isBlank(str))
            return true;
        HashMap<String, String> hsmpValue = StringUtils.toHashMap(str, ";", "=");
        String filename = hsmpValue.get("FILE");
        String resource = hsmpValue.get("RESOURCE");
        if (filename != null || resource != null) {
            byte[] data = readFile(filename, resource);
            return formatPrint(new String(data), hsmpParam);
        }
        return true;
    }

    private byte[] readFile(String filename, String resource) {
        byte[] data = null;
        if (!StringUtils.isBlank(filename)) {
            File file = new File(filename);
            if (file.isFile() && file.length() > 0) {
                data = IOUtils.readFile(filename);
                if (data != null) {
                    if (data.length == 0)
                        data = null;
                }
            }
        }
        if (data == null && !StringUtils.isBlank(resource)) {
            data = IOUtils.readResource(resource);
            if (data != null) {
                if (data.length == 0)
                    data = null;
            }
        }
        return data;
    }

    private boolean cmdReset(String str) {
        return reset();
    }

    private boolean cmdSetting(String str) {
        if (StringUtils.isBlank(str))
            return true;
        HashMap<String, String> hsmpSettings = StringUtils.toHashMap(str, ";", "=");
        if (hsmpSettings.size() > 0) {
            return setSettings(hsmpSettings);
        }
        return true;
    }

    private boolean cmdBarcode(String str) {
        if (StringUtils.isBlank(str))
            return true;
        int idx = str.indexOf(':');
        if (idx > 0) {
            String mode = str.substring(0, idx);
            String barcode = str.substring(idx + 1).trim();
            return printBarcode(mode.trim(), barcode);
        }
        return true;
    }

    //Changed from private to public
    public boolean cmdQRCode(String str) {
        if (StringUtils.isBlank(str))
            return true;
        int idx = str.indexOf(':');
        if (idx > 0) {
            String mode = str.substring(0, idx);
            String qrCode = str.substring(idx + 1);
            return printQRCode(mode.trim(), qrCode);
        }
        return true;
    }

    private boolean cmdLine(String str) {
        return printLine();
    }

    private boolean cmdImage(String str) {
        if (StringUtils.isBlank(str))
            return true;
        HashMap<String, String> hsmpValue = StringUtils.toHashMap(str, ";", "=");
        String filename = hsmpValue.get("FILE");
        String resource = hsmpValue.get("RESOURCE");
        String width = hsmpValue.get("WIDTH");
        String height = hsmpValue.get("HEIGHT");
        if (filename != null || resource != null)
            return printImage(filename, resource, width, height);
        return true;
    }

    private boolean cmdTextImage(String str) {
        if (StringUtils.isBlank(str))
            return true;
        return printTextImage(str);
    }

    private boolean cmdCode(String str) {
        if (StringUtils.isBlank(str))
            return true;
        HashMap<String, String> hsmpValue = StringUtils.toHashMap(str, ";", "=");
        return formatPrint(execPrintCode(hsmpValue), null);
    }

    private boolean cmdText(String str) {
        if (StringUtils.isEmpty(str))
            return true;
        return printText(str);
    }

    private boolean cmdData(String str) {
        if (StringUtils.isBlank(str))
            return true;
        HashMap<String, String> hsmpValue = StringUtils.toHashMap(str, ";", "=");
        if (hsmpValue.size() == 0)
            return true;
        String RADIX = hsmpValue.get("RADIX");
        String DIV = hsmpValue.get("DIV");
        if (DIV == null) {
            DIV = " ";
        } else if (DIV.toUpperCase().indexOf("SPACE") != -1) {
            DIV = " ";
        } else if (DIV.length() != 1) {
            DIV = " ";
        }
        int radix = 16;
        if (!StringUtils.isBlank(RADIX))
            radix = Integer.parseInt(RADIX);
        String filename = hsmpValue.get("FILE");
        String resource = hsmpValue.get("RESOURCE");
        String value = null;
        if (filename != null || resource != null) {
            byte[] data = readFile(filename, resource);
            if (data != null) {
                value = StringUtils.replace(StringUtils.replace(StringUtils.replace(new String(data), "\n", DIV), "\r", DIV), "\t", DIV);
            }
        }
        if (value == null) {
            value = hsmpValue.get("DATA");
        }
        if (value != null) {
            String[] items = value.split(DIV);
            byte[] data = new byte[items.length];
            int dataLen = 0;
            for (int i = 0; i < items.length; i++) {
                items[i] = items[i].trim();
                if (items[i].length() == 0)
                    continue;
                int n = Integer.valueOf(items[i], radix);
                data[dataLen] = (byte) (n & 0xFF);
                dataLen++;
            }
            if (dataLen > 0) {
                return printData(data, 0, dataLen);
            }
        }
        return true;
    }

    private boolean cmdCut(String str) {
        if (StringUtils.isBlank(str))
            return cutPaper(null);
        return cutPaper(str.trim());
    }
}
