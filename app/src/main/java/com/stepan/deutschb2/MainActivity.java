package com.stepan.deutschb2;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    final int GREEN=Color.rgb(32,90,75), CREAM=Color.rgb(247,244,238), INK=Color.rgb(23,53,46);
    LinearLayout root, content; TextToSpeech tts; ArrayList<Word> words=new ArrayList<>(); JSONArray lessons; SharedPreferences prefs; Uri lastPhoto;
    String[] tabs={"Сегодня","Словарь","Темы","Практика","Вокруг меня"};

    static class Word { String level,article,word,plural,ru,de,exru; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("p",0); tts=new TextToSpeech(this,this); loadData(); showShell(0); }
    void loadData(){ try{
        String j=readAsset("lessons.json"); lessons=new JSONArray(j);
        BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("words.csv"))); String s; br.readLine();
        while((s=br.readLine())!=null){ String[] p=s.split(";",-1); if(p.length<7)continue; Word w=new Word(); w.level=p[0];w.article=p[1];w.word=p[2];w.plural=p[3];w.ru=p[4];w.de=p[5];w.exru=p[6];words.add(w); }
    }catch(Exception e){ toast(e.toString()); } }
    String readAsset(String n)throws Exception{ InputStream is=getAssets().open(n); ByteArrayOutputStream o=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int r; while((r=is.read(buf))>0)o.write(buf,0,r); return o.toString("UTF-8"); }
    TextView tv(String s,int sp,boolean bold){ TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(INK);v.setPadding(dp(16),dp(10),dp(16),dp(10)); if(bold)v.setTypeface(null,1);return v; }
    Button btn(String s){ Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b; }
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
    void showShell(int tab){ root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(CREAM);
        TextView head=tv("Deutsch bis B2",25,true);head.setTextColor(Color.WHITE);head.setBackgroundColor(GREEN);root.addView(head,new LinearLayout.LayoutParams(-1,dp(70)));
        ScrollView sv=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(10),dp(8),dp(10),dp(100));sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); for(int i=0;i<tabs.length;i++){final int ix=i;Button b=btn(tabs[i]);b.setTextSize(11);b.setOnClickListener(v->showShell(ix));nav.addView(b,new LinearLayout.LayoutParams(0,dp(64),1));} root.addView(nav);setContentView(root);
        if(tab==0)today(); else if(tab==1)dictionary(); else if(tab==2)topics(); else if(tab==3)practice(); else camera(); }
    void cardTitle(String title,String sub){ TextView t=tv(title,26,true);content.addView(t); if(sub!=null)content.addView(tv(sub,15,false)); }
    void today(){ cardTitle("Твой маршрут","Учись понемногу каждый день. Данные и прогресс хранятся локально.");
        int known=prefs.getInt("known",0), streak=prefs.getInt("streak",1); content.addView(tv("Слов в словаре: "+words.size()+"   •   Освоено: "+known+"   •   Серия: "+streak+" дн.",18,true));
        String[] x={"20 слов - вспомнить и назвать","1 урок грамматики - прочитать и выполнить практику","10 повторений - интервальная система","5 минут произношения - слушать и повторять","Мини-дневник - 3 предложения о дне"}; for(String s:x){TextView v=tv("• "+s,17,false);content.addView(v);} Button b=btn("Начать практику");b.setOnClickListener(v->showShell(3));content.addView(b); }
    void dictionary(){ cardTitle("Слова, которые нужны тебе","Существительные всегда показаны вместе с артиклем и множественным числом."); EditText q=new EditText(this);q.setHint("Поиск по немецкому или русскому");content.addView(q); LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list); Runnable render=()->{list.removeAllViews();String z=q.getText().toString().toLowerCase(); for(Word w:words){String full=(w.article+" "+w.word+" - "+w.plural+"\n"+w.ru+"\n"+w.de+"\n"+w.exru); if(!z.isEmpty()&&!full.toLowerCase().contains(z))continue; LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.HORIZONTAL); TextView t=tv(full,16,false);c.addView(t,new LinearLayout.LayoutParams(0,-2,1));Button sp=btn("🔊");sp.setOnClickListener(v->speak(w.article+" "+w.word+". "+w.de));c.addView(sp);list.addView(c);} };q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){render.run();}public void afterTextChanged(android.text.Editable e){}});render.run(); }
    void topics(){ cardTitle("Грамматика A1-B2","Сначала смысл и структура предложения, затем правило, примеры с переводом и практика."); try{ for(int i=0;i<lessons.length();i++){JSONObject o=lessons.getJSONObject(i);Button b=btn(o.getString("level")+" • "+o.getString("title"));final int ix=i;b.setOnClickListener(v->lesson(ix));content.addView(b);} }catch(Exception e){toast(e.toString());} }
    void lesson(int ix){ content.removeAllViews(); try{JSONObject o=lessons.getJSONObject(ix);cardTitle(o.getString("level")+" • "+o.getString("title"),null);content.addView(tv(o.getString("theory"),17,false));JSONArray a=o.getJSONArray("examples");for(int i=0;i<a.length();i++){String[]p=a.getString(i).split("\\|");content.addView(tv(p[0]+"\n"+(p.length>1?p[1]:""),17,false));}Button s=btn("🔊 Озвучить примеры");s.setOnClickListener(v->{try{StringBuilder q=new StringBuilder();for(int i=0;i<a.length();i++)q.append(a.getString(i).split("\\|")[0]).append(". ");speak(q.toString());}catch(Exception ignored){}});content.addView(s);Button p=btn("Практика по теме");p.setOnClickListener(v->practice());content.addView(p);}catch(Exception e){toast(e.toString());} }
    void practice(){ content.removeAllViews();cardTitle("Практика","Отвечай из памяти. После ответа приложение покажет правильный вариант с переводом."); if(words.isEmpty())return; Random r=new Random(); Word w=words.get(r.nextInt(words.size())); TextView q=tv("Как будет по-немецки: «"+w.ru+"»? Напиши слово вместе с артиклем.",20,true);content.addView(q);EditText a=new EditText(this);a.setHint("например: die Wohnung");content.addView(a);TextView res=tv("",16,false);content.addView(res);Button check=btn("Проверить");check.setOnClickListener(v->{String exp=(w.article+" "+w.word).toLowerCase();String got=a.getText().toString().trim().toLowerCase();boolean ok=got.equals(exp);res.setText((ok?"✓ Верно":"✗ Правильно: "+w.article+" "+w.word)+"\nМн. число: "+w.plural+"\n"+w.de+"\n"+w.exru);if(ok)prefs.edit().putInt("known",prefs.getInt("known",0)+1).apply();});content.addView(check);Button next=btn("Следующее");next.setOnClickListener(v->practice());content.addView(next); }
    void camera(){ cardTitle("Вокруг меня","Сделай фото. Распознавание текста и общих предметов выполняется на устройстве ML Kit. Для неизвестного предмета можно добавить слово вручную в следующей версии.");Button cam=btn("📷 Сделать фото");cam.setOnClickListener(v->{if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},9);return;}Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,77);});content.addView(cam);content.addView(tv("Подсказка: наведи камеру на вывеску, меню, документ или предмет. Текст распознается лучше, чем редкие предметы.",15,false)); }
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==77&&c==RESULT_OK&&data!=null&&data.getExtras()!=null){Bitmap bm=(Bitmap)data.getExtras().get("data");analyze(bm);} }
    void analyze(Bitmap bm){ content.removeAllViews();cardTitle("Результат камеры",null);ImageView im=new ImageView(this);im.setImageBitmap(bm);im.setAdjustViewBounds(true);content.addView(im);TextView out=tv("Распознаю...",17,false);content.addView(out);InputImage img=InputImage.fromBitmap(bm,0);TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img).addOnSuccessListener(t->{String s=t.getText(); if(!s.trim().isEmpty()) out.setText("Текст:\n"+s+"\n\nНажми на Словарь и найди нужное слово."); else label(img,out);}).addOnFailureListener(e->label(img,out)); }
    void label(InputImage img,TextView out){ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS).process(img).addOnSuccessListener(ls->{StringBuilder s=new StringBuilder("Предположения:\n");for(int i=0;i<Math.min(5,ls.size());i++)s.append(ls.get(i).getText()).append(" - ").append(Math.round(ls.get(i).getConfidence()*100)).append("%\n");s.append("\nДля перевода открой Словарь. Камерный перевод работает офлайн для слов, которые есть в локальной базе.");out.setText(s.toString());}).addOnFailureListener(e->out.setText("Не удалось распознать объект."));}
    void speak(String s){ if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"de"); }
    public void onInit(int status){ if(status==TextToSpeech.SUCCESS)tts.setLanguage(Locale.GERMANY); }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}
}
