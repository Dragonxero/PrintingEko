package com.example.matthew.testingprint;

import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import comm.CommEntity;
import comm.entity.BasePrinter;
import common.utils.IOUtils;

public class MainActivity extends AppCompatActivity {

    public final static int QRcodeWidth = 500;
    private static final String IMAGE_DIRECTORY = "/QRcodeDemonuts";
    Bitmap bitmap;
    EditText data;
    ImageView qr;


    private static String RecycleContainerModel = "[FORMAT]" +
            "[RESET]" +
            "[SETTINGS]HIGHSIZE=1;WIDESIZE=1;STRONG=Y;ALIGN=CENTER;COLOR=BLACK" +
            "[TEXT]CONTENEDOR DE RECICLAJE" +
            "[LINE]" +
            "[RESET]" +
            "[SETTINGS]HIGHSIZE=1;WIDESIZE=1;STRONG=N;ALIGN=CENTER;COLOR=BLACK" +
            "[TEXT]NRO:$ID$" +
            "[LINE]" +
            "[QRCODE]DATAMATRIX:$DATA$;" +
            "[RESET]" +
            "[SETTINGS]STRONG=Y;ALIGN=CENTER;" +
            "[TEXT]Adjunte este voucher al contenedor retirado." +
            "[LINE]"+
            "[LINE]"+
            "[LINE]"+
            "[LINE]"+
            "[LINE]"+
            "[LINE]"+
            "[LINE]"+
            "[LINE]"+
            "[CUT]ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        data = (EditText) findViewById(R.id.textdata);
        qr = (ImageView) findViewById(R.id.qrfield);
        CommEntity.init();
    }

    public void createQR(View view) {
        EditText data = (EditText) findViewById(R.id.textdata);
        if (data.getText().toString().trim().length() == 0) {
            Toast.makeText(MainActivity.this, "Enter String!", Toast.LENGTH_SHORT).show();
        } else {
            try {
                bitmap = TextToImageEncode(data.getText().toString());
                qr.setImageBitmap(bitmap);
                String path = saveImage(bitmap);
                Toast.makeText(MainActivity.this, "QR creado en  => " + path, Toast.LENGTH_SHORT).show();
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }
    }

    public void printQR(View view) {

        String textModel = "[FORMAT]\n"+
                "[RESET]\n"+
                "[SETTINGS]HIGHSIZE=1;WIDESIZE=1;STRONG=Y;UNDERLINE=N;ALIGN=CENTER;COLOR;"+
                "[LINE]"+
                "[LINE]"+
                "[TEXT]Voucher"+
                "[LINE]"+
                "[QRCODE]DATAMATRIX:www.google.cl"+
                "[SETTINGS]STRONG=Y;UNDERLINE=N;ALIGN=CENTER;COLOR=BLACK;"+
                "[LINE]"+
                "[TEXT]ViveEKO" +
                "[LINE]"+
                "[TEXT]Gracias por su Preferencia"+
                "[CUT]HALF";

        printData("www.google.com");
    }
    public static void printData(String data) {
        if (StringUtils.isNotBlank(data)) {
            HashMap<String, String> hsmpParam = new HashMap<String, String>();
            BasePrinter basePrinter = CommEntity.getPrinter(1);
            String model = RecycleContainerModel
                    .replace("$DATA$", data)
                    .replace("$ID$", "1");
            basePrinter.print(model, hsmpParam);
        }
    }


    //Metodos para crear el QR y luego mostarlo en la pantalla.
    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs());
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();   //give read write permission
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    public BitMatrix bitMatrix;

    private Bitmap TextToImageEncode(String Value) throws WriterException {
        //BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    QRcodeWidth, QRcodeWidth, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.colorBlack) : getResources().getColor(R.color.colorWhite);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }
}