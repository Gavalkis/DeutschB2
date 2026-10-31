package com.stepan.deutschb2;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;

import androidx.activity.ComponentActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import org.json.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends ComponentActivity implements TextToSpeech.OnInitListener {
    final int GREEN=Color.rgb(32,90,75), DARK=Color.rgb(20,61,51), CREAM=Color.rgb(248,245,238),
            INK=Color.rgb(23,53,46), MUTED=Color.rgb(112,120,114), PALE=Color.rgb(232,239,232),
            GOLD=Color.rgb(196,164,91), RED=Color.rgb(183,73,73);
    LinearLayout root, content;
    TextToSpeech tts;
    boolean ttsReady=false;
    String speakingKey="";
    ArrayList<Word> words=new ArrayList<>();
    JSONArray lessons, guides;
    SharedPreferences prefs;
    int currentTab=0;
    int speechLesson=-1, speechExample=-1;
    ExecutorService cameraExecutor=Executors.newSingleThreadExecutor();
    long lastAnalysis=0;

    final long[] SRS = {
            10*60_000L, 30*60_000L, 60*60_000L, 3*60*60_000L, 6*60*60_000L,
            24*60*60_000L, 3*24*60*60_000L, 10*24*60*60_000L, 20*24*60*60_000L, 30*24*60*60_000L
    };

    static class Word { String level,article,word,plural,ru,de,exru; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("progress_v2",0);
        tts=new TextToSpeech(this,this);
        loadData();
        showShell(0);
    }

    void loadData(){
        try{
            lessons=new JSONArray(readAsset("lessons.json")); guides=new JSONArray(readAsset("lesson_guides.json"));
            BufferedReader br=new BufferedReader(new InputStreamReader(getAssets().open("words.csv")));
            String s; br.readLine();
            while((s=br.readLine())!=null){
                String[] p=s.split(";",-1);
                if(p.length<7)continue;
                Word w=new Word(); w.level=p[0];w.article=p[1];w.word=p[2];w.plural=p[3];
                w.ru=p[4];w.de=p[5];w.exru=p[6];words.add(w);
            }
        }catch(Exception e){ toast("Ошибка данных: "+e.getMessage()); }
    }

    String readAsset(String n)throws Exception{
        InputStream is=getAssets().open(n); ByteArrayOutputStream o=new ByteArrayOutputStream();
        byte[] buf=new byte[4096]; int r; while((r=is.read(buf))>0)o.write(buf,0,r); return o.toString("UTF-8");
    }

    int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density); }

    GradientDrawable bg(int color,int radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color);g.setCornerRadius(dp(radius)); return g;
    }

    TextView tv(String s,int sp,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(INK);
        v.setPadding(dp(16),dp(10),dp(16),dp(10));
        v.setTypeface(null,bold?Typeface.BOLD:Typeface.NORMAL);
        return v;
    }

    TextView serif(String s,int sp,boolean bold){
        TextView v=tv(s,sp,bold);
        v.setTypeface(Typeface.create("serif",bold?Typeface.BOLD:Typeface.NORMAL));
        return v;
    }

    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(15);
        b.setTextColor(Color.WHITE); b.setBackground(bg(GREEN,18)); b.setPadding(dp(14),dp(10),dp(14),dp(10));
        return b;
    }

    Button ghost(String s){
        Button b=btn(s); b.setTextColor(INK); b.setBackground(bg(PALE,18)); return b;
    }

    LinearLayout card(){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(12),dp(12),dp(12),dp(12)); c.setBackground(bg(Color.WHITE,22));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(dp(8),dp(8),dp(8),dp(8)); c.setLayoutParams(lp);
        return c;
    }

    void stopAudio(){
        if(tts!=null) tts.stop();
        speakingKey="";
    }

    void showShell(int tab){
        stopAudio();
        currentTab=tab;
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(CREAM);

        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(18),dp(12),dp(18),dp(12));top.setBackgroundColor(GREEN);
        TextView brand=serif("Mein Deutsch",27,true);brand.setTextColor(Color.WHITE);brand.setPadding(0,0,0,2);top.addView(brand);
        TextView sub=tv("A1 → B2 • каждый день",12,false);sub.setTextColor(Color.rgb(215,232,225));sub.setPadding(0,0,0,0);top.addView(sub);
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(78)));

        ScrollView sv=new ScrollView(this); content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(10),dp(12),dp(10),dp(100));sv.addView(content);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(4),dp(4),dp(4),dp(4));nav.setBackgroundColor(Color.WHITE);
        String[] tabs={"Дом","Слова","Уроки","Задания","Камера"};
        for(int i=0;i<tabs.length;i++){
            final int ix=i;Button b=ghost(tabs[i]);b.setTextSize(10);b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if(i==tab){ b.setTextColor(Color.WHITE); b.setBackground(bg(GREEN,16)); }
            b.setOnClickListener(v->showShell(ix));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1);lp.setMargins(dp(2),0,dp(2),0);nav.addView(b,lp);
        }
        root.addView(nav);setContentView(root);

        if(tab==0)today(); else if(tab==1)dictionary(); else if(tab==2)route(); else if(tab==3)reviewHub(); else cameraGate();
    }

    void headline(String eyebrow,String title,String sub){
        TextView e=tv(eyebrow.toUpperCase(Locale.ROOT),12,true);e.setLetterSpacing(.18f);e.setTextColor(GREEN);content.addView(e);
        content.addView(serif(title,31,true));
        if(sub!=null){TextView s=tv(sub,16,false);s.setTextColor(MUTED);content.addView(s);}
    }

    boolean isOnline(){
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        Network n=cm.getActiveNetwork(); if(n==null)return false;
        NetworkCapabilities c=cm.getNetworkCapabilities(n); return c!=null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    void onlineBadge(){
        TextView b=tv(isOnline()?"● Онлайн • все функции доступны":"● Офлайн • сетевые функции отключены",13,true);
        b.setTextColor(isOnline()?GREEN:MUTED); b.setBackground(bg(isOnline()?PALE:Color.rgb(235,235,235),18));
        content.addView(b);
    }

    int dueWords(){
        int count=0; long now=System.currentTimeMillis();
        for(int i=0;i<words.size();i++){
            long due=prefs.getLong("word_due_"+i,0);
            if(due>0&&due<=now)count++;
        } return count;
    }

    void today(){
        headline("Ежедневная практика","Маленький шаг. Большой словарный запас.","Маршрут подстраивается под повторения и пройденные темы.");
        onlineBadge();

        LinearLayout stat=card();stat.setBackground(bg(DARK,24));
        TextView a=serif(String.valueOf(words.size()),30,true);a.setTextColor(Color.WHITE);stat.addView(a);
        TextView b=tv("слов в базе • "+dueWords()+" повторений сейчас",14,false);b.setTextColor(Color.rgb(220,235,228));stat.addView(b);
        content.addView(stat);

        LinearLayout c=card();
        c.addView(tv("Твой маршрут",22,true));
        int unlocked=prefs.getInt("unlocked_lesson",0);
        int total=lessons==null?0:lessons.length();
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(total,1));pb.setProgress(Math.min(unlocked,total));c.addView(pb);
        c.addView(tv("Открыто уроков: "+Math.min(unlocked+1,total)+" из "+total,14,false));
        Button go=btn("Продолжить маршрут");go.setOnClickListener(v->route());c.addView(go);
        content.addView(c);

        LinearLayout r=card();r.addView(tv("Сегодня",22,true));
        r.addView(tv("• 1 новый урок\n• "+Math.max(dueWords(),5)+" интервальных повторений\n• 5 минут произношения\n• 3 предложения в мини-дневнике",16,false));
        Button rev=ghost("Повторить то, что пора");rev.setOnClickListener(v->reviewHub());r.addView(rev);content.addView(r);
    }

    void dictionary(){
        headline("Личный словарь","Слова, которые нужны тебе","Существительные всегда идут с родом и множественным числом.");
        EditText q=new EditText(this);q.setHint("например: Wohnung, квартира");q.setBackground(bg(Color.WHITE,18));q.setPadding(dp(16),dp(14),dp(16),dp(14));content.addView(q);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Runnable render=()->{
            list.removeAllViews();String z=q.getText().toString().toLowerCase();
            for(int i=0;i<words.size();i++){
                final int ix=i;Word w=words.get(i);String full=w.article+" "+w.word+"  •  "+w.plural+"  "+w.ru;
                if(!z.isEmpty()&&!full.toLowerCase().contains(z))continue;
                LinearLayout c=card();c.addView(serif(w.article+" "+w.word,23,true));
                TextView meta=tv(w.plural+"  •  "+w.level+"\n"+w.ru,14,false);meta.setTextColor(MUTED);c.addView(meta);
                c.addView(tv(w.de+"\n"+w.exru,16,false));
                LinearLayout row=new LinearLayout(this);
                Button sp=ghost("🔊 Слушать");sp.setOnClickListener(v->toggleSpeak("word_"+ix,w.article+" "+w.word+". "+w.de,sp));
                Button add=ghost("↻ В повторение");add.setOnClickListener(v->{scheduleWord(ix,true);toast("Добавлено в интервальные повторения");});
                row.addView(sp,new LinearLayout.LayoutParams(0,-2,1));row.addView(add,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);list.addView(c);
            }
        };
        q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){render.run();} public void afterTextChanged(android.text.Editable e){}});
        render.run();
    }

    void route(){
        content.removeAllViews();
        headline("Путь A1 → B2","Твой маршрут","Каждый урок: теория → примеры → практика → интервальное повторение. После уровня - контрольный экзамен.");
        int unlocked=prefs.getInt("unlocked_lesson",0);
        try{
            String current="";
            for(int i=0;i<lessons.length();i++){
                JSONObject o=lessons.getJSONObject(i);String level=o.getString("level");
                if(!level.equals(current)){
                    if(!current.isEmpty()) addCheckpoint(current,i-1,unlocked);
                    current=level; TextView h=serif(level,27,true);h.setTextColor(GREEN);content.addView(h);
                }
                final int ix=i; boolean open=i<=unlocked; boolean done=prefs.getBoolean("lesson_done_"+i,false);
                LinearLayout c=card(); c.setAlpha(open?1f:.45f);
                c.addView(tv((done?"✓ ":"○ ")+(i+1)+". "+o.getString("title"),18,true));
                c.addView(tv(open?(done?"Пройдено • можно повторить":"Открыто • теория и практика"):"Сначала пройди предыдущий урок",13,false));
                if(open){Button b=done?ghost("Повторить урок"):btn("Начать урок");b.setOnClickListener(v->lesson(ix));c.addView(b);}
                content.addView(c);
            }
            if(lessons.length()>0)addCheckpoint("B2",lessons.length()-1,unlocked);
        }catch(Exception e){toast(e.getMessage());}
    }

    void addCheckpoint(String level,int lastIndex,int unlocked){
        LinearLayout c=card();c.setBackground(bg(Color.rgb(241,234,216),22));
        c.addView(tv("🏁 Контрольный "+level,19,true));
        c.addView(tv("Смешанные задания по темам уровня + старые слова из предыдущих уроков.",14,false));
        boolean open=unlocked>lastIndex;
        Button b=open?btn("Начать мини-экзамен"):ghost("Закрыто");
        b.setEnabled(open); if(!open)b.setAlpha(.45f); else b.setOnClickListener(v->startExam(level));
        c.addView(b);content.addView(c);
    }

    void lesson(int ix){
        stopAudio(); showShell(2); content.removeAllViews();
        try{
            JSONObject o=lessons.getJSONObject(ix);
            headline(o.getString("level")+" • урок "+(ix+1),o.getString("title"),null);

            LinearLayout theory=card();theory.addView(tv("Разбор правила",18,true));
            JSONObject guide=guides.getJSONObject(ix);
            theory.addView(tv(guide.getString("why"),16,false));
            TextView formula=tv(guide.getString("formula"),15,true);formula.setBackground(bg(PALE,14));theory.addView(formula);
            JSONArray table=guide.getJSONArray("table");
            for(int rr=0;rr<table.length();rr++){
                JSONArray cells=table.getJSONArray(rr);LinearLayout line=new LinearLayout(this);line.setBackground(bg(rr==0?PALE:rr%2==0?CREAM:Color.WHITE,6));
                for(int cc=0;cc<cells.length();cc++){TextView x=tv(cells.getString(cc),rr==0?12:13,rr==0);x.setPadding(dp(5),dp(8),dp(5),dp(8));line.addView(x,new LinearLayout.LayoutParams(0,-2,1));}theory.addView(line);
            }
            theory.addView(tv("Запомни: "+guide.getString("tip"),14,true));theory.addView(tv(o.getString("theory"),15,false));content.addView(theory);

            JSONArray a=o.getJSONArray("examples");
            for(int i=0;i<a.length();i++){
                final int ex=i;String[] p=a.getString(i).split("\\|",2);
                LinearLayout c=card();c.addView(serif(p[0],20,true));if(p.length>1)c.addView(tv(p[1],15,false));
                LinearLayout row=new LinearLayout(this);
                Button sp=ghost("🔊 Слушать");sp.setOnClickListener(v->toggleSpeak("lesson_"+ix+"_"+ex,p[0],sp));
                Button mic=ghost("🎙 Повторить");mic.setOnClickListener(v->startPronunciation(ix,ex,p[0]));
                row.addView(sp,new LinearLayout.LayoutParams(0,-2,1));row.addView(mic,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);content.addView(c);
            }

            LinearLayout hint=card();hint.setBackground(bg(PALE,22));hint.addView(tv("Как закрепляем",17,true));
            hint.addView(tv("Пройденные слова и правила будут регулярно возвращаться в следующих уроках.",14,false));content.addView(hint);

            Button p=btn("Пройти практику по этой теме");p.setOnClickListener(v->topicPractice(ix,0,0));content.addView(p);
        }catch(Exception e){toast(e.getMessage());}
    }

    void topicPractice(int lessonIx,int step,int score){
        stopAudio(); content.removeAllViews();
        try{
            JSONObject o=lessons.getJSONObject(lessonIx);JSONArray ex=o.getJSONArray("examples");
            int total=7;
            if(step>=total){finishLessonPractice(lessonIx,score,total);return;}
            headline("Практика "+(step+1)+"/"+total,o.getString("title"),"Задания создаются только по текущей теме и её примерам.");

            int mode=step%6; String[] ep=ex.getString(step%ex.length()).split("\\|",2);
            if(mode==0) multipleChoice(lessonIx,step,score,total,ep);
            else if(mode==1) translateTask(lessonIx,step,score,total,ep);
            else if(mode==2) listeningTask(lessonIx,step,score,total,ep);
            else if(mode==3) clozeTask(lessonIx,step,score,total,ep);
            else if(mode==4) reorderTask(lessonIx,step,score,total,ep);
            else structureTask(lessonIx,step,score,total,ep);
        }catch(Exception e){toast(e.getMessage());}
    }

    void multipleChoice(int li,int step,int score,int total,String[] ep){
        LinearLayout c=card();c.addView(tv("Выбери правильный перевод",17,true));c.addView(serif(ep[0],22,true));content.addView(c);
        ArrayList<String> opts=new ArrayList<>();opts.add(ep.length>1?ep[1]:"");
        for(Word w:words)if(opts.size()<3&&!opts.contains(w.exru))opts.add(w.exru);
        Collections.shuffle(opts);
        for(String x:opts){Button b=ghost(x);b.setOnClickListener(v->topicPractice(li,step+1,score+(x.equals(ep[1])?1:0)));content.addView(b);}
    }

    void translateTask(int li,int step,int score,int total,String[] ep){
        LinearLayout c=card();c.addView(tv("Напиши по-немецки",17,true));c.addView(serif(ep.length>1?ep[1]:"",22,true));content.addView(c);
        EditText in=new EditText(this);in.setHint("Твой ответ");in.setBackground(bg(Color.WHITE,18));in.setPadding(dp(16),dp(14),dp(16),dp(14));content.addView(in);
        TextView result=tv("",15,false);content.addView(result);
        Button check=btn("Проверить");check.setOnClickListener(v->{String a=normalize(in.getText().toString()),e=normalize(ep[0]);boolean ok=a.equals(e);result.setText(ok?"✓ Верно":"Правильно:\n"+ep[0]+"\n"+(ep.length>1?ep[1]:""));check.setText("Дальше");check.setOnClickListener(z->topicPractice(li,step+1,score+(ok?1:0)));});content.addView(check);
    }

    void listeningTask(int li,int step,int score,int total,String[] ep){
        LinearLayout c=card();c.addView(tv("Прослушай фразу и напиши, что услышал",17,true));
        Button sp=btn("🔊 Воспроизвести");sp.setOnClickListener(v->toggleSpeak("practice_"+li+"_"+step,ep[0],sp));c.addView(sp);content.addView(c);
        EditText in=new EditText(this);in.setHint("Немецкая фраза");in.setBackground(bg(Color.WHITE,18));content.addView(in);
        TextView result=tv("",15,false);content.addView(result);
        Button check=btn("Проверить");check.setOnClickListener(v->{boolean ok=similar(normalize(in.getText().toString()),normalize(ep[0]))>.82;result.setText(ok?"✓ Хорошо":"Фраза была:\n"+ep[0]+"\n"+(ep.length>1?ep[1]:""));check.setText("Дальше");check.setOnClickListener(z->topicPractice(li,step+1,score+(ok?1:0)));});content.addView(check);
    }

    void clozeTask(int li,int step,int score,int total,String[] ep){
        String original=ep[0];String[] chunks=original.replaceAll("[.!?]","").split(" ");
        int target=Math.min(chunks.length-1,Math.max(0,chunks.length/2));String answer=chunks[target];chunks[target]="_____";
        headline("Вставь слово","Заполни пропуск",ep.length>1?ep[1]:"");
        LinearLayout c=card();c.addView(serif(String.join(" ",chunks),23,true));content.addView(c);
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Какое слово пропущено?");content.addView(input);
        TextView result=tv("",16,false);content.addView(result);
        Button check=btn("Проверить");check.setOnClickListener(v->{
            boolean ok=normalize(input.getText().toString()).equals(normalize(answer));
            result.setText((ok?"✓ Верно!":"Правильно: "+answer)+"\\n"+original+"\\n"+(ep.length>1?ep[1]:""));
            check.setText("Дальше");check.setOnClickListener(x->topicPractice(li,step+1,score+(ok?1:0)));
        });content.addView(check);
    }

    void reorderTask(int li,int step,int score,int total,String[] ep){
        headline("Собери предложение","Расставь слова",ep.length>1?ep[1]:"");
        ArrayList<String> parts=new ArrayList<>(Arrays.asList(ep[0].replaceAll("[.!?]","").split(" ")));
        Collections.shuffle(parts);
        TextView built=serif("...",22,true);content.addView(built);
        ArrayList<String> chosen=new ArrayList<>();LinearLayout opts=card();opts.addView(tv("Нажимай слова по порядку:",15,true));
        for(String word:parts){Button b=ghost(word);b.setOnClickListener(v->{chosen.add(word);b.setEnabled(false);b.setAlpha(.4f);built.setText(String.join(" ",chosen));});opts.addView(b);}content.addView(opts);
        TextView result=tv("",15,false);content.addView(result);
        Button check=btn("Проверить");check.setOnClickListener(v->{
            boolean ok=normalize(String.join(" ",chosen)).equals(normalize(ep[0]));
            result.setText(ok?"✓ Верно!":"Правильно: "+ep[0]);check.setText("Дальше");check.setOnClickListener(x->topicPractice(li,step+1,score+(ok?1:0)));
        });content.addView(check);
    }

    void structureTask(int li,int step,int score,int total,String[] ep){
        LinearLayout c=card();c.addView(tv("Логика предложения",17,true));c.addView(serif(ep[0],21,true));c.addView(tv("Объясни себе: где подлежащее, где спрягаемый глагол, дополнение/обстоятельство и почему глагол занимает именно эту позицию.",15,false));content.addView(c);
        Button known=btn("Я могу объяснить структуру");known.setOnClickListener(v->topicPractice(li,step+1,score+1));content.addView(known);
        Button repeat=ghost("Нужно повторить правило");repeat.setOnClickListener(v->lesson(li));content.addView(repeat);
    }

    void finishLessonPractice(int li,int score,int total){
        content.removeAllViews();headline("Урок завершён","Результат: "+score+" / "+total, score*100/total>=70?"Тема закреплена. Она вернётся по интервальному расписанию.":"Лучше повторить теорию и пройти практику ещё раз.");
        int pct=score*100/total;
        if(pct>=70){
            prefs.edit().putBoolean("lesson_done_"+li,true).putInt("unlocked_lesson",Math.max(prefs.getInt("unlocked_lesson",0),li+1)).apply();
            scheduleLesson(li,true);
            Button b=btn("Продолжить маршрут");b.setOnClickListener(v->showShell(2));content.addView(b);
        }else{
            Button b=btn("Повторить урок");b.setOnClickListener(v->lesson(li));content.addView(b);
        }
    }

    void reviewHub(){
        content.removeAllViews();headline("Интервальное повторение","То, что пора вернуть в память","Старые слова и правила специально появляются внутри новых заданий.");
        int due=dueWords();content.addView(tv("Сейчас пора повторить: "+due+" слов.",17,true));
        Button w=btn("Повторять слова");w.setEnabled(!words.isEmpty());w.setOnClickListener(v->reviewWord(nextDueWord()));content.addView(w);
        Button l=ghost("Повторять правила");l.setOnClickListener(v->reviewLesson(nextDueLesson()));content.addView(l);
    }

    int nextDueWord(){
        long now=System.currentTimeMillis();for(int i=0;i<words.size();i++)if(prefs.getLong("word_due_"+i,0)<=now)return i;return new Random().nextInt(words.size());
    }

    int nextDueLesson(){
        long now=System.currentTimeMillis();try{for(int i=0;i<lessons.length();i++)if(prefs.getBoolean("lesson_done_"+i,false)&&prefs.getLong("lesson_due_"+i,0)<=now)return i;}catch(Exception ignored){}
        return Math.max(0,prefs.getInt("unlocked_lesson",1)-1);
    }

    void reviewWord(int ix){
        content.removeAllViews();Word w=words.get(ix);headline("Повторение",w.ru,"Вспомни немецкое слово вместе с артиклем.");
        EditText in=new EditText(this);in.setHint("артикль + слово");in.setBackground(bg(Color.WHITE,18));content.addView(in);
        TextView res=tv("",16,false);content.addView(res);
        Button check=btn("Проверить");check.setOnClickListener(v->{boolean ok=normalize(in.getText().toString()).equals(normalize(w.article+" "+w.word));res.setText(ok?"✓ Верно":"Правильно: "+w.article+" "+w.word+"\n"+w.plural);advanceWord(ix,ok);check.setText("Следующее");check.setOnClickListener(z->reviewWord(nextDueWord()));});content.addView(check);
    }

    void reviewLesson(int ix){ lesson(ix); }

    void scheduleWord(int ix,boolean reset){
        int st=reset?0:prefs.getInt("word_stage_"+ix,0);prefs.edit().putInt("word_stage_"+ix,st).putLong("word_due_"+ix,System.currentTimeMillis()+SRS[Math.min(st,SRS.length-1)]).apply();
    }
    void advanceWord(int ix,boolean ok){
        int st=prefs.getInt("word_stage_"+ix,0);st=ok?Math.min(st+1,SRS.length-1):0;
        prefs.edit().putInt("word_stage_"+ix,st).putLong("word_due_"+ix,System.currentTimeMillis()+SRS[st]).apply();
    }
    void scheduleLesson(int ix,boolean reset){
        int st=reset?0:prefs.getInt("lesson_stage_"+ix,0);prefs.edit().putInt("lesson_stage_"+ix,st).putLong("lesson_due_"+ix,System.currentTimeMillis()+SRS[Math.min(st,SRS.length-1)]).apply();
    }

    void startExam(String level){
        stopAudio();content.removeAllViews();headline("Контрольный "+level,"Мини-экзамен","Смешанная проверка слов, понимания структуры и тем уровня.");
        int score=0,total=5;Random r=new Random();
        for(int i=0;i<total;i++){
            Word w=words.get(r.nextInt(words.size()));
            LinearLayout c=card();c.addView(tv((i+1)+". "+w.ru,16,true));c.addView(tv("Вспомни: "+w.article+" "+w.word+" • пример: "+w.de,14,false));content.addView(c);
        }
        Button done=btn("Я повторил задания");done.setOnClickListener(v->{toast("Контрольный блок завершён. Следующие уроки доступны по маршруту.");showShell(2);});content.addView(done);
    }

    void toggleSpeak(String key,String text,Button b){
        if(key.equals(speakingKey)){
            stopAudio();b.setText("🔊 Слушать");return;
        }
        stopAudio();speakingKey=key;b.setText("⏹ Стоп");
        if(ttsReady)tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,key);
        new Handler(Looper.getMainLooper()).postDelayed(()->{if(key.equals(speakingKey)){speakingKey="";b.setText("🔊 Слушать");}},Math.max(1800,text.length()*95L));
    }

    void startPronunciation(int li,int ex,String expected){
        stopAudio();speechLesson=li;speechExample=ex;
        prefs.edit().putString("speech_expected",expected).apply();
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"de-DE");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Повтори немецкую фразу");
        try{startActivityForResult(i,501);}catch(Exception e){toast("На устройстве нет службы распознавания речи");}
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==501&&res==RESULT_OK&&data!=null){
            ArrayList<String> arr=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(arr!=null&&!arr.isEmpty()){
                String got=arr.get(0),exp=prefs.getString("speech_expected","");
                int pct=(int)(similar(normalize(got),normalize(exp))*100);
                new AlertDialog.Builder(this).setTitle("Произношение: "+pct+"%")
                        .setMessage("Распознано:\n"+got+"\n\nОжидалось:\n"+exp+"\n\nПовтори ещё раз, если ниже 80%.")
                        .setPositiveButton("OK",null).show();
            }
        }
    }

    double similar(String a,String b){
        if(a.isEmpty()||b.isEmpty())return 0;int[][] d=new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++)d[i][0]=i;for(int j=0;j<=b.length();j++)d[0][j]=j;
        for(int i=1;i<=a.length();i++)for(int j=1;j<=b.length();j++)d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
        return 1.0-(double)d[a.length()][b.length()]/Math.max(a.length(),b.length());
    }

    String normalize(String s){return s.toLowerCase(Locale.GERMAN).replaceAll("[.,!?;:]","").replaceAll("\\s+"," ").trim();}

    void cameraGate(){
        content.removeAllViews();headline("Слова вокруг тебя","Наведи и назови","Распознавание предметов работает в реальном времени и доступно только при подключении к интернету.");
        onlineBadge();
        if(!isOnline()){
            LinearLayout c=card();c.setAlpha(.55f);c.addView(tv("Камера недоступна офлайн",20,true));c.addView(tv("Подключись к интернету, чтобы включить распознавание предметов в реальном времени.",15,false));content.addView(c);return;
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            Button ask=btn("Разрешить камеру");ask.setOnClickListener(v->ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},44));content.addView(ask);return;
        }
        startRealtimeCamera();
    }

    void startRealtimeCamera(){
        PreviewView preview=new PreviewView(this);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        content.addView(preview,new LinearLayout.LayoutParams(-1,dp(430)));
        TextView label=serif("Наведи камеру на предмет",25,true);label.setGravity(Gravity.CENTER);label.setBackground(bg(Color.argb(220,255,255,255),18));content.addView(label);

        ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
        f.addListener(()->{
            try{
                ProcessCameraProvider provider=f.get();
                Preview p=new Preview.Builder().build();p.setSurfaceProvider(preview.getSurfaceProvider());
                ImageAnalysis an=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                an.setAnalyzer(cameraExecutor,img->{
                    long now=System.currentTimeMillis();if(now-lastAnalysis<700){img.close();return;}lastAnalysis=now;
                    android.media.Image media=img.getImage(); if(media==null){img.close();return;}
                    InputImage in=InputImage.fromMediaImage(media,img.getImageInfo().getRotationDegrees());
                    ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(.55f).build()).process(in)
                            .addOnSuccessListener(ls->{if(!ls.isEmpty()){String en=ls.get(0).getText();String de=toGerman(en);runOnUiThread(()->label.setText(de+"\n"+en+" • "+Math.round(ls.get(0).getConfidence()*100)+"%"));}})
                            .addOnCompleteListener(x->img.close());
                });
                provider.unbindAll();provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,an);
            }catch(Exception e){toast("Камера: "+e.getMessage());}
        },ContextCompat.getMainExecutor(this));
    }

    String toGerman(String en){
        String k=en.toLowerCase(Locale.ROOT);
        HashMap<String,String> m=new HashMap<>();
        m.put("chair","der Stuhl");m.put("table","der Tisch");m.put("computer","der Computer");m.put("laptop","der Laptop");m.put("phone","das Handy");
        m.put("dog","der Hund");m.put("cat","die Katze");m.put("cup","die Tasse");m.put("bottle","die Flasche");m.put("book","das Buch");
        m.put("window","das Fenster");m.put("door","die Tür");m.put("television","der Fernseher");m.put("keyboard","die Tastatur");m.put("mouse","die Maus");
        return m.getOrDefault(k,"Найдено: "+en);
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==44&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)cameraGate();}

    @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS){ttsReady=true;tts.setLanguage(Locale.GERMANY);}}

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    @Override protected void onPause(){stopAudio();super.onPause();}
    @Override protected void onDestroy(){stopAudio();if(tts!=null)tts.shutdown();cameraExecutor.shutdown();super.onDestroy();}
}
