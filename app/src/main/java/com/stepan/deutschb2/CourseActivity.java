package com.stepan.deutschb2;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class CourseActivity extends Activity implements TextToSpeech.OnInitListener {
 final int BG=0xfff6f7f2, NAVY=0xff142b28, GREEN=0xff256c55, LIGHT=0xffe6efe7, MUTED=0xff647873, WHITE=0xffffffff, RED=0xffb93a4a, GOLD=0xffffc84d;
 LinearLayout root,body,bottom; ScrollView scroll; TextToSpeech tts; boolean voiceReady=false; String speaking=""; android.media.ToneGenerator feedbackTone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,32);
 JSONArray guides,lessons,bank,dialogues; android.content.SharedPreferences prefs;
 int page=0,unit=0,stage=0,qindex=0,score=0; boolean exam=false,answered=false,repairMode=false,fromSpeech=false; String chosen="",expected="",expectedRu="";
 LearningEngine.Store store; ArrayList<Integer> wrong=new ArrayList<>(); ArrayList<Integer> repairs=new ArrayList<>(); long sessionStarted; int lastSpeechIndex=-1;
 ArrayList<Integer> questionOrder=new ArrayList<>(); final Random random=new Random(19);
 int[] intervals={10,30,60,180,360,1440,4320,14400,28800,43200,86400}; // minutes
 String[] tabs={"Дом","Уроки","Слова","Практика","Камера"};
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
 prefs=getSharedPreferences("course_v4",MODE_PRIVATE);store=new LearningEngine.Store(this);store.touchToday();sessionStarted=System.currentTimeMillis();tts=new TextToSpeech(this,this);
 try{guides=new JSONArray(asset("lesson_guides.json"));lessons=new JSONArray(asset("lessons.json"));bank=new JSONArray(asset("practice_bank.json"));dialogues=new JSONArray(asset("listening_dialogues.json"));}catch(Exception ex){Toast.makeText(this,ex.toString(),1).show();}
 draw(0);}
 String asset(String name)throws Exception{java.io.InputStream in=getAssets().open(name);java.io.ByteArrayOutputStream o=new java.io.ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toString("UTF-8");}
 int d(float x){return (int)(x*getResources().getDisplayMetrics().density);}
 GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;}
 TextView text(String s,int sz,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(NAVY);t.setTextSize(sz);t.setLineSpacing(d(3),1);t.setTypeface(null,bold?1:0);return t;}
 TextView title(String s,int size){TextView t=text(s,size,true);return t;}
 LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
 LinearLayout horizontal(){LinearLayout v=new LinearLayout(this);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,d(h)));}
 void pad(LinearLayout v,int a){v.setPadding(d(a),d(a),d(a),d(a));}
 LinearLayout card(){LinearLayout c=vertical();pad(c,18);c.setBackground(shape(WHITE,22));return c;}
 void add(LinearLayout p,View v,int space){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,d(space));p.addView(v,lp);}
 TextView small(String s){TextView t=text(s,14,false);t.setTextColor(MUTED);return t;}
 TextView button(String s,boolean primary,Runnable action){TextView b=text(s,16,true);b.setTextColor(primary?WHITE:NAVY);b.setGravity(Gravity.CENTER);b.setMinHeight(d(54));b.setPadding(d(14),d(12),d(14),d(12));b.setBackground(shape(primary?GREEN:LIGHT,15));b.setOnClickListener(v->action.run());return b;}
 void stopAudio(){if(tts!=null)tts.stop();speaking="";}
 void talk(String k,String phrase){if(k.equals(speaking)){stopAudio();return;}stopAudio();speaking=k;if(voiceReady)tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null,k);}
 public void onInit(int status){if(status==TextToSpeech.SUCCESS){voiceReady=true;tts.setLanguage(Locale.GERMANY);}}
 void layout(boolean full){stopAudio();getWindow().setStatusBarColor(full?NAVY:BG);getWindow().setNavigationBarColor(full?NAVY:BG);getWindow().getDecorView().setSystemUiVisibility(full?0:(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));root=vertical();root.setBackgroundColor(full?NAVY:BG);root.setFitsSystemWindows(true);setContentView(root);
 scroll=new ScrollView(this);scroll.setFillViewport(false);body=vertical();body.setPadding(d(20),d(14),d(20),d(22));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));bottom=vertical();bottom.setPadding(d(14),d(8),d(14),d(13));bottom.setBackgroundColor(full?NAVY:WHITE);root.addView(bottom);
 }
 void header(String eyebrow,String heading,String desc){TextView e=small(eyebrow.toUpperCase(Locale.ROOT));e.setLetterSpacing(.13f);e.setTextColor(GREEN);add(body,e,14);add(body,title(heading,29),10);if(desc!=null)add(body,small(desc),20);}
 void navbar(int selected){LinearLayout bar=horizontal();for(int i=0;i<tabs.length;i++){final int idx=i;LinearLayout item=vertical();item.setGravity(Gravity.CENTER);item.setPadding(d(2),d(9),d(2),d(9));item.setBackground(shape(i==selected?LIGHT:WHITE,14));TextView ic=text(new String[]{"⌂","◉","▤","✓","◎"}[i],22,true);ic.setGravity(Gravity.CENTER);ic.setTextColor(i==selected?GREEN:MUTED);item.addView(ic);TextView lab=text(tabs[i],11,i==selected);lab.setSingleLine(true);lab.setTextSize(10);lab.setGravity(Gravity.CENTER);lab.setPadding(0,d(3),0,0);lab.setTextColor(i==selected?GREEN:MUTED);item.addView(lab);bar.addView(item,new LinearLayout.LayoutParams(0,d(72),1));item.setOnClickListener(v->draw(idx));}bottom.addView(bar);}
 void draw(int dest){page=dest;layout(false);if(dest==0)home();if(dest==1)path();if(dest==2)dictionary();if(dest==3)practiceHub();if(dest==4)camera();navbar(dest);}
 int done(){return prefs.getInt("unlocked",0);}

 void home(){
  header("MEIN DEUTSCH","Guten Morgen!","Каждый день немного немецкого. Теория, речь и практика.");
  int total=lessons.length();int finished=Math.min(store.unlocked(),total);int percent=(int)Math.round(100.0*finished/total);
  LinearLayout hero=card();hero.setBackground(shape(GREEN,24));
  LinearLayout heroRow=horizontal();LinearLayout details=vertical();
  TextView n=text(percent+"% пути до B2",22,true);n.setTextColor(WHITE);add(details,n,6);
  TextView lvl=text("Текущий уровень: "+level(Math.min(finished,total-1)),15,false);lvl.setTextColor(0xffdeede5);add(details,lvl,9);
  ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgress(percent);add(details,progress,8);
  TextView st=text("🔥 "+store.streak()+" дней подряд",15,true);st.setTextColor(WHITE);add(details,st,0);
  heroRow.addView(details,new LinearLayout.LayoutParams(0,-2,1));
  MascotView tiger=new MascotView(this,store.streak()>1?1:0);heroRow.addView(tiger,new LinearLayout.LayoutParams(d(100),d(132)));tiger.animate().translationY(-d(6)).setDuration(1100).setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).withEndAction(()->tiger.animate().translationY(0).setDuration(1100).start()).start();
  add(hero,heroRow,17);add(hero,button("ПРОДОЛЖИТЬ УРОК",false,()->openLesson(Math.min(finished,total-1))),0);add(body,hero,18);

  LinearLayout today=card();add(today,title("Сегодня",22),12);
  add(today,text("◉ Изучить 1 правило и пройти практику",16,false),10);
  add(today,text("↻ Повторить "+store.dueCount()+" слов и конструкций",16,false),10);
  add(today,text("♫ Потренировать аудирование",16,false),10);
  add(today,text("✎ Написать 3 предложения",16,false),15);
  add(today,button("НАЧАТЬ ТРЕНИРОВКУ",true,()->draw(3)),0);add(body,today,15);

  LinearLayout stats=card();add(stats,title("Статистика",22),12);
  add(stats,text("✓ Ответов: "+store.answers()+"  •  Верных: "+store.right(),16,false),10);
  add(stats,text("▤ Выученных слов: "+store.words(),16,false),10);
  add(stats,text("◷ Время занятий: "+store.minutes()+" мин",16,false),0);add(body,stats,15);

  LinearLayout theory=card();add(theory,title("Справочник грамматики",22),9);
  add(theory,small("Все таблицы склонений, артикли, местоимения, падежи, времена и правила порядка слов."),14);
  add(theory,button("ОТКРЫТЬ ТЕОРИЮ",true,()->reference()),0);add(body,theory,15);
 }
 void reference(){layout(false);header("NACH­SCHLAGEN","Грамматика A1-B2","Подробные правила и таблицы с примерами и переводами.");
  String[] topics={"Артикли, ein/kein/Ø","Падежи и склонения","Личные местоимения","Притяжательные местоимения","Времена: Präsens, Präteritum, Perfekt","Plusquamperfekt и Futur I","Предлоги и управление","Порядок слов"};
  int[] selected={4,5,6,7,15,30,17,10};
  for(int j=0;j<topics.length;j++){final int v=selected[j];LinearLayout c=card();add(c,title(topics[j],18),8);
    if(j==1)add(c,small("Nom: der/die/das • Akk: den/die/das • Dat: dem/der/dem • Gen: des/der/des"),10);
    if(j==4)add(c,small("Präsens - настоящее; Präteritum/Perfekt - прошедшее"),10);
    if(j==5)add(c,small("Ich hatte gelernt - я уже выучил. Ich werde lernen - я буду учиться."),10);
    add(c,button("ПРАВИЛО И ПРИМЕРЫ",true,()->{unit=v;stage=0;showTheory();}),0);add(body,c,12);
  }
 }
 String level(int i){try{return lessons.getJSONObject(Math.max(0,Math.min(i,lessons.length()-1))).getString("level");}catch(Exception e){return "A1";}}
 void path(){header("КУРС","Маршрут A1 → B2",lessons.length()+" разделов грамматики. Теория частями, упражнения, затем повторение.");
 for(int i=0;i<lessons.length();i++){final int idx=i;try{JSONObject l=lessons.getJSONObject(i);LinearLayout c=card();boolean open=i<=done();c.setAlpha(open?1:.48f);
 TextView lab=small(level(i)+"  ·  УРОК "+(i+1)+(prefs.getBoolean("complete_"+i,false)?"   ✓ ПРОЙДЕН":""));add(c,lab,8);add(c,title(l.getString("title"),19),9);
 add(c,small(open?"Теория → примеры → практика":"Откроется после предыдущего урока"),12);
 if(open)add(c,button(prefs.getBoolean("complete_"+i,false)?"ПОВТОРИТЬ":"НАЧАТЬ",true,()->{c.animate().scaleX(.97f).scaleY(.97f).setDuration(95).withEndAction(()->openLesson(idx)).start();}),0);add(body,c,13);
 if(i==lessons.length()-1||!level(i).equals(level(i+1))){final int bound=i;add(body,button("◈ ПРОВЕРКА УРОВНЯ "+level(i),false,()->startExam(bound)),17);}
 }catch(Exception ignored){}}
 }
 void openLesson(int i){unit=i;stage=0;showTheory();body.setAlpha(0f);body.animate().alpha(1f).setDuration(230).start();}
 void showTheory(){layout(false);try{JSONObject g=guides.getJSONObject(unit),l=lessons.getJSONObject(unit);
 header(level(unit)+" · УРОК "+(unit+1),"Разберём правило","Шаг "+(stage+1)+" из 3  •  Сначала пойми логику.");
 LinearLayout c=card();
 if(stage==0){add(c,title(l.getString("title"),24),13);add(c,text(g.getString("why"),16,false),14);
 TextView f=text(g.getString("formula"),16,true);f.setPadding(d(12),d(14),d(12),d(14));f.setBackground(shape(LIGHT,12));add(c,f,10);}
 if(stage==1){add(c,title("Формы и окончания",22),14);
 JSONArray tab=g.getJSONArray("table");for(int j=0;j<tab.length();j++){JSONArray row=tab.getJSONArray(j);LinearLayout rr=horizontal();rr.setBackground(shape(j%2==0?LIGHT:BG,7));for(int k=0;k<row.length();k++){TextView cell=text(row.getString(k),row.length()>3?12:14,j==0);cell.setPadding(d(5),d(12),d(3),d(12));rr.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}add(c,rr,3);}
 add(c,small(g.getString("tip")),3);}
 if(stage==2){add(c,title("Примеры с переводом",22),12);JSONArray ex=l.getJSONArray("examples");JSONArray extra=bank.getJSONObject(unit).getJSONArray("items");int shown=Math.max(5,ex.length());for(int j=0;j<shown;j++){String[] pair;if(j<ex.length())pair=ex.getString(j).split("\\|",2);else {JSONObject item=extra.getJSONObject((j-ex.length())%extra.length());pair=new String[]{item.getString("de"),item.getString("ru")};}final int k=j;LinearLayout line=vertical();line.setBackground(shape(BG,12));pad(line,12);
 add(line,title(pair[0],18),6);add(line,small(pair.length>1?pair[1]:""),12);add(line,button("🔊  СЛУШАТЬ",false,()->talk("ex"+unit+"-"+k,pair[0])),0);add(c,line,11);}
 }
 add(body,c,20);
 add(bottom,button(stage<2?"ДАЛЬШЕ →":"ПРАКТИКА ПО ТЕМЕ →",true,()->{if(stage<2){stage++;showTheory();}else startPractice(unit,false);}),0);
 }catch(Exception e){Toast.makeText(this,e.toString(),1).show();}}
 void startPractice(int topic,boolean isExam){unit=topic;exam=isExam;qindex=0;score=0;repairMode=false;wrong.clear();repairs.clear();questionOrder.clear();
 int n=isExam?15:10;for(int i=0;i<n;i++)questionOrder.add(i);showQuestion();}
 JSONObject item(int at)throws Exception{int idx=(at*3+(unit%4))%bank.getJSONObject(unit).getJSONArray("items").length();return bank.getJSONObject(unit).getJSONArray("items").getJSONObject(idx);}
 String normal(String s){return LearningEngine.normalize(s);}
 void showQuestion(){if(qindex>=(repairMode?repairs.size():questionOrder.size())){results();return;}answered=false;chosen="";layout(true);
 try{int actual=repairMode?repairs.get(qindex):qindex;JSONObject item=item(actual);String de=item.getString("de"),ru=item.getString("ru"),focus=item.getString("focus");expected=de;expectedRu=ru;
 LinearLayout top=horizontal();TextView back=text("✕",25,true);back.setTextColor(WHITE);top.addView(back);back.setOnClickListener(v->draw(1));
 ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(repairMode?repairs.size():questionOrder.size());progress.setProgress(qindex+1);
 LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(12),1);p.setMargins(d(15),0,0,0);top.addView(progress,p);add(body,top,25);
 int type=repairMode?((actual%8)==6?5:actual%8):qindex%8;
 TextView heading=title(new String[]{"Выбери перевод","Заполни пропуск","Собери предложение","Что ты услышал?","Выбери форму","Напиши предложение","Повтори вслух","Найди ошибку"}[type],24);
 heading.setTextColor(WHITE);add(body,heading,17);
 if(type==0){addGermanPrompt(de,22);
 ArrayList<String> variants=new ArrayList<>();variants.add(ru);JSONArray all=bank.getJSONObject(unit).getJSONArray("items");for(int j=0;j<all.length()&&variants.size()<4;j++){String candidate=all.getJSONObject((j+qindex+3)%all.length()).getString("ru");if(!variants.contains(candidate))variants.add(candidate);}Collections.shuffle(variants);choiceList(variants,ru);}
 else if(type==1||type==4){String[] words=de.replaceAll("[!?.,]","").split(" ");int pos=0;for(int j=0;j<words.length;j++)if(words[j].equalsIgnoreCase(focus)){pos=j;break;}String target=words[pos];String prompt=de.replaceFirst("(?i)\\b"+java.util.regex.Pattern.quote(target)+"\\b","____");
 addGermanPrompt(prompt,8);add(body,smallLight("Нажми на немецкое слово для подсказки. Перевод фразы появится после ответа."),24);
 ArrayList<String> opts=new ArrayList<>(Arrays.asList(LearningEngine.topicForms(unit,target)));if(unit==23&&focus.equals("Ø"))opts=new ArrayList<>(Arrays.asList("Ø","ein","eine","kein"));Collections.shuffle(opts);choiceList(opts,target);}
 else if(type==2){TextView line=text(ru,18,true);line.setTextColor(WHITE);add(body,line,16);wordBank(de);}
 else if(type==3){dialogueQuestion();}
 else if(type==5){TextView line=text(ru,18,true);line.setTextColor(WHITE);add(body,line,16);inputQuestion("Переведи на немецкий",de);}
 else if(type==6){addGermanPrompt(de,16);add(body,button("🔊  СЛУШАТЬ",false,()->talk("say"+qindex,de)),14);add(body,button("🎙  ПРОИЗНЕСТИ",false,()->recognize(de)),12);
 add(body,smallLight("После распознавания приложение сравнит текст с эталоном. Порог: 70%."),0);}
 else {String erroneous=makeError(de,focus);addGermanPrompt(erroneous,12);inputQuestion("Исправь ошибку в предложении",de);}
 }catch(Exception e){Toast.makeText(this,e.toString(),1).show();}
 }
 TextView titleLight(String s){TextView t=title(s,22);t.setTextColor(WHITE);return t;}
 TextView smallLight(String s){TextView t=small(s);t.setTextColor(0xffb7ccc5);return t;}

 void dialogueQuestion(){
  try{
    ArrayList<JSONObject> matching=new ArrayList<>();
    String target=level(unit);
    for(int i=0;i<dialogues.length();i++){JSONObject d=dialogues.getJSONObject(i);if(d.getString("level").equals(target))matching.add(d);}
    if(matching.isEmpty()){inputQuestion("Напиши услышанное",expected);return;}
    JSONObject dialogue=matching.get((qindex/3)%matching.size());
    String audio=dialogue.getString("de"),statement=dialogue.getString("statement"),correct=dialogue.getString("answer");
    expectedRu=dialogue.getString("explanation");
    expected=audio;
    add(body,button("🔊 СЛУШАТЬ ДИАЛОГ",false,()->talk("dialogue_"+qindex,audio)),20);
    add(body,smallLight("Прослушай дважды. Оцени утверждение."),13);
    TextView question=titleLight(statement);add(body,question,20);
    ArrayList<String> variants=new ArrayList<>(Arrays.asList("Richtig","Falsch"));
    choiceList(variants,correct);
  }catch(Exception e){Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();}
 }
 void choiceList(ArrayList<String> options,String answer){LinearLayout list=vertical();for(String x:options){TextView b=text(x,17,false);b.setPadding(d(18),d(20),d(14),d(20));b.setBackground(shape(0xff263e39,16));b.setTextColor(WHITE);add(list,b,10);b.setOnClickListener(v->{chosen=x;for(int j=0;j<list.getChildCount();j++){View z=list.getChildAt(j);z.setBackground(shape(z==b?GREEN:0xff263e39,16));}buttonArea(answer);});}add(body,list,20);buttonArea(answer);}
 void buttonArea(String answer){bottom.removeAllViews();add(bottom,button("ДАЛЕЕ →",true,()->{if(answered){next(true);}else{feedback(normal(chosen).equals(normal(answer)),answer);}}),0);}
 void wordBank(String de){ArrayList<String> parts=new ArrayList<>(Arrays.asList(de.replaceAll("[.!?]","").split(" ")));Collections.shuffle(parts);
 TextView output=titleLight("...");add(body,output,23);LinearLayout grid=vertical();ArrayList<String> built=new ArrayList<>();
 for(String word:parts){TextView choice=text(word,17,false);choice.setTextColor(WHITE);choice.setPadding(d(15),d(12),d(15),d(12));choice.setBackground(shape(0xff273f39,12));add(grid,choice,7);
 choice.setOnClickListener(v->{built.add(word);choice.setVisibility(View.GONE);chosen=String.join(" ",built);output.setText(chosen);});}
 add(body,grid,10);buttonArea(de);}
 void inputQuestion(String hint,String correct){EditText field=new EditText(this);field.setTextColor(WHITE);field.setHintTextColor(0xffa7b9b3);field.setHint(hint);field.setTextSize(18);field.setMinLines(2);field.setBackground(shape(0xff263e39,16));field.setPadding(d(18),d(15),d(18),d(15));add(body,field,20);
 bottom.removeAllViews();add(bottom,button("ДАЛЕЕ →",true,()->{chosen=field.getText().toString();feedback(normal(chosen).equals(normal(correct)),correct);}),0);}

 void softFeedback(boolean ok){
  new Thread(()->{try{
    int rate=22050;int ms=ok?190:145;int n=rate*ms/1000;byte[] pcm=new byte[n*2];
    for(int i=0;i<n;i++){
      double t=(double)i/rate;double fade=Math.sin(Math.PI*(double)i/n);fade=fade*fade;
      double hz=ok?(i<n/2?523.25:659.25):220.0;
      double wave=Math.sin(2*Math.PI*hz*t)*(ok?.18:.09)*fade;
      short sample=(short)(wave*32767);pcm[2*i]=(byte)(sample&255);pcm[2*i+1]=(byte)((sample>>8)&255);
    }
    android.media.AudioTrack tr=new android.media.AudioTrack(android.media.AudioManager.STREAM_MUSIC,rate,android.media.AudioFormat.CHANNEL_OUT_MONO,android.media.AudioFormat.ENCODING_PCM_16BIT,pcm.length,android.media.AudioTrack.MODE_STATIC);
    tr.write(pcm,0,pcm.length);tr.play();Thread.sleep(ms+30);tr.release();
  }catch(Exception ignored){}}).start();
 }
 void feedback(boolean ok,String solution){if(answered)return;answered=true;softFeedback(ok);if(ok)score++;else if(!repairMode)wrong.add(qindex);store.record("topic_"+unit+"_"+(repairMode?repairs.get(qindex):qindex),ok);
 bottom.removeAllViews();TextView msg=text((ok?"✓ Отлично!":"Правильный ответ:")+" \n"+solution+"\n"+expectedRu,16,true);msg.setTextColor(ok?GREEN:RED);msg.setPadding(d(12),d(10),d(12),d(12));msg.setBackground(shape(ok?LIGHT:0xffffeded,13));add(bottom,msg,10);
 add(bottom,button("ДАЛЕЕ →",true,()->next(ok)),0);
 }
 void next(boolean ok){if(repairMode&&!ok)repairs.add(repairs.get(qindex));qindex++;showQuestion();}

 void showRepairIntro(){layout(true);add(body,titleLight("Работа над ошибками"),20);add(body,smallLight("Ты допустил "+wrong.size()+" ошибок. Сейчас повторятся те же задания. Нужно исправить каждую, чтобы завершить урок."),22);add(bottom,button("ИСПРАВИТЬ ОШИБКИ",true,()->showQuestion()),0);}
 String makeError(String de,String focus){
    String[] words=de.split(" ");int at=-1;
    for(int i=0;i<words.length;i++){if(normal(words[i]).equals(normal(focus))){at=i;break;}}
    if(at<0)at=Math.min(1,words.length-1);
    String word=words[at],replacement;
    if(word.equalsIgnoreCase("der"))replacement="den";
    else if(word.equalsIgnoreCase("die"))replacement="der";
    else if(word.equalsIgnoreCase("das"))replacement="die";
    else if(word.equalsIgnoreCase("ein"))replacement="eine";
    else if(word.equalsIgnoreCase("eine"))replacement="ein";
    else if(word.equalsIgnoreCase("den"))replacement="dem";
    else if(word.equalsIgnoreCase("einen"))replacement="ein";
    else if(word.equalsIgnoreCase("dem"))replacement="den";
    else if(word.equalsIgnoreCase("ist"))replacement="sind";
    else if(word.equalsIgnoreCase("sind"))replacement="ist";
    else if(word.equalsIgnoreCase("habe"))replacement="hat";
    else if(word.equalsIgnoreCase("hat"))replacement="habe";
    else if(word.length()>3)replacement=word+"en";
    else replacement="nicht";
    words[at]=replacement;
    return String.join(" ",words);
 }
 void addGermanPrompt(String sentence,int spacing){
   LinearLayout wrap=vertical();TextView hint=smallLight("Нажми на слово, чтобы увидеть его перевод");add(wrap,hint,10);
   android.widget.TextView line=titleLight(sentence);line.setTextIsSelectable(false);
   android.text.SpannableString span=new android.text.SpannableString(sentence);
   java.util.regex.Matcher match=java.util.regex.Pattern.compile("[\\p{L}ÄÖÜäöüß]+").matcher(sentence);
   while(match.find()){String w=match.group();span.setSpan(new android.text.style.ClickableSpan(){
      public void onClick(View v){wordHint(w);}
      public void updateDrawState(android.text.TextPaint p){p.setColor(WHITE);p.setUnderlineText(true);}
   },match.start(),match.end(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
   line.setText(span);line.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());line.setHighlightColor(0xff457968);add(wrap,line,0);add(body,wrap,spacing);
 }
 String wordTranslation(String w){try{for(int i=0;i<bank.length();i++){JSONArray items=bank.getJSONObject(i).getJSONArray("items");for(int j=0;j<items.length();j++){JSONObject x=items.getJSONObject(j);String de=x.getString("de"),ru=x.getString("ru");if(de.toLowerCase(Locale.GERMAN).contains(w))return ru;}}}catch(Exception ignored){}return "Перевод слова уточняется в контексте";}
 void wordHint(String word){
   String basic=normal(word);HashMap<String,String> m=new HashMap<>();
   String[] pairs={"der=определённый артикль, муж. род","die=определённый артикль, жен. род/мн. число","das=определённый артикль, ср. род","ein=неопределённый артикль","eine=неопределённый артикль, жен. род","einen=неопределённый артикль, Akkusativ муж. рода","keine=нет, отрицание существительного","ich=я","du=ты","ist=есть, является","sind=являются","habe=имею","hat=имеет","brauche=нуждаюсь","wohnung=квартира (die)","universität=университет (die)","tisch=стол (der)","bahnhof=вокзал (der)","schwer=тяжёлый","alt=старый","groß=большой","wasser=вода (das)","student=студент (der)","auto=автомобиль (das)","lehrer=учитель (der)"};
   for(String p:pairs){String[] ab=p.split("=",2);m.put(ab[0],ab[1]);}
   String translation=m.getOrDefault(basic,wordTranslation(basic));
   android.widget.PopupWindow popup=new android.widget.PopupWindow(this);
   TextView bubble=text(translation,14,true);bubble.setPadding(d(13),d(10),d(13),d(10));bubble.setBackground(shape(LIGHT,12));
   popup.setContentView(bubble);popup.setWidth(-2);popup.setHeight(-2);popup.setOutsideTouchable(true);popup.setBackgroundDrawable(shape(LIGHT,12));popup.setElevation(d(7));popup.showAtLocation(root,Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,d(190));
   bubble.setOnClickListener(v->popup.dismiss());new Handler().postDelayed(()->{if(popup.isShowing())popup.dismiss();},3200);
 }
 void results(){if(!repairMode&&!wrong.isEmpty()){repairMode=true;repairs.addAll(wrong);qindex=0;showRepairIntro();return;}if(repairMode){for(int i=0;i<repairs.size();i++){} }layout(true);TextView trophy=text(score>=8?"★ Отличная работа!":"Урок завершён",28,true);trophy.setTextColor(GOLD);add(body,trophy,28);
 TextView points=text(score+" / "+questionOrder.size()+" правильных",25,true);points.setTextColor(WHITE);add(body,points,14);
 add(body,smallLight("Повторяй ошибочные формы. В следующих уроках будут снова встречаться знакомые слова."),20);
 boolean pass=wrong.isEmpty()||repairMode;if(pass&&!exam){store.complete(unit);prefs.edit().putBoolean("complete_"+unit,true).putInt("unlocked",Math.max(done(),unit+1)).apply();}
 add(bottom,button("НАЗАД К МАРШРУТУ",true,()->draw(1)),0);
 }

 ArrayList<String[]> vocabulary=new ArrayList<>();
 int cardIndex=0;boolean cardRevealed=false,cardRuToDe=false;
 void readVocabulary(){
  if(!vocabulary.isEmpty())return;
  try{java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(getAssets().open("words.csv")));
   String line;r.readLine();while((line=r.readLine())!=null){String[] p=line.split(";",-1);if(p.length>=7)vocabulary.add(p);}
  }catch(Exception ignored){}
 }
 void flashcard(){readVocabulary();if(vocabulary.isEmpty()){draw(2);return;}layout(false);
   header("KARTEIKARTEN","Вспоминай активно","Переверни карточку, затем обязательно введи перевод.");
   String[] w=vocabulary.get(cardIndex%vocabulary.size());cardRuToDe=(cardIndex%2==1);String front=cardRuToDe?w[4]:w[1]+" "+w[2],back=cardRuToDe?(w[1]+" "+w[2]):w[4];
   LinearLayout c=card();c.setMinimumHeight(d(230));c.setGravity(Gravity.CENTER);
   TextView face=title(front,28);face.setGravity(Gravity.CENTER);add(c,face,15);
   TextView clue=small("Нажми, чтобы перевернуть");clue.setGravity(Gravity.CENTER);add(c,clue,0);
   c.setOnClickListener(v->{cardRevealed=!cardRevealed;face.setText(cardRevealed?back:front);});
   add(body,c,20);
   EditText in=new EditText(this);in.setSingleLine(true);in.setHint(cardRuToDe?"Артикль + слово по-немецки":"Перевод на русский");in.setBackground(shape(WHITE,15));in.setPadding(d(16),d(17),d(16),d(17));add(body,in,14);
   TextView feedback=small("");add(body,feedback,14);
   add(body,button("ПРОВЕРИТЬ ПЕРЕВОД",true,()->{boolean ok=LearningEngine.acceptedAnswer(back,in.getText().toString());feedback.setText(ok?"✓ Верно":"Правильно: "+back);
     store.record("word_"+cardIndex,ok);if(ok&&store.errors("word_"+cardIndex)==0)store.markWord();}),12);
   add(body,button("СЛЕДУЮЩАЯ КАРТОЧКА",false,()->{cardIndex++;cardRevealed=false;flashcard();}),0);
 }
 void dictionary(){header("WÖRTER","Словарь","Слова с родом, множественным числом и переводом.");add(body,button("↺ ПОВТОРЯТЬ КАРТОЧКИ",true,()->flashcard()),18);
 try{java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(getAssets().open("words.csv")));String row;r.readLine();while((row=r.readLine())!=null){String[] p=row.split(";",-1);if(p.length<7)continue;LinearLayout c=card();add(c,title(p[1]+" "+p[2],22),5);add(c,small(p[3]+" · "+p[4]+" · "+p[0]),9);add(c,text(p[5],17,true),5);add(c,small(p[6]),12);String word=p[1]+" "+p[2];add(c,button("🔊 СЛУШАТЬ",false,()->talk(word,word)),0);add(body,c,12);}}catch(Exception e){} }
 void writing(){
    layout(false);header("SCHREIBEN B1/B2","Письменная практика","Напиши небольшой связный текст. Офлайн проверяется объём и наличие ключевых слов. Смысл, грамматику и стиль оценивай по чек-листу.");
    int lvl=done()<18?0:done()<21?1:2;
    String topic=lvl==0?"Сообщение другу: ты переносишь встречу. Объясни причину и предложи новое время.":lvl==1?"Письмо в университет: спроси об условиях поступления, документах и сроках.":"Аргументированное мнение: стоит ли студентам совмещать учёбу и работу? Приведи 2 аргумента, контраргумент и вывод.";
    add(body,title(topic,21),16);
    add(body,small(lvl==0?"40-60 слов":lvl==1?"80-120 слов":"150-200 слов"),15);
    EditText essay=new EditText(this);essay.setGravity(Gravity.TOP);essay.setHint("Пиши по-немецки...");essay.setMinLines(10);essay.setBackground(shape(WHITE,16));essay.setPadding(d(14),d(16),d(14),d(16));add(body,essay,15);
    add(body,button("СОХРАНИТЬ И ПРОВЕРИТЬ ОБЪЁМ",true,()->{
        String txt=essay.getText().toString().trim();int count=txt.isEmpty()?0:txt.split("\\s+").length;
        prefs.edit().putString("writing_"+lvl,txt).apply();
        new AlertDialog.Builder(this).setTitle("Написано: "+count+" слов").setMessage("Чек-лист: 1) Выполнены все пункты задания? 2) Глагол в главном предложении на позиции 2? 3) После weil/dass глагол в конце? 4) Верные артикли и падежи? 5) Есть связки и вывод?\\n\\nЭто самопроверка, не автоматическая оценка Goethe.").setPositiveButton("Понятно",null).show();
    }),10);
    add(body,button("ВЕРНУТЬСЯ К ПРАКТИКЕ",false,()->draw(3)),0);
 }

 android.media.MediaPlayer importedPlayer;
 void importAudio(){
   Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("audio/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
   try{startActivityForResult(intent,107);}catch(Exception e){Toast.makeText(this,"Не удалось открыть аудиофайл",1).show();}
 }
 void playImported(){
   try{
     String uri=prefs.getString("audio_uri","");if(uri.isEmpty()){importAudio();return;}
     if(importedPlayer!=null){importedPlayer.release();importedPlayer=null;return;}
     importedPlayer=new android.media.MediaPlayer();
     importedPlayer.setDataSource(this,android.net.Uri.parse(uri));importedPlayer.prepare();
     importedPlayer.setOnCompletionListener(player->{player.release();importedPlayer=null;});importedPlayer.start();
   }catch(Exception ex){Toast.makeText(this,"Не удалось воспроизвести: "+ex.getMessage(),1).show();}
 }
 void practiceHub(){header("НАВЫКИ","Тренировка","Проверяй грамматику, лексику, аудирование и речь на материалах уже открытых тем.");
 String[] n={"Слушать свой MP3","Импорт своего MP3","Карточки слов","Пробелы и окончания","Сборка предложений","Аудирование","Письмо и перевод","Произношение","Смешанное повторение"};
 for(String mode:n){LinearLayout c=card();add(c,title(mode,19),11);add(c,button("НАЧАТЬ",true,()->{if(mode.equals("Письмо и перевод"))writing();else if(mode.equals("Слушать свой MP3"))playImported();else if(mode.equals("Импорт своего MP3"))importAudio();else if(mode.equals("Карточки слов"))flashcard();else startPractice(Math.min(done(),22),false);}),0);add(body,c,12);}
 }

 static class MascotView extends View{
   final android.graphics.Paint p=new android.graphics.Paint(3);int mood;public MascotView(Context c,int m){super(c);mood=m;}
   void oval(android.graphics.Canvas c,int col,float a,float b,float x,float y){p.setColor(col);p.setStyle(android.graphics.Paint.Style.FILL);c.drawOval(a,b,x,y,p);}
   protected void onDraw(android.graphics.Canvas cv){super.onDraw(cv);cv.save();cv.scale(getWidth()/140f,getHeight()/170f);
     int orange=0xffffaa46,dk=0xff273b32,cream=0xffffe4b9;
     oval(cv,orange,15,35,125,150);oval(cv,orange,13,13,45,57);oval(cv,orange,92,13,126,57);
     oval(cv,0xfff9dba5,24,52,116,133);oval(cv,cream,45,92,95,135);
     oval(cv,dk,39,67,51,83);oval(cv,dk,88,67,100,83);oval(cv,dk,64,98,77,108);
     p.setColor(dk);p.setStrokeWidth(5);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
     cv.drawLine(52,35,59,55,p);cv.drawLine(82,35,77,55,p);
     cv.drawLine(19,84,35,88,p);cv.drawLine(104,88,121,84,p);
     if(mood==1){p.setStyle(android.graphics.Paint.Style.STROKE);cv.drawArc(51,102,88,120,0,180,false,p);p.setStyle(android.graphics.Paint.Style.FILL);}
     else{p.setStrokeWidth(3);cv.drawLine(64,116,79,116,p);}
     cv.restore();
   }
 }
 void camera(){header("WÖRTER UM MICH","Камера и перевод","Для распознавания предметов понадобится интернет и настроенный ключ облачного сервиса.");LinearLayout c=card();add(c,text("В этом выпуске модуль распознавания открывается отдельно, чтобы обучение и проверка ответов работали без интернета.",16,false),15);add(c,button("ОТКРЫТЬ КАМЕРУ",true,()->{Intent intent=new Intent(this,CameraActivity.class);startActivity(intent);}),0);add(body,c,12);}
 void startExam(int end){startPractice(end,true);}
 void recognize(String target){Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"de-DE");intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
 lastSpeechIndex=qindex;try{startActivityForResult(intent,63);}catch(Exception e){new AlertDialog.Builder(this).setMessage("Распознавание речи недоступно без установленной службы Android Speech.").setPositiveButton("OK",null).show();}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==107&&result==RESULT_OK&&data!=null){android.net.Uri uri=data.getData();if(uri!=null){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}prefs.edit().putString("audio_uri",uri.toString()).apply();new AlertDialog.Builder(this).setTitle("Аудиофайл подключён").setMessage("Файл доступен локально для воспроизведения.").setPositiveButton("Слушать",(d,w)->playImported()).setNegativeButton("Позже",null).show();}}if(request==63&&result==RESULT_OK&&data!=null){ArrayList<String> txt=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(txt!=null&&!txt.isEmpty()){String heard=txt.get(0);int pct=(int)Math.round(LearningEngine.similarity(expected,heard)*100);boolean ok=LearningEngine.acceptedSpeech(expected,heard);feedback(ok,expected);new AlertDialog.Builder(this).setTitle("Совпадение: "+pct+"%"+(ok?" ✓":"")).setMessage("Распознано: "+heard+"\nЭталон: "+expected+"\n"+(ok?"Порог 70% пройден.":"Попробуй ещё раз в конце урока.")).setPositiveButton("Понятно",null).show();}}}
 @Override protected void onPause(){stopAudio();super.onPause();}
 @Override protected void onDestroy(){stopAudio();if(tts!=null)tts.shutdown();if(importedPlayer!=null)importedPlayer.release();if(feedbackTone!=null)feedbackTone.release();store.addSession(System.currentTimeMillis()-sessionStarted);super.onDestroy();}
}