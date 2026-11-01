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
 TextView button(String s,boolean primary,Runnable action){
 TextView b=text(s,16,true);b.setTextColor(primary?WHITE:NAVY);b.setGravity(Gravity.CENTER);
 b.setMinHeight(d(54));b.setPadding(d(14),d(12),d(14),d(12));
 android.graphics.drawable.GradientDrawable face=shape(primary?GREEN:LIGHT,15);
 b.setBackground(face);b.setElevation(d(4));b.setStateListAnimator(null);
 pressFeedback(b,action);return b;
}
void pressFeedback(View b,Runnable action){
 b.setOnTouchListener((v,e)->{
   if(e.getAction()==MotionEvent.ACTION_DOWN){
     v.animate().cancel();v.animate().scaleX(.965f).scaleY(.948f).translationY(d(2)).setDuration(75).start();
   }else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){
     v.animate().cancel();v.animate().scaleX(1).scaleY(1).translationY(0).setDuration(145).setInterpolator(new android.view.animation.OvershootInterpolator(.65f)).start();
   }
   return false;
 });
 b.setOnClickListener(v->{if(!v.isEnabled())return;v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);action.run();});
}
void enterAnimation(){
  if(body!=null){body.setAlpha(0f);body.setTranslationX(d(18));body.animate().alpha(1).translationX(0).setDuration(235).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
}

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
 LinearLayout backRow=horizontal();TextView back=text("‹  Назад",16,true);back.setTextColor(GREEN);back.setPadding(d(7),d(8),d(8),d(14));backRow.addView(back);back.setOnClickListener(v->previousTheory());add(body,backRow,10);
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
 }catch(Exception e){Toast.makeText(this,e.toString(),1).show();}enterAnimation();}
 void previousTheory(){if(stage>0){stage--;showTheory();}else draw(1);}
 @Override public void onBackPressed(){if(page==1&&stage>0){previousTheory();return;}if(page==1){draw(1);return;}super.onBackPressed();}
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
 }catch(Exception e){Toast.makeText(this,e.toString(),1).show();}enterAnimation();
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

 void wordBank(String de){
   ArrayList<String> source=new ArrayList<>(Arrays.asList(de.replaceAll("[.!?]","").split("\\s+")));
   ArrayList<Integer> shuffled=new ArrayList<>();for(int i=0;i<source.size();i++)shuffled.add(i);Collections.shuffle(shuffled);
   ArrayList<Integer> selected=new ArrayList<>();
   TextView info=smallLight("Нажми на слово, чтобы добавить. Нажми на выбранное слово, чтобы вернуть.");
   add(body,info,13);
   LinearLayout answerLine=vertical(),available=vertical();
   answerLine.setPadding(d(8),d(10),d(8),d(12));answerLine.setMinimumHeight(d(105));
   answerLine.setBackground(shape(0xff25413a,15));
   add(body,answerLine,16);add(body,available,8);
   Runnable[] redraw=new Runnable[1];
   redraw[0]=()->{
     answerLine.removeAllViews();available.removeAllViews();
     if(selected.isEmpty()){TextView placeholder=smallLight("Нажимай на слова ниже");add(answerLine,placeholder,13);}
     LinearLayout row=null;int i=0;
     for(int wordId:selected){
       if(i%3==0){row=horizontal();row.setGravity(Gravity.CENTER_VERTICAL);add(answerLine,row,9);}
       TextView token=tokenButton(source.get(wordId),true);
       LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(54),1);p.setMargins(d(4),0,d(4),0);row.addView(token,p);
       final int remove=wordId;
       pressFeedback(token,()->{if(answered)return;selected.remove(Integer.valueOf(remove));redraw[0].run();});
       token.setAlpha(0);token.setScaleX(.84f);token.setScaleY(.84f);
       token.animate().alpha(1).scaleX(1).scaleY(1).setDuration(180).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
       i++;
     }
     row=null;i=0;
     for(int id:shuffled){if(selected.contains(id))continue;
       if(i%3==0){row=horizontal();row.setGravity(Gravity.CENTER_VERTICAL);add(available,row,9);}
       TextView token=tokenButton(source.get(id),false);
       LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(56),1);p.setMargins(d(4),0,d(4),0);row.addView(token,p);
       final int addId=id;pressFeedback(token,()->{if(answered)return;selected.add(addId);redraw[0].run();});
       i++;
     }
     ArrayList<String> built=new ArrayList<>();for(int id:selected)built.add(source.get(id));
     chosen=String.join(" ",built);
   };
   redraw[0].run();
   buttonArea(de);
 }
 TextView tokenButton(String word,boolean active){
   TextView view=text(word,17,true);view.setGravity(Gravity.CENTER);view.setTextColor(active?NAVY:WHITE);
   view.setPadding(d(3),d(5),d(3),d(5));view.setBackground(shape(active?0xffdbece4:0xff33534a,13));
   view.setElevation(d(2));return view;
 }
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
 bottom.removeAllViews();TextView msg=text((ok?"✓ Отлично!":"Правильный ответ:")+" \n"+solution+"\n"+expectedRu,16,true);msg.setTextColor(ok?GREEN:RED);msg.setPadding(d(12),d(10),d(12),d(12));msg.setBackground(shape(ok?LIGHT:0xffffeded,13));add(bottom,msg,10);msg.setAlpha(0);msg.setTranslationY(d(50));msg.animate().alpha(1).translationY(0).setDuration(320).setInterpolator(new android.view.animation.OvershootInterpolator(.7f)).start();
 TextView advance=button("ДАЛЕЕ →",true,()->next(ok));add(bottom,advance,0);advance.setAlpha(0);advance.animate().alpha(1).setStartDelay(120).setDuration(240).start();
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
   LinearLayout wrap=vertical();
   add(wrap,smallLight("Нажми на слово: перевод и произношение"),10);
   TextView line=titleLight(sentence);line.setTextIsSelectable(false);
   android.text.SpannableString span=new android.text.SpannableString(sentence);
   java.util.regex.Matcher match=java.util.regex.Pattern.compile("[\\p{L}ÄÖÜäöüß]+").matcher(sentence);
   while(match.find()){
     String word=match.group();int start=match.start(),end=match.end();
     span.setSpan(new android.text.style.ClickableSpan(){
       public void onClick(View view){wordHint(word,(TextView)view,start,end);}
       public void updateDrawState(android.text.TextPaint paint){paint.setColor(WHITE);paint.setUnderlineText(false);}
     },start,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
   }
   line.setText(span);line.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());line.setHighlightColor(0xff33534a);
   add(wrap,line,0);add(body,wrap,spacing);
 }
 String wordTranslation(String w){
   readVocabulary();
   for(String[] p:vocabulary){if(p.length<5)continue;
     if(p[2].equalsIgnoreCase(w)||p[3].equalsIgnoreCase(w))return p[4]+" ("+p[1]+" "+p[2]+")";
   }
   HashMap<String,String> common=new HashMap<>();
   String entries="ist=есть, является|sind=есть, являются|interessant=интересный|schwer=тяжёлый|groß=большой|klein=маленький|gut=хороший|alt=старый|neu=новый|mein=мой|meine=моя|dein=твой|deine=твоя|du=ты|ich=я|wir=мы|ihr=вы|er=он|sie=она, они|es=оно|lernen=учить|lerne=учу|lernst=учишь|gelernt=выучил|kommen=приходить|kommt=приходит|gekommen=пришёл|gehe=иду|geht=идёт|gehen=идти|ging=шёл|wird=станет, будет|werden=становиться, будут|habe=имею|hast=имеешь|hat=имеет|hatte=имел|nicht=не|kein=нет, никакой|keine=нет, никакая|einen=артикль Akkusativ мужского рода|einer=артикль Dativ женского рода|mit=с, на|für=для|auf=на, по|in=в|zu=к, в|bei=у, при|nach=в, после|aus=из|im=в (in dem)|am=в, на (an dem)|um=в (время)|von=от|weil=потому что|dass=что|wenn=если, когда|obwohl=хотя|deshalb=поэтому|trotzdem=несмотря на это|morgen=завтра|heute=сегодня|gestern=вчера|früh=рано|spät=поздно|immer=всегда|oft=часто|viel=много|sehr=очень|auch=тоже|schon=уже|noch=ещё|ich=я|weiß=знаю|möchte=хотел бы|muss=должен|kann=могу|will=хочу|besonders=особенно|wichtig=важный|möglich=возможный|besser=лучше|größer=больше|schneller=быстрее|schöne=красивый|schön=красивый|brauche=мне нужно|brauchen=нуждаться|warten=ждать|sprechen=говорить|spreche=говорю|spricht=говорит|verstanden=понял|wurde=был (пассив)|würde=бы (сослагательное)|hätte=имел бы|wäre=был бы|bin=есть (я)|bist=есть (ты)|war=был";
   for(String entry:entries.split("\\|")){String[] pair=entry.split("=",2);if(pair.length==2)common.put(pair[0],pair[1]);}
   return common.getOrDefault(w,"Значение уточняется в контексте");
 }

 void wordHint(String word,TextView line,int start,int end){
   String basic=normal(word);String translation=wordTranslation(basic);
   if(translation.startsWith("Значение уточняется"))translation=contextualHint(word);
   talk("hint_"+System.nanoTime(),word);
   android.text.Layout layout=line.getLayout();if(layout==null)return;
   int row=layout.getLineForOffset(start);
   float x=layout.getPrimaryHorizontal(start);
   float endX=layout.getPrimaryHorizontal(Math.max(start,end-1));
   int[] position=new int[2];line.getLocationOnScreen(position);
   int screenWidth=getResources().getDisplayMetrics().widthPixels;
   int width=Math.min(d(240),screenWidth-d(24));
   int center=(int)(position[0]+line.getTotalPaddingLeft()+(x+endX)/2);
   int left=Math.max(d(12),Math.min(screenWidth-width-d(12),center-width/2));
   int top=position[1]+layout.getLineBottom(row)+d(8);
   android.widget.PopupWindow popup=new android.widget.PopupWindow(this);
   LinearLayout content=vertical();content.setPadding(d(12),d(10),d(12),d(10));content.setBackground(shape(LIGHT,14));
   TextView value=text(word+"  🔊",13,true);value.setTextColor(GREEN);add(content,value,2);
   TextView meaning=text(translation,14,true);meaning.setTextColor(NAVY);add(content,meaning,0);
   popup.setContentView(content);popup.setWidth(width);popup.setHeight(-2);popup.setOutsideTouchable(true);
   popup.setBackgroundDrawable(shape(LIGHT,14));popup.setElevation(d(8));content.setAlpha(0);content.setScaleX(.94f);content.setScaleY(.94f);
   popup.showAtLocation(root,Gravity.TOP|Gravity.LEFT,left,top);
   content.animate().alpha(1).scaleX(1).scaleY(1).setDuration(180).start();
   content.setOnClickListener(v->{talk("hint_"+System.nanoTime(),word);});
   new Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(popup.isShowing())popup.dismiss();},3800);
 }
 String contextualHint(String word){
   String basic=normal(word);String de=expected,ru=expectedRu;
   if(basic.equals("erklärt"))return "объясняет";
   if(basic.equals("lehrerin"))return "учительница";
   if(basic.equals("bücher"))return "книги (das Buch)";
   if(basic.equals("interessant"))return "интересный";
   if(basic.equals("das")||basic.equals("die")||basic.equals("der"))return "определённый артикль, форма зависит от рода и падежа";
   if(basic.equals("wörter"))return "слова (das Wort)";
   if(basic.equals("man"))return "люди, кто-то (неопределённое лицо)";
   return "Контекст: "+ru;
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

 ArrayList<Integer> weakCards=new ArrayList<>();
 void flashcard(){
   readVocabulary();if(vocabulary.isEmpty()){draw(2);return;}
   layout(false);header("KARTEIKARTEN","Вспоминай активно","Ответь самостоятельно. Сложные слова вернутся в следующих карточках.");
   final int index=(cardIndex>0&&cardIndex%4==0&&!weakCards.isEmpty())?weakCards.remove(0):cardIndex%vocabulary.size();
   String[] w=vocabulary.get(index);cardRuToDe=(cardIndex%2==1);
   String front=cardRuToDe?w[4]:w[1]+" "+w[2],back=cardRuToDe?(w[1]+" "+w[2]):w[4];
   LinearLayout c=card();c.setMinimumHeight(d(205));c.setGravity(Gravity.CENTER);
   TextView face=title(front,29);face.setGravity(Gravity.CENTER);add(c,face,12);
   TextView clue=small("Нажми для подсказки на обратной стороне");clue.setGravity(Gravity.CENTER);add(c,clue,0);
   c.setOnClickListener(v->{cardRevealed=!cardRevealed;face.setText(cardRevealed?back:front);});add(body,c,20);
   EditText in=new EditText(this);in.setSingleLine(true);in.setHint(cardRuToDe?"Артикль + существительное":"Русский перевод");in.setBackground(shape(WHITE,15));
   in.setPadding(d(16),d(16),d(16),d(16));add(body,in,13);
   TextView feedback=small("");add(body,feedback,11);
   add(bottom,button("ДАЛЕЕ →",true,()->{
      if(in.getTag()!=null)return;
      in.setTag("checked");
      boolean ok=LearningEngine.acceptedAnswer(back,in.getText().toString());
      store.record("word_"+index,ok);
      if(ok&&!prefs.getBoolean("known_"+index,false)){prefs.edit().putBoolean("known_"+index,true).apply();store.markWord();}
      if(!ok)weakCards.add(index);
      feedback.setText(ok?"✓ Правильно! "+back:"Нужно запомнить: "+back);
      face.setText(back);
      in.setEnabled(false);
      final LinearLayout liveRoot=root;
      new Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(root==liveRoot){cardIndex++;cardRevealed=false;flashcard();}},ok?1000:1900);
   }),0);
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
   final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
   final int ORANGE=0xffff922c, BLACK=0xff162b27, CREAM=0xfffff1d5, PINK=0xfff47175;
   int mood;public MascotView(Context c,int m){super(c);mood=m;}
   void oval(Canvas c,int color,float l,float t,float r,float b){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawOval(l,t,r,b,paint);}
   void path(Canvas c,int color,float... coords){
     Path path=new Path();path.moveTo(coords[0],coords[1]);for(int i=2;i<coords.length;i+=2)path.lineTo(coords[i],coords[i+1]);
     path.close();paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(path,paint);
   }
   void smile(Canvas c){paint.setColor(BLACK);paint.setStrokeWidth(4);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
     c.drawArc(49,92,92,125,5,165,false,paint);paint.setStyle(Paint.Style.FILL);}
   protected void onDraw(Canvas cv){
     super.onDraw(cv);cv.save();cv.scale(getWidth()/140f,getHeight()/170f);
     oval(cv,0xffe3ad67,17,144,126,159);
     oval(cv,BLACK,10,19,48,63);oval(cv,BLACK,89,19,130,63);
     oval(cv,CREAM,17,27,45,56);oval(cv,CREAM,95,27,123,56);
     oval(cv,BLACK,15,33,127,142);
     oval(cv,ORANGE,20,35,122,139);
     oval(cv,CREAM,24,79,58,120);oval(cv,CREAM,83,79,118,120);
     oval(cv,CREAM,49,100,93,136);
     path(cv,BLACK,26,64,43,72,43,79,27,75);
     path(cv,BLACK,116,64,101,72,100,79,116,75);
     path(cv,BLACK,42,41,57,49,62,60,47,58);
     path(cv,BLACK,99,41,83,49,79,60,94,58);
     path(cv,BLACK,65,37,75,37,79,59,70,50,61,58);
     oval(cv,WHITE,37,67,66,102);oval(cv,WHITE,75,67,106,102);
     oval(cv,BLACK,49,73,64,96);oval(cv,BLACK,79,73,94,96);
     oval(cv,WHITE,51,75,56,82);oval(cv,WHITE,81,75,86,82);
     oval(cv,PINK,57,95,83,113);
     path(cv,BLACK,58,98,82,98,71,109);
     smile(cv);
     oval(cv,BLACK,14,126,56,159);oval(cv,BLACK,85,126,128,159);
     oval(cv,ORANGE,19,126,53,154);oval(cv,ORANGE,89,126,124,154);
     path(cv,BLACK,31,132,38,137,37,145,28,141);
     path(cv,BLACK,111,132,104,137,105,145,114,141);
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