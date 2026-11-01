package com.stepan.deutschb2;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.camera.core.*;
import androidx.camera.view.PreviewView;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.io.*;
import java.net.*;
import org.json.*;

public class CameraActivity extends Activity implements TextToSpeech.OnInitListener {
  private LinearLayout screen,overlays;
  private PreviewView preview;
  private TextView label,status;
  private TextToSpeech tts;
  private volatile boolean busy=false;
  private long last=0;
  private String spoken="";
  private final java.util.concurrent.ExecutorService work=java.util.concurrent.Executors.newSingleThreadExecutor();
  private android.content.SharedPreferences settings;
  public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff132d28);getWindow().setNavigationBarColor(0xff132d28);settings=getSharedPreferences("camera_options",MODE_PRIVATE);tts=new TextToSpeech(this,this);render();}
  private boolean connected(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);return caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);}
  private GradientDrawable bg(int col,int rad){GradientDrawable d=new GradientDrawable();d.setColor(col);d.setCornerRadius(rad*getResources().getDisplayMetrics().density);return d;}
  private TextView field(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(-1);v.setPadding(18,18,18,18);return v;}
  private TextView button(String value,Runnable r){TextView v=field(value,17);v.setGravity(Gravity.CENTER);v.setBackground(bg(0xff276b54,14));v.setOnClickListener(w->r.run());return v;}
  private void render(){
    screen=new LinearLayout(this);screen.setOrientation(1);screen.setBackgroundColor(0xff132d28);screen.setFitsSystemWindows(true);setContentView(screen);
    LinearLayout top=new LinearLayout(this);top.setOrientation(1);top.setPadding(18,10,18,10);
    TextView title=field("‹  Слова вокруг меня",22);title.setOnClickListener(v->finish());top.addView(title);
    status=field(connected()?"● Онлайн • облачная модель":"● Офлайн • камера недоступна",13);top.addView(status);screen.addView(top);
    if(!connected()){screen.addView(field("Подключись к интернету, чтобы использовать камеру. Слова и уроки доступны офлайн.",17));return;}
    String key=settings.getString("google_api_key","");
    if(key.isEmpty()){
       screen.addView(field("Подключи Gemini API-ключ из Google AI Studio. Кадры камеры будут отправляться Google для распознавания; возможны расходы и ограничения API.",16));
       EditText input=new EditText(this);input.setTextColor(-1);input.setHintTextColor(0xffb9cec5);input.setHint("Gemini API Key");input.setSingleLine(true);screen.addView(input);
       screen.addView(button("СОХРАНИТЬ КЛЮЧ И ВКЛЮЧИТЬ КАМЕРУ",()->{
          settings.edit().putString("google_api_key",input.getText().toString().trim()).apply();render();
       }));return;
    }
    preview=new PreviewView(this);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
    screen.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
    overlays=new LinearLayout(this);overlays.setOrientation(1);overlays.setPadding(16,16,16,16);overlays.setBackground(bg(0xff213f35,18));
    label=field("Наведи камеру на предмет. Результат появится на немецком, с транскрипцией и переводом.",19);
    overlays.addView(label);overlays.addView(button("🔊 СЛУШАТЬ",()->{if(tts!=null&&!spoken.isEmpty())tts.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,"camera");}));
    overlays.addView(button("ЗАМЕНИТЬ API-КЛЮЧ",()->{settings.edit().remove("google_api_key").apply();render();}));
    screen.addView(overlays);
    if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
        requestPermissions(new String[]{Manifest.permission.CAMERA},44);
    }else begin();
  }
  public void onRequestPermissionsResult(int code,String[] p,int[] g){super.onRequestPermissionsResult(code,p,g);if(code==44&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)begin();}
  @androidx.camera.core.ExperimentalGetImage
  private void begin(){
    ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
    future.addListener(()->{
      try{
        ProcessCameraProvider cp=future.get();Preview pv=new Preview.Builder().build();pv.setSurfaceProvider(preview.getSurfaceProvider());
        ImageAnalysis ia=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        ia.setAnalyzer(work,image->{
          long now=System.currentTimeMillis();if(busy||now-last<4500){image.close();return;}last=now;
          busy=true;Bitmap bitmap=frameBitmap(image);image.close();
          if(bitmap==null){busy=false;return;}analyze(bitmap);
        });
        cp.unbindAll();cp.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pv,ia);
      }catch(Exception e){runOnUiThread(()->label.setText("Ошибка камеры: "+e.getMessage()));}
    },ContextCompat.getMainExecutor(this));
  }
  @androidx.camera.core.ExperimentalGetImage
  private Bitmap frameBitmap(ImageProxy proxy){
    try{
      android.media.Image image=proxy.getImage();if(image==null)return null;
      android.media.Image.Plane[] planes=image.getPlanes();int w=proxy.getWidth(),h=proxy.getHeight();
      byte[] nv=new byte[w*h*3/2];java.nio.ByteBuffer y=planes[0].getBuffer(),u=planes[1].getBuffer(),v=planes[2].getBuffer();
      for(int row=0;row<h;row++)for(int col=0;col<w;col++)nv[row*w+col]=y.get(row*planes[0].getRowStride()+col*planes[0].getPixelStride());
      int off=w*h;for(int row=0;row<h/2;row++)for(int col=0;col<w/2;col++){nv[off++]=v.get(row*planes[2].getRowStride()+col*planes[2].getPixelStride());nv[off++]=u.get(row*planes[1].getRowStride()+col*planes[1].getPixelStride());}
      android.graphics.YuvImage yi=new android.graphics.YuvImage(nv,android.graphics.ImageFormat.NV21,w,h,null);
      ByteArrayOutputStream stream=new ByteArrayOutputStream();yi.compressToJpeg(new Rect(0,0,w,h),70,stream);
      Bitmap b=BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size());Matrix rotate=new Matrix();rotate.postRotate(proxy.getImageInfo().getRotationDegrees());
      Bitmap adjusted=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),rotate,true);
      int max=Math.max(adjusted.getWidth(),adjusted.getHeight());float scale=Math.min(1f,640f/max);
      return Bitmap.createScaledBitmap(adjusted,Math.max(1,(int)(adjusted.getWidth()*scale)),Math.max(1,(int)(adjusted.getHeight()*scale)),true);
    }catch(Exception e){return null;}
  }
  private void analyze(Bitmap bitmap){
    work.execute(()->{HttpURLConnection conn=null;try{
      ByteArrayOutputStream jpeg=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,75,jpeg);
      String encoded=android.util.Base64.encodeToString(jpeg.toByteArray(),android.util.Base64.NO_WRAP);
      JSONObject req=new JSONObject().put("contents",new JSONArray().put(
        new JSONObject().put("parts",new JSONArray()
         .put(new JSONObject().put("text","Identify the most prominent real physical object accurately. Return 3 lines only: German noun with correct article; IPA German pronunciation; Russian translation. For keyboard, mouse, headphones give specific terms. If uncertain say unbekannt, don't invent."))
         .put(new JSONObject().put("inline_data",new JSONObject().put("mime_type","image/jpeg").put("data",encoded))))));
      URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
      conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("x-goog-api-key",settings.getString("google_api_key",""));
      conn.setConnectTimeout(10000);conn.setReadTimeout(15000);conn.setDoOutput(true);
      byte[] json=req.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);conn.getOutputStream().write(json);
      int code=conn.getResponseCode();if(code!=200)throw new IOException("HTTP "+code+" • Проверь API-ключ и лимиты.");
      ByteArrayOutputStream res=new ByteArrayOutputStream();InputStream in=conn.getInputStream();byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1)res.write(buf,0,n);
      String response=new JSONObject(res.toString("UTF-8")).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
      String[] fields=response.trim().split("\n");spoken=fields[0].trim();
      runOnUiThread(()->label.setText(response.trim()));
    }catch(Exception ex){runOnUiThread(()->label.setText("Распознавание временно недоступно: "+ex.getMessage()));}
     finally{if(conn!=null)conn.disconnect();busy=false;}
    });
  }
  public void onInit(int status){if(status==TextToSpeech.SUCCESS)tts.setLanguage(Locale.GERMANY);}
  protected void onPause(){if(tts!=null)tts.stop();super.onPause();}
  protected void onDestroy(){if(tts!=null)tts.shutdown();work.shutdown();super.onDestroy();}
}