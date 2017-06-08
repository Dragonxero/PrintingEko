package comm.entity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.code.microlog4android.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android_serialport_api.SerialPort;
import common.SysConfig;
import common.SysGlobal;
import common.json.JSONUtils;
import common.queue.FIFOQueue;
import common.task.TaskAction;
import common.task.TickTaskMgr;
import common.utils.EncryptUtils;
import common.utils.IOUtils;
import common.utils.StringUtils;

public class PrinterSP_EU58IIIExCommEntity extends BasePrinter {
    private final static Logger logger = Logger.getLogger("PrinterCommEntity");
    private SerialPort printerSerialPort = null;
    private FIFOQueue printerFIFOQueue = new FIFOQueue();
    private HardwareAlarmState paperState = HardwareAlarmState.UNKNOWN;
    private HardwareAlarmState realPaperState = HardwareAlarmState.UNKNOWN;
    private HardwareAlarmState printerState = HardwareAlarmState.UNKNOWN;
    private int errorTimes = 0;
    private long lHasNoPaper = 0;
    private long lHasNoPaperDelayCheck = 300000;
    private boolean checkStateEnable = true;
    private int baud = 9600;

    private int id;
    private CommEventListener commEventListener;

    private static final Locale CL = new Locale("es", "CL");

    private static enum HardwareAlarmState {
        UNKNOWN, NORMAL, ALARM
    }

    public PrinterSP_EU58IIIExCommEntity(int id, CommEventListener commEventListener) {
        this.id = id;
        this.commEventListener = commEventListener;
        lHasNoPaperDelayCheck = Long.parseLong(SysConfig.get("COM.PRINTER.PAPER.CHECK.MAXDURATION")) * 1000;
        TickTaskMgr.getTickTaskMgr().register(new TaskAction() {
            @Override
            public void execute() {
                checkPaperState();
            }
        }, Integer.parseInt(SysConfig.get("COM.PRINTER.PAPER.CHECK.INTERVAL")), false);
    }

    private void raiseEvent(HashMap hsmpEvent) {
        if (commEventListener != null) {
            commEventListener.apply(hsmpEvent);
        }
    }

