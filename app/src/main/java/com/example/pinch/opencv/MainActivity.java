package com.example.pinch.opencv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {


    List<String> images = new ArrayList<String>();
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                //Когда загрузились- запускаем очередь обработки изображений
                case LoaderCallbackInterface.SUCCESS:
                    proceedImageQueue();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Добавляем фотографии
        images.add("https://scontent-frt3-1.xx.fbcdn.net/hphotos-xft1/t31.0-8/c0.220.851.315/p851x315/11110168_1083572655003439_4345780475539415398_o.jpg");
        images.add("http://pics.utro.ru/utro_photos/2016/04/19/1279090.jpg");
        images.add("https://mensby.com/images/stories/articles/2015/6433/how-communicate-people-m.jpg");
        images.add("http://www.womenpretty.ru/images/stories/obshenie15-1.jpg");
        images.add("http://kakimenno.ru/uploads/posts/2013-01/1359228908_nauchitsya-obshchatsya-s-ludmi-1.jpg");
        images.add("http://cdn15.img22.ria.ru/images/100053/93/1000539361.jpg");
        images.add("http://img-fotki.yandex.ru/get/6300/166659.184/0_7381b_f76133a7_XL.jpg");
        images.add("http://rf.biz/wp-content/uploads/2015/02/28-620x264.jpg");
        images.add("http://twokidsandamap.com/wp-content/uploads/2013/04/loveland5.jpg");
        images.add("http://cs624620.vk.me/v624620989/3050d/Pt8ACU-bn8A.jpg");
        images.add("http://cs622525.vk.me/v622525170/1f4f8/Xnwlcepn2lU.jpg");
        images.add("http://cs619416.vk.me/v619416170/206ed/9W5WTDoAK8g.jpg");
    }

    @Override
    public void onResume() {
        super.onResume();
        //Вызываем асинхронный загрузчик библиотеки
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
    }

    public void proceedImageQueue() {
        //Пока очередь обработки не дошла до конца- обрабатываем новые изображения
        if (images.size() > 0) {
            new DownloadImageTask().execute(images.get(0));
            images.remove(0);
        }
    }

    public Bitmap peopleDetect(String path) {
        Bitmap bitmap = null;
        float execTime;
        try {
            // Закачиваем фотографию
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(input, null, opts);
            long time = System.currentTimeMillis();
            // Создаем матрицу изображения для OpenCV и помещаем в нее нашу фотографию
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            // Переконвертируем матрицу с RGB на градацию серого
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY, 4);
            HOGDescriptor hog = new HOGDescriptor();
            //Получаем стандартный определитель людей и устанавливаем его нашему дескриптору
            MatOfFloat descriptors = HOGDescriptor.getDefaultPeopleDetector();
            hog.setSVMDetector(descriptors);
            // Определяем переменные, в которые будут помещены результаты поиска ( locations - прямоуголные области, weights - вес (можно сказать релевантность) соответствующей локации)
            MatOfRect locations = new MatOfRect();
            MatOfDouble weights = new MatOfDouble();
            // Собственно говоря, сам анализ фотографий. Результаты запишутся в locations и weights
            hog.detectMultiScale(mat, locations, weights);
            execTime = ((float) (System.currentTimeMillis() - time)) / 1000f;
            //Переменные для выделения областей на фотографии
            Point rectPoint1 = new Point();
            Point rectPoint2 = new Point();
            Scalar fontColor = new Scalar(0, 0, 0);
            Point fontPoint = new Point();
            // Если есть результат - добавляем на фотографию области и вес каждой из них
            if (locations.rows() > 0) {
                List<Rect> rectangles = locations.toList();
                int i = 0;
                List<Double> weightList = weights.toList();
                for (Rect rect : rectangles) {
                    float weigh = weightList.get(i++).floatValue();

                    rectPoint1.x = rect.x;
                    rectPoint1.y = rect.y;
                    fontPoint.x = rect.x;
                    fontPoint.y = rect.y - 4;
                    rectPoint2.x = rect.x + rect.width;
                    rectPoint2.y = rect.y + rect.height;
                    final Scalar rectColor = new Scalar(0, 0, 0);
                    // Добавляем на изображения найденную информацию
                    Core.rectangle(mat, rectPoint1, rectPoint2, rectColor, 2);
                    Core.putText(mat,
                            String.format("%1.2f", weigh),
                            fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
                            2, Core.LINE_AA, false);

                }
            }
            fontPoint.x = 15;
            fontPoint.y = bitmap.getHeight() - 20;
            // Добавляем дополнительную отладочную информацию
            Core.putText(mat,
                    "Processing time:" + execTime + " width:" + bitmap.getWidth() + " height:" + bitmap.getHeight(),
                    fontPoint, Core.FONT_HERSHEY_PLAIN, 1.5, fontColor,
                    2, Core.LINE_AA, false);
            Utils.matToBitmap(mat, bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            return peopleDetect(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                LinearLayout layout = (LinearLayout) findViewById(R.id.linear);
                ImageView image = new ImageView(getApplicationContext());
                image.setImageBitmap(bitmap);
                layout.addView(image);
            }
            proceedImageQueue();
        }
    }
}