package org.opencv.samples.fd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class FdView extends SampleCvViewBase {
    private static final String TAG = "Sample::FdView";
    private Mat                 mRgba;
    private Mat                 mGray;

    private CascadeClassifier   mCascade;

    public FdView(Context context) {
        super(context);

        try {
            //Obtenemos la base de conocimientos
        	InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
        	//InputStream is1 = context.getResources().openRawResource(R.raw.hc_eye);
            //Creamos un directorio con acceso privado llamado cascade
        	File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        	//File cascadeDir1 = context.getDir("eye",Context.MODE_PRIVATE);
            //Creamos un archivo llamado lbpcascade_forntalface.xml en el directorio anterior
        	File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            //File cascadeFile1 = new File(cascadeDir1,"hc_eye.xml");
            //Se crea un stream para escribir en el fichero anteriormente creado
        	FileOutputStream os = new FileOutputStream(cascadeFile);
        	//FileOutputStream os1 = new FileOutputStream(cascadeFile1);
        	//Crea un buffer de 4BMB
        	byte[] buffer = new byte[4096];
        	//byte[] buffer1 = new byte[4096];
            int bytesRead;
            //int bytesRead1;
            //Llena el buffer hasta que el archivo termine y almacenalo en el nuevo archivo creado
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            
            //while((bytesRead1 = is1.read(buffer)) != -1){
            //	os1.write(buffer1,0,bytesRead1);
            //}
            //is1.close();
            //os1.close();
            //Crea un clasificador con el archivo recien creado, si la carga es satisfactoria, elimina el directorio y su contenido.
            mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            //mCascade1 = new CascadeClassifier(cascadeFile1.getAbsolutePath());
            if (mCascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mCascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

            cascadeFile.delete();
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @Override //Si el celular se mueve de posicion crea de nuevo las matrices
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        super.surfaceChanged(_holder, format, width, height);

        synchronized (this) {
            // inicializador de matrices para su manipulacion
            mGray = new Mat();
            mRgba = new Mat();
        }
    }

    @Override //regresa un mapa de bits despues de procesar una captura de video *frame*
    protected Bitmap processFrame(VideoCapture capture) {
    	//Toma las imagenes de la captura en forma de RGBA o GRAY llenando las matrices
        capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

        //Si existe clasificador determina el alto con el numero de columnas de la matriz gray, despues el size del rostro usando la altura por el valor minimo
       //Despues obtiene objetos de diferentes escalas y los toma en la lista faces para guardar los objetos que se encuentran en dicho frame
        if (mCascade != null) {
            int height = mGray.rows();
            int faceSize = Math.round(height * FdActivity.minFaceSize);
            List<Rect> faces = new LinkedList<Rect>();
            mCascade.detectMultiScale(mGray, faces, 1.1, 2, 2 // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    , new Size(faceSize, faceSize));
            //recorre los rectangulos(objetos) y dibujalos proporcionandoles color
            for (Rect r : faces)
                Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 3);
        }
        //Crea un mapa de bits aparitr de la imagen resultante
        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);

        if (Utils.matToBitmap(mRgba, bmp))
            return bmp;

        bmp.recycle();
        return null;
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
                mRgba.release();
            if (mGray != null)
                mGray.release();

            mRgba = null;
            mGray = null;
        }
    }
}