    public void init() {
        synchronized (this) {
            try {
                if (printerSerialPort == null) {
                    String baudStr = SysConfig.get("COM.PRINTER" + id + ".BAND");
                    String confPrinter = String.format(CL, "%s%d.%s", "COM.PRINTER", id, SysConfig.get("PLATFORM"));
                    String device = SysConfig.get(confPrinter);
                    //SysConfig.get("COM.PRINTER" + id + "." + SysConfig.get("PLATFORM"));
                    //String device = SysConfig.get("COM.PRINTER1.arm");

                    logger.info(String.format("PrinterSP_EU58IIIExCommEntity: %s : Dispositivo: %s Bauds: %s", confPrinter, device, baudStr));

                    baud = Integer.parseInt(baudStr);
                    printerSerialPort = new SerialPort(new File(device), baud, 0);
                    paperState = HardwareAlarmState.UNKNOWN;
                    realPaperState = HardwareAlarmState.UNKNOWN;
                    printerState = HardwareAlarmState.UNKNOWN;
                    errorTimes = 0;
                    SysGlobal.execute(new RecvThread());
                    SysGlobal.execute(new InitThread());
                    SysGlobal.execute(printThread);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                logger.error("PrinterSP_EU58IIIExCommEntity: Error al iniciar impresora", e);
            }
        }
    }

    public String execute(String cmd, String json) throws Exception {
        init();
        try {
            checkStateEnable = false;
            if ("CHECK:START".equalsIgnoreCase(cmd)) {
                return null;
            }
            if ("CHECK:END".equalsIgnoreCase(cmd)) {
                return null;
            }
            if (("PRINTER" + id + ":CHECK_OPEN").equalsIgnoreCase(cmd)) {
                return (printerSerialPort != null) ? "TRUE" : "FALSE";
            }
            if (("PRINTER" + id + ":state").equalsIgnoreCase(cmd)) {
                return execute("state", null);
            }
            if (("PRINTER" + id + ":init").equalsIgnoreCase(cmd)) {
                return execute("init", null);
            }
            if (("PRINTER" + id + ":printer").equalsIgnoreCase(cmd)) {
                HashMap hsmpPrinter = JSONUtils.toHashMap(json);
                String printModel = (hsmpPrinter == null) ? null : (String) hsmpPrinter.get("MODEL");
                if ("TRUE".equalsIgnoreCase(SysConfig.get("PRINTER.DEBUG"))) {
                    printModel = initPrintModel(printModel);
                }
                HashMap hsmpParam = (hsmpPrinter == null) ? null : (HashMap) hsmpPrinter.get("PARAM");
                //String printModel = "[FORMAT][MODEL]FILE=/sdcard/rvm/voucher_model.txt";
                print(printModel, hsmpParam);
            }
            if (("PRINTER" + id + ":cut").equalsIgnoreCase(cmd)) {
                return execute("cut", null);
            }
            if ("enable".equalsIgnoreCase(cmd)) {
                if (printerSerialPort != null
                        && realPaperState != HardwareAlarmState.ALARM
                        && printerState != HardwareAlarmState.ALARM
                        )
                    return "TRUE";
                else
                    return "FALSE";
            }
            if ("reset".equals(cmd)) {
                printerFIFOQueue.reset();
                return null;
            }
            if ("init".equals(cmd)) {
                return null;
            }
            if ("printer".equals(cmd)) {
                HashMap hsmpPrinter = JSONUtils.toHashMap(json);
                String printModel = initPrintModel((hsmpPrinter == null) ? null : (String) hsmpPrinter.get("MODEL"));
                HashMap hsmpParam = (hsmpPrinter == null) ? null : (HashMap) hsmpPrinter.get("PARAM");
                //String printModel = "[FORMAT][MODEL]FILE=/sdcard/rvm/voucher_model.txt";
                print(printModel, hsmpParam);
            }
            if ("preview".equals(cmd)) {
                HashMap hsmpPrinter = JSONUtils.toHashMap(json);
                String printModel = initPrintModel((hsmpPrinter == null) ? null : (String) hsmpPrinter.get("MODEL"));
                HashMap hsmpParam = (hsmpPrinter == null) ? null : (HashMap) hsmpPrinter.get("PARAM");
                return preview(printModel, hsmpParam);
            }
            if ("cut".equals(cmd)) {
                return null;
            }
            if ("state".equals(cmd)) {
                if (realPaperState == HardwareAlarmState.NORMAL)
                    return "havePaper";
                if (realPaperState == HardwareAlarmState.ALARM)
                    return "noPaper";
                return "unknown";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            checkStateEnable = true;
        }
        return null;
    }

    protected String initPrintModel(String printModel) {
        return printModel;
    }

    private boolean sendData(byte[] data, int pos, int len) {
        try {
            if (printerSerialPort == null)
                return false;
            int page = 14 + 384 * 3;
            long lPrinterTime = 1000;
            if (!StringUtils.isBlank(SysConfig.get("PRINTER.SPEEDTIME"))) {
                lPrinterTime = Long.parseLong(SysConfig.get("PRINTER.SPEEDTIME"));
            }
            synchronized (this) {
                OutputStream os = printerSerialPort.getOutputStream();
                int count = 0;
                while (count < len) {
                    int n = len - count;
                    if (n > page)
                        n = page;
                    long delayMillisecond = lPrinterTime * n / (baud / 12);
                    long lStartTime = System.currentTimeMillis();
                    os.write(data, pos + count, n);
                    os.flush();
                    count += n;
                    long lEndTime = System.currentTimeMillis();
                    if ((lEndTime - lStartTime) < delayMillisecond) {
                        try {
                            Thread.sleep(delayMillisecond - (lEndTime - lStartTime));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendData(byte[] data) {
        return sendData(data, 0, data.length);
    }

    private void checkPaperState() {
        execPrinterCmdExecutor(printerCmdExecutorPaperState, null, null);
    }

    private PrinterCmdExecutor printerCmdExecutorPaperState = new PrinterCmdExecutor() {
        @Override
        public boolean execute(String str, HashMap<String, String> hsmpParam) {
            execPaperState();
            return true;
        }
    };

    private void clearPrinterBuffer() {
        printerFIFOQueue.reset();
        byte[] bufferState = new byte[3];
        bufferState[0] = (byte) 0x10;
        bufferState[1] = (byte) 0x04;
        bufferState[2] = (byte) 0x04;
        if (!sendData(bufferState))
            return;
        printerFIFOQueue.pop(2000);
    }

    boolean logRecvEnable = false;

    private void execPaperState() {
        if (!checkStateEnable)
            return;
        byte[] bufferState = new byte[3];
        bufferState[0] = (byte) 0x10;
        bufferState[1] = (byte) 0x04;
        bufferState[2] = (byte) 0x04;
        long lStartTime = System.currentTimeMillis();
        if (!sendData(bufferState))
            return;
        logRecvEnable = false;
        Object obj = printerFIFOQueue.pop(1000);
        long lEndTime = System.currentTimeMillis();
        if (obj == null) {
            logRecvEnable = true;
            errorTimes++;
            logger.debug("State CMD:" + EncryptUtils.byte2hex(bufferState) + ";OOT:" + (lEndTime - lStartTime) + "ms");
            monitorError();
            return;
        }
        printerFIFOQueue.reset();
        Byte bState = (Byte) obj;
        boolean hasPaper = ((bState & 0x60) != 0x60);
        if (hasPaper) {
            lHasNoPaper = 0;
            realPaperState = HardwareAlarmState.NORMAL;
            if (paperState != HardwareAlarmState.NORMAL) {
                paperState = HardwareAlarmState.NORMAL;
                HashMap<String, String> hsmpParam = new HashMap<String, String>();
                hsmpParam.put("type", "PRINTER_NO_PAPER_RECOVERY");
                hsmpParam.put("data", "" + id);
                raiseEvent(hsmpParam);
                logger.debug("Alarm recovery;CMD:" + EncryptUtils.byte2hex(bufferState) + ";RECV:" + EncryptUtils.byte2hex(new byte[]{bState}));
            }
        } else {
            realPaperState = HardwareAlarmState.ALARM;
            if (paperState != HardwareAlarmState.ALARM) {
                if (lHasNoPaper == 0)
                    lHasNoPaper = System.currentTimeMillis();
                long lTime = System.currentTimeMillis();
                if ((lHasNoPaperDelayCheck + lHasNoPaper) < lTime) {
                    paperState = HardwareAlarmState.ALARM;
                    HashMap<String, String> hsmpParam = new HashMap<String, String>();
                    hsmpParam.put("type", "PRINTER_NO_PAPER");
                    hsmpParam.put("data", "" + id);
                    raiseEvent(hsmpParam);
                    logger.debug("Alarm raise;CMD:" + EncryptUtils.byte2hex(bufferState) + ";RECV:" + EncryptUtils.byte2hex(new byte[]{bState}));
                }
            }
        }
    }

    private void monitorError() {
        if (errorTimes == 5) {
            if (printerState != HardwareAlarmState.ALARM) {
                printerState = HardwareAlarmState.ALARM;
                HashMap<String, String> hsmpParam = new HashMap<String, String>();
                hsmpParam.put("type", "PRINTER_ERROR");
                hsmpParam.put("data", "" + id);
                raiseEvent(hsmpParam);
            }
        }
        if (errorTimes == 0) {
            if (printerState != HardwareAlarmState.NORMAL) {
                printerState = HardwareAlarmState.NORMAL;
                HashMap<String, String> hsmpParam = new HashMap<String, String>();
                hsmpParam.put("type", "PRINTER_ERROR_RECOVERY");
                hsmpParam.put("data", "" + id);
                raiseEvent(hsmpParam);
            }
        }
    }

    private class RecvThread extends Thread {
        public void run() {
            InputStream is = printerSerialPort.getInputStream();
            byte[] buffer = new byte[1];

            try {
                while (is != null) {
                    int readlen = is.read(buffer);
                    if (readlen > 0) {
                        if (logRecvEnable) {
                            logger.debug("Printer RECV:" + EncryptUtils.byte2hex(buffer, 0, 1));
                        }
                        errorTimes = 0;
                        monitorError();
                        printerFIFOQueue.push(new Byte(buffer[0]));
                    } else {
                        is.close();
                        break;
                    }

                }
            } catch (Exception e) {
            } finally {
                printerSerialPort.close();
                printerSerialPort = null;
            }
        }
    }

    private class InitThread extends Thread {
        public void run() {
            checkPaperState();
        }
    }

    private static class PrinterCmdExecutorInfo {
        PrinterCmdExecutor printerCmdExecutor;
        String str;
        HashMap<String, String> hsmpParam;

        public PrinterCmdExecutorInfo(PrinterCmdExecutor printerCmdExecutor, String str, HashMap<String, String> hsmpParam) {
            this.printerCmdExecutor = printerCmdExecutor;
            this.str = str;
            this.hsmpParam = hsmpParam;
        }

        public void execute() {
            printerCmdExecutor.execute(str, hsmpParam);
        }
    }

    @Override
    protected boolean execPrinterCmdExecutor(PrinterCmdExecutor printerCmdExecutor, String str, HashMap<String, String> hsmpParam) {
        printFIFOQueue.push(new PrinterCmdExecutorInfo(printerCmdExecutor, str, hsmpParam));
        return true;
    }

    private FIFOQueue printFIFOQueue = new FIFOQueue();
    private Thread printThread = new Thread() {
        public void run() {
            PrinterCmdExecutorInfo printerCmdExecutorInfo = null;
            while ((printerCmdExecutorInfo = (PrinterCmdExecutorInfo) printFIFOQueue.pop()) != null) {
                printerCmdExecutorInfo.execute();
            }
        }
    };

    private byte XYSize = 0;
    private String align = null;
    private String charset = null;
    private String color = PrinterOptions.COLOR_BLACK;
    private final static int LINE_MAX_SIZE = 32;
    private final static String BLANK_LINE = "                                ";
    private final static int IMAGE_MAX_WIDTH = 380;

    @Override
    protected boolean reset() {
        byte[] bufferCmd = new byte[2];
        bufferCmd[0] = 0x1B;
        bufferCmd[1] = 0x40;
        sendData(bufferCmd);
        XYSize = 0;
        charset = null;
        align = null;
        color = null;
        return true;
    }

    @Override
    protected boolean setSettings(HashMap<String, String> hsmpSettings) {
        if (hsmpSettings == null)
            return true;
        String val = null;
        byte b = 0;
        val = hsmpSettings.get(PrinterOptions.SETTINGS_STRONG);
        if (val != null) {
            if ("Y".equalsIgnoreCase(val)) b = 1;
            if ("N".equalsIgnoreCase(val)) b = 0;
            byte[] bufferCmd = new byte[3];
            bufferCmd[0] = 0x1B;
            bufferCmd[1] = 0x45;
            bufferCmd[2] = b;
            sendData(bufferCmd);
        }
        val = hsmpSettings.get(PrinterOptions.SETTINGS_HIGHSIZE);
        if (val != null) {
            int size = Integer.parseInt(val);
            if (size < 1 || size > 8) {
                return true;
            }
            XYSize = (byte) ((XYSize & 0xF0) | ((size - 1) & 0x0F));
        }
        val = hsmpSettings.get(PrinterOptions.SETTINGS_WIDESIZE);
        if (val != null) {
            int size = Integer.parseInt(val);
            if (size < 1 || size > 8) {
                return true;
            }
            XYSize = (byte) ((XYSize & 0x0F) | (((size - 1) << 4) & 0xF0));
        }
        if (!StringUtils.isBlank(hsmpSettings.get(PrinterOptions.SETTINGS_HIGHSIZE))
                || !StringUtils.isBlank(hsmpSettings.get(PrinterOptions.SETTINGS_WIDESIZE))
                ) {
            byte[] bufferCmd = new byte[3];
            bufferCmd[0] = 0x1D;
            bufferCmd[1] = 0x21;
            bufferCmd[2] = XYSize;
            sendData(bufferCmd);
        }
        val = hsmpSettings.get(PrinterOptions.SETTINGS_UNDERLINE);
        if (val != null) {
            if ("Y".equalsIgnoreCase(val)) b = 1;
            if ("N".equalsIgnoreCase(val)) b = 0;
            byte[] bufferCmd = new byte[3];
            bufferCmd[0] = 0x1B;
            bufferCmd[1] = 0x2D;
            bufferCmd[2] = b;
            sendData(bufferCmd);
        }
        val = hsmpSettings.get(PrinterOptions.SETTINGS_ALIGN);
        if (val != null) {
            setAlign(val.trim());
        }
        val = hsmpSettings.get(PrinterOptions.SETTINGS_COLOR);
        if (val != null) {
            setColor(val.trim());
        }
        val = hsmpSettings.get(PrinterOptions.SETTINGS_CHARSET);
        if (val != null) {
            setCharset(val.trim());
        }
        return true;
    }

    protected boolean setAlign(String align) {
        this.align = align;
        byte b = -1;
        if (PrinterOptions.ALIGN_LEFT.equalsIgnoreCase(align)) {
            b = 0;
        }
        if (PrinterOptions.ALIGN_CENTER.equalsIgnoreCase(align)) {
            b = 1;
        }
        if (PrinterOptions.ALIGN_RIGHT.equalsIgnoreCase(align)) {
            b = 2;
        }
        if (b != -1) {
            byte[] bufferCmd = new byte[3];
            bufferCmd[0] = 0x1B;
            bufferCmd[1] = 0x61;
            bufferCmd[2] = b;
            sendData(bufferCmd);
        }
        return true;
    }

    protected boolean setColor(String color) {
        if (this.color != null) {
            if (this.color.equalsIgnoreCase(color))
                return true;
        }
        byte b = 0;
        if (!PrinterOptions.COLOR_BLACK.equalsIgnoreCase(color))
            b = 1;
        byte[] bufferCmd = new byte[3];
        bufferCmd[0] = 0x1D;
        bufferCmd[1] = 0x42;
        bufferCmd[2] = b;
        sendData(bufferCmd);
        this.color = color;
        return true;
    }

    protected boolean setCharset(String charset) {
        this.charset = charset;
        if ("GBK".equalsIgnoreCase(charset) || "GB2312".equalsIgnoreCase(charset)) {
            byte[] bufferCmd = new byte[2];
            bufferCmd[0] = 0x1C;
            bufferCmd[1] = 0x26;
            sendData(bufferCmd);
        } else {
            byte[] bufferCmd = new byte[2];
            bufferCmd[0] = 0x1C;
            bufferCmd[1] = 0x2E;
            sendData(bufferCmd);
        }
        return true;
    }

    @Override
    protected boolean printLine() {
        int oneCharSize = ((XYSize >> 4) & 0x0F) + 1;
        int count = LINE_MAX_SIZE / oneCharSize;
        String str = null;
        if (count <= 0) {
            str = "\n";
        } else {
            str = BLANK_LINE.substring(0, count) + "\n";
        }
        String newColor = null;
        String oldColor = this.color;
        if (PrinterOptions.COLOR_WHITE.equalsIgnoreCase(this.color)) {
            newColor = PrinterOptions.COLOR_BLACK;
        } else {
            newColor = PrinterOptions.COLOR_WHITE;
        }
        setColor(newColor);
        sendData(str.getBytes());
        setColor(oldColor);
        return true;
    }

    private final static String LINE = "\n";

    protected List<String> splitText(String text, int lineCharCount) {
        List<String> list = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();
        int strLen = text.length();
        int pos = 0;
        for (int i = 0; i < strLen; i++) {
            char cc = text.charAt(i);
            if (cc == '\n') {
                if (i == pos) {
                    list.add(LINE);
                } else if ((i - pos) == lineCharCount) {
                    list.add(sb.toString());
                    sb.setLength(0);
                } else {
                    list.add(sb.toString());
                    list.add(LINE);
                    sb.setLength(0);
                }
                pos = i + 1;
                continue;
            }
            sb.append(cc);
        }
        if (sb.length() > 0)
            list.add(sb.toString());
        return list;
    }

    @Override
    protected boolean printText(String text) {
        text = formatText(text);
        String charset = this.charset;
        if (StringUtils.isBlank(charset)) {
            charset = null;
        }
        for (int i = 0; i < 2; i++) {
            try {
                byte[] data = null;
                if (charset == null)
                    data = text.getBytes();
                else
                    data = text.getBytes(charset);
                sendData(data);
                break;
            } catch (Exception e) {
                charset = null;
            }
        }
        return true;
    }

    @Override
    protected boolean printQRCode(String mode, String qrCode) {
        reset();
        byte v = 0, r = 0;
        byte t = 0;
        int n = qrCode.length();
        boolean isQRCode = false;
        if ("PDF417".equalsIgnoreCase(mode)) {
            v = (byte) (new Double(Math.sqrt(n)).intValue() + 4);
            r = 4;
            t = 0;
            isQRCode = true;
        } else if ("DATAMATRIX".equalsIgnoreCase(mode)) {
            v = 0;
            r = 0;
            t = 1;
            isQRCode = true;
        } else if ("QRCODE".equalsIgnoreCase(mode)) {
            v = 0;
            r = 4;
            t = 2;
            isQRCode = true;
        }
        if (isQRCode) {
            byte[] data = qrCode.getBytes();
            byte[] buff = new byte[3];
            buff[0] = 0x1D;
            buff[1] = 0x5A;
            buff[2] = t;
            sendData(buff);

            buff = new byte[7 + data.length];
            buff[0] = 0x1B;
            buff[1] = 0x5A;
            buff[2] = v;
            buff[3] = r;
            buff[4] = 6;
            buff[5] = (byte) (n & 0xFF);
            buff[6] = (byte) ((n >> 8) & 0xFF);
            System.arraycopy(data, 0, buff, 7, data.length);
            sendData(buff);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    protected boolean printBarcode(String mode, String barcode) {
        reset();
        int m = 0;
        byte[] data = null;
        if ("UPC-A".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 11 || barcode.length() > 12)
                return true;
            m = 65;
            data = barcode.getBytes();
        } else if ("UPC-E".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 11 || barcode.length() > 12)
                return true;
            m = 66;
            data = barcode.getBytes();
        } else if ("EAN13".equalsIgnoreCase(mode) || "JAN13".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 12 || barcode.length() > 13)
                return true;
            m = 67;
            data = barcode.getBytes();
        } else if ("EAN8".equalsIgnoreCase(mode) || "JAN8".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 7 || barcode.length() > 8)
                return true;
            m = 68;
            data = barcode.getBytes();
        } else if ("CODE39".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 1 || barcode.length() > 253)
                return true;
            String newBarcode = "*" + barcode + "*";
            m = 69;
            data = newBarcode.getBytes();
        } else if ("ITF".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 1 || barcode.length() > 255)
                return true;
            m = 70;
            data = barcode.getBytes();
        } else if ("CODABAR".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 1 || barcode.length() > 255)
                return true;
            m = 71;
            data = barcode.getBytes();
        } else if ("CODE93".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            if (barcode.length() < 1 || barcode.length() > 255)
                return true;
            m = 72;
            data = barcode.getBytes();
        } else if ("CODE128".equalsIgnoreCase(mode)) {
            barcode = barcode.trim();
            String newBarcode = StringUtils.replace(barcode, "{", "{{");
            if (newBarcode.length() < 2 || newBarcode.length() > 253)
                return true;
            newBarcode = "{B" + newBarcode;
            m = 73;
            data = newBarcode.getBytes();
        }
        if (data != null) {
            byte[] buff = new byte[3];
            buff[0] = 0x1D;
            buff[1] = 0x68;
            buff[2] = 54;
            sendData(buff);

            buff = new byte[4 + data.length];
            buff[0] = 0x1D;
            buff[1] = 0x6B;
            buff[2] = (byte) m;
            buff[3] = (byte) data.length;
            System.arraycopy(data, 0, buff, 4, data.length);
            sendData(buff);
        }
        return true;
    }

    @Override
    protected boolean printData(byte[] data, int offset, int count) {
        if (count > 2) {
            if (data[offset] == 0x1B && data[offset + 1] == 0x40) {
                clearPrinterBuffer();
            }
        }
        int pageSize = 1154;
        int n = (count + pageSize - 1) / pageSize;
        for (int i = 0; i < n; i++) {
            int finalCount = count - i * pageSize;
            if (finalCount > pageSize) {
                finalCount = pageSize;
            }
            sendData(data, offset + i * pageSize, finalCount);
        }
        return true;
    }

    @Override
    protected boolean printImage(String filename, String resource, String width, String height) {
        InputStream is = null;
        try {
            int fw = IMAGE_MAX_WIDTH;
            int fh = 0;
            int pfw = IMAGE_MAX_WIDTH;
            int pfh = 0;

            if (!StringUtils.isBlank(width)) {
                pfw = Integer.parseInt(width);
            }
            if (!StringUtils.isBlank(height)) {
                pfh = Integer.parseInt(height);
            }
            if (pfw > IMAGE_MAX_WIDTH) {
                if (pfh > 0) {
                    fh = fw * pfh / pfw;
                }
            } else {
                fw = pfw;
                if (pfh > 0)
                    fh = pfh;
            }
            Drawable drawable = null;
            if (!StringUtils.isBlank(filename)) {
                File file = new File(filename);
                if (file.isFile() && file.length() > 0) {
                    drawable = Drawable.createFromPath(filename);
                }
            }
            if (drawable == null && !StringUtils.isBlank(resource)) {
                is = IOUtils.getResourceAsInputStream(resource);
                drawable = Drawable.createFromStream(is, null);
            }
            if (drawable != null) {
                int dw = drawable.getIntrinsicWidth();
                int dh = drawable.getIntrinsicHeight();
                if (fh == 0) {
                    fh = fw * dh / dw;
                }

                Bitmap bitmap = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_4444);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                drawable.draw(canvas);

                List<byte[]> listData = bitmap2printerdata(bitmap);
                /*
                int h = bitmap.getHeight();
				int w = bitmap.getWidth();
				for(int c=0;c<w;c++) {
					for(int r=0;r<h;r++) {
						int color = bitmap.getPixel(c, r) & 0xFFFFFFFF;
						if((color & 0xFF0000) > 0x880000
						&& (color & 0x00FF00) > 0x008800
						&& (color & 0x0000FF) > 0x000088
						) {
							bitmap.setPixel(c, r, 0xFFFFFFFF);
						} else {
							bitmap.setPixel(c, r, 0xFF000000);
						}
						//System.out.println(c+";" + r + ";" + Integer.toHexString(color));
					}
				}
				saveBitmap("/sdcard/mybmp.bmp",bitmap);
				*/
                bitmap.recycle();
                printImage(listData);
            }
        } catch (Exception e) {
        } finally {
            IOUtils.close(is);
        }
        return true;
    }

    protected boolean printImage(List<byte[]> listData) {
        clearPrinterBuffer();
        int lineLen = listData.get(0).length;
        for (int i = 0; i < listData.size(); i += 3) {
            int m = 1;
            if ((i + 2) < listData.size()) {
                m = 0x21;
                byte[] buff = new byte[lineLen * 3];
                byte[] line0 = listData.get(i + 0);
                byte[] line1 = listData.get(i + 1);
                byte[] line2 = listData.get(i + 2);
                for (int c = 0; c < lineLen; c++) {
                    buff[c * 3 + 0] = line0[c];
                    buff[c * 3 + 1] = line1[c];
                    buff[c * 3 + 2] = line2[c];
                }
                printImage(buff, m, lineLen, 3);
            } else {
                m = 0x01;
                for (int r = i; r < listData.size(); r++) {
                    printImage(listData.get(r), m, lineLen, 1);
                }
            }
        }
        clearPrinterBuffer();
        return true;
    }

    private final static byte[] IMAGE_HEAD = {
            0x1B, 0x40, 0x1B, 0x63, 0x00, 0x1B, 0x31, 0x00, 0x1B, 0x2A, 0x21, 0x00, 0x00
    };

    private boolean printImage(byte[] data, int m, int rlen, int r) {
        while (rlen > 1) {
            boolean isPrinted = false;
            for (int i = 0; i < r; i++) {
                if (data[(rlen - 1) * r + i] != 0) {
                    isPrinted = true;
                    break;
                }
            }
            if (isPrinted)
                break;
            rlen--;
        }
        int nl = rlen & 0x0FF;
        int nh = (rlen >> 8) & 0x0FF;
        byte[] buff = new byte[rlen * r + IMAGE_HEAD.length + 1];
        System.arraycopy(IMAGE_HEAD, 0, buff, 0, IMAGE_HEAD.length);
        System.arraycopy(data, 0, buff, IMAGE_HEAD.length, rlen * r);
        buff[IMAGE_HEAD.length - 3] = (byte) m;
        buff[IMAGE_HEAD.length - 2] = (byte) nl;
        buff[IMAGE_HEAD.length - 1] = (byte) nh;
        buff[buff.length - 1] = 0x0D;
        sendData(buff);
        XYSize = 0;
        charset = null;
        align = null;
        color = null;
        return true;
    }

    public static List<byte[]> bitmap2printerdata(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int rr = 0;
        List<byte[]> listRow = new ArrayList<byte[]>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (rr < h) {
            for (int x = 0; x < w; x++) {
                byte b = 0;
                for (int y = 0; y < 8; y++) {
                    int val = 0;
                    if ((rr + y) < h) {
                        int color = bitmap.getPixel(x, rr + y) & 0xFFFFFFFF;
                        if ((color & 0xFF0000) > 0x800000
                                && (color & 0x00FF00) > 0x008000
                                && (color & 0x0000FF) > 0x000080
                                ) {
                            val = 0;
                        } else {
                            val = 1;
                        }
                    }
                    b <<= 1;
                    b |= val;
                }
                baos.write(b);
            }
            byte[] data = baos.toByteArray();
            listRow.add(data);
            baos.reset();
            rr += 8;
        }
        return listRow;
    }

    public static void saveBitmap(String filename, Bitmap bmp) {
        File f = new File(filename);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected boolean cutPaper(String mode) {
        byte b = 0;
        if (PrinterOptions.CUT_HALF.equalsIgnoreCase(mode)) b = 1;

        byte[] bufferCut = new byte[3];
        bufferCut[0] = (byte) 0x1D;
        bufferCut[1] = (byte) 0x56;
        bufferCut[2] = b;
        sendData(bufferCut);
        long lCutWaitTime = 0;
        try {
            if (!StringUtils.isBlank(SysConfig.get("PRINTER.CUT.WAITTIME"))) {
                lCutWaitTime = Integer.parseInt(SysConfig.get("PRINTER.CUT.WAITTIME"));
                if (lCutWaitTime > 0) {
                    Thread.sleep(lCutWaitTime);
                }
            }
        } catch (Exception e) {
        }
        return true;
    }

    @Override
    protected boolean printTextImage(String str) {
        int idx = -1;
        int start = 0;
        String filename = null;
        String resource = null;
        String X = null;
        String Y = null;
        String WIDTH = null;
        String HEIGHT = null;
        String FONTSIZE = null;
        String color = PrinterOptions.COLOR_BLACK;
        String align = PrinterOptions.ALIGN_LEFT;
        int fontSize = 24;
        while ((idx = str.indexOf(";", start)) != -1) {
            String s = str.substring(start, idx);
            boolean isKey = false;
            if (s.startsWith("FILE=")) {
                filename = s.substring("FILE=".length());
                isKey = true;
            }
            if (s.startsWith("RESOURCE=")) {
                resource = s.substring("RESOURCE=".length());
                isKey = true;
            }
            if (s.startsWith("X=")) {
                X = s.substring("X=".length());
                isKey = true;
            }
            if (s.startsWith("Y=")) {
                Y = s.substring("Y=".length());
                isKey = true;
            }
            if (s.startsWith("FONTSIZE=")) {
                FONTSIZE = s.substring("FONTSIZE=".length());
                isKey = true;
            }
            if (s.startsWith("COLOR=")) {
                color = s.substring("COLOR=".length());
                isKey = true;
            }
            if (s.startsWith("ALIGN=")) {
                align = s.substring("ALIGN=".length());
                isKey = true;
            }
            if (s.startsWith("WIDTH=")) {
                WIDTH = s.substring("WIDTH=".length());
                isKey = true;
            }
            if (s.startsWith("HEIGHT=")) {
                HEIGHT = s.substring("HEIGHT=".length());
                isKey = true;
            }
            if (isKey) {
                start = idx + 1;
            } else {
                break;
            }
        }
        if (start > 0) {
            str = str.substring(start);
        }
        if (!StringUtils.isBlank(FONTSIZE)) {
            fontSize = Integer.parseInt(FONTSIZE);
        }
        int fw = IMAGE_MAX_WIDTH;
        int fh = fontSize + 8;
        int pfw = 0;
        int pfh = 0;

        if (!StringUtils.isBlank(WIDTH)) {
            pfw = Integer.parseInt(WIDTH);
        }
        if (!StringUtils.isBlank(HEIGHT)) {
            pfh = Integer.parseInt(HEIGHT);
        }
        Bitmap bitmap = null;
        if (!StringUtils.isBlank(filename) || !StringUtils.isBlank(resource)) {
            Drawable drawable = null;
            InputStream is = null;
            try {
                if (!StringUtils.isBlank(filename)) {
                    File file = new File(filename);
                    if (file.isFile() && file.length() > 0) {
                        drawable = Drawable.createFromPath(filename);
                    }
                }
                if (drawable == null && !StringUtils.isBlank(resource)) {
                    is = IOUtils.getResourceAsInputStream(resource);
                    drawable = Drawable.createFromStream(is, null);
                }
                if (drawable != null) {
                    fw = drawable.getIntrinsicWidth();
                    fh = drawable.getIntrinsicHeight();
                    if (pfw > 0) {
                        if (pfh == 0) {
                            pfh = fh * pfw / fw;
                        }
                        fw = pfw;
                        fh = pfh;
                    }

                    bitmap = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_4444);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawable.draw(canvas);
                }
            } finally {
                IOUtils.close(is);
            }
        }
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_4444);
            int initColor = Color.WHITE;
            if (PrinterOptions.COLOR_WHITE.equalsIgnoreCase(color)) {
                initColor = Color.BLACK;
            }
            for (int c = 0; c < fw; c++) {
                for (int r = 0; r < fh; r++) {
                    bitmap.setPixel(c, r, initColor);
                }
            }
        }
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        if (PrinterOptions.COLOR_WHITE.equalsIgnoreCase(color)) {
            paint.setColor(Color.WHITE);
        } else {
            paint.setColor(Color.BLACK);
        }
        int x = 0;
        int y = fontSize;
        if (!StringUtils.isBlank(X)) {
            x = Integer.parseInt(X);
        } else {
            int textLen = str.getBytes().length;
            if (PrinterOptions.ALIGN_RIGHT.equalsIgnoreCase(align)) {
                x = fw - fontSize * textLen / 2;
            }
            if (PrinterOptions.ALIGN_CENTER.equalsIgnoreCase(align)) {
                x = (fw - fontSize * textLen / 2) / 2;
            }
        }
        if (!StringUtils.isBlank(Y)) {
            y = Integer.parseInt(Y);
        }
        canvas.drawText(str, x, y, paint);

        if (fw > IMAGE_MAX_WIDTH) {
            int dw = bitmap.getWidth();
            int dh = bitmap.getHeight();
            if (fh == 0) {
                fh = fw * dh / dw;
            }

            BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
            Bitmap tBitmap = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_4444);
            Canvas tCanvas = new Canvas(tBitmap);
            bitmapDrawable.setBounds(0, 0, tBitmap.getWidth(), tBitmap.getHeight());
            bitmapDrawable.draw(tCanvas);
            bitmap.recycle();
            bitmap = tBitmap;
        }
        List<byte[]> listData = bitmap2printerdata(bitmap);
        bitmap.recycle();
        return printImage(listData);
    }

    @Override
    protected String execPrintCode(HashMap<String, String> hsmpValue) {
        return null;
    }

    private String formatText(String text) {
        int oneCharSize = ((XYSize >> 4) & 0x0F) + 1;
        int lineCharCount = LINE_MAX_SIZE / oneCharSize;
        text = StringUtils.replace(text, "\r", "");
        if (PrinterOptions.ALIGN_RIGHT.equalsIgnoreCase(align)) {
            StringBuffer sb = new StringBuffer();
            List<String> listLine = splitText(text, lineCharCount);
            for (int i = 0; i < listLine.size(); i++) {
                String line = listLine.get(i);
                if (line.equals(LINE)) {
                    sb.append(LINE);
                } else {
                    int len = line.getBytes().length;
                    if (len >= lineCharCount) {
                        sb.append(line);
                    } else {
                        sb.append((BLANK_LINE.substring(0, lineCharCount - len)) + line);
                    }
                }
            }
            text = sb.toString();
        } else if (PrinterOptions.ALIGN_CENTER.equalsIgnoreCase(align)) {
            StringBuffer sb = new StringBuffer();
            List<String> listLine = splitText(text, lineCharCount);
            for (int i = 0; i < listLine.size(); i++) {
                String line = listLine.get(i);
                if (line.equals(LINE)) {
                    sb.append(LINE);
                } else {
                    int len = line.getBytes().length;
                    if (len >= lineCharCount || (len + 2) > lineCharCount) {
                        sb.append(line);
                    } else {
                        sb.append((BLANK_LINE.substring(0, (lineCharCount - len) / 2)) + line + (BLANK_LINE.substring(0, (lineCharCount - len) / 2)));
                    }
                }
            }
            text = sb.toString();
        }
        return text;
    }
}
