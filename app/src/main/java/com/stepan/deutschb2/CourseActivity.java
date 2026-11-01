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
 LinearLayout root,body,bottom; ScrollView scroll; TextToSpeech tts; boolean voiceReady=false; String speaking="";
 JSONArray guides,lessons,bank; android.content.SharedPreferences prefs;
 int page=0,unit=0,stage=0,qindex=0,score=0; boolean exam=false,answered=false,repairMode=false,fromSpeech=false; String chosen="",expected="",expectedRu="";
 LearningEngine.Store store; ArrayList<Integer> wrong=new ArrayList<>(); ArrayList<Integer> repairs=new ArrayList<>(); long sessionStarted; int lastSpeechIndex=-1;
 ArrayList<Integer> questionOrder=new ArrayList<>(); final Random random=new Random(19);
 int[] intervals={10,30,60,180,360,1440,4320,14400,28800,43200,86400}; // minutes
 String[] tabs={"Дом","Уроки","Слова","Практика","Камера"};
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
 prefs=getSharedPreferences("course_v4",MODE_PRIVATE);store=new LearningEngine.Store(this);store.touchToday();sessionStarted=System.currentTimeMillis();tts=new TextToSpeech(this,this);
 try{guides=new JSONArray(asset("lesson_guides.json"));lessons=new JSONArray(asset("lessons.json"));bank=new JSONArray(asset("practice_bank.json"));}catch(Exception ex){Toast.makeText(this,ex.toString(),1).show();}
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
 void home(){header("MEIN DEUTSCH","Сегодня твой день","От теории к живой речи. Каждый урок открывает следующий шаг.");
 LinearLayout hero=card();hero.setBackground(shape(GREEN,24));TextView a=text("DEUTSCH A1 → B2",15,true);a.setTextColor(WHITE);add(hero,a,12);
 TextView b=text("Урок "+(Math.min(done()+1,23))+" из 23",28,true);b.setTextColor(WHITE);add(hero,b,8);TextView c=text("По одной небольшой теме. Потом практика и повторение.",15,false);c.setTextColor(0xffdfefe6);add(hero,c,18);
 add(hero,button("ПРОДОЛЖИТЬ →",false,()->openLesson(Math.min(done(),22))),0);add(body,hero,20);
 LinearLayout today=card();add(today,title("Твоя практика",21),15);
 for(String s:new String[]{"◷  Повторение слов и правил","♫  Слушание и диктант","✎  Построение предложений","◎  Произношение вслух"})add(today,text(s,16,false),15);
 add(today,button("ТРЕНИРОВАТЬСЯ",true,()->draw(3)),0);add(body,today,18);
 LinearLayout tip=card();add(tip,title("Правило обучения",20),10);add(tip,text("Сначала разбираешь структуру предложения и новые слова с переводом. Затем строишь предложения самостоятельно. Ошибки возвращаются в следующих занятиях.",15,false),0);add(body,tip,12);
 }
 String level(int i){return i<9?"A1":i<18?"A2":i<21?"B1":"B2";}
 void path(){header("КУРС","Маршрут A1 → B2","23 раздела грамматики, теория частями и упражнения после каждой части. Учебный контент постепенно расширяется.");
 for(int i=0;i<23;i++){final int idx=i;try{JSONObject l=lessons.getJSONObject(i);LinearLayout c=card();boolean open=i<=done();c.setAlpha(open?1:.48f);
 TextView lab=small(level(i)+"  ·  УРОК "+(i+1)+(prefs.getBoolean("complete_"+i,false)?"   ✓ ПРОЙДЕН":""));add(c,lab,8);add(c,title(l.getString("title"),19),9);
 add(c,small(open?"Теория → примеры → практика":"Откроется после предыдущего урока"),12);
 if(open)add(c,button(prefs.getBoolean("complete_"+i,false)?"ПОВТОРИТЬ":"НАЧАТЬ",true,()->openLesson(idx)),0);add(body,c,13);
 if(i==8||i==17||i==20||i==22){final int bound=i;add(body,button("◈ ПРОВЕРКА УРОВНЯ "+level(i),false,()->startExam(bound)),17);}
 }catch(Exception ignored){}}
 }
 void openLesson(int i){unit=i;stage=0;showTheory();overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);}
 void showTheory(){layout(false);try{JSONObject g=guides.getJSONObject(unit),l=lessons.getJSONObject(unit);
 header(level(unit)+" · УРОК "+(unit+1),"Разберём правило","Шаг "+(stage+1)+" из 3  •  Сначала пойми логику.");
 LinearLayout c=card();
 if(stage==0){add(c,title(l.getString("title"),24),13);add(c,text(g.getString("why"),16,false),14);
 TextView f=text(g.getString("formula"),16,true);f.setPadding(d(12),d(14),d(12),d(14));f.setBackground(shape(LIGHT,12));add(c,f,10);}
 if(stage==1){add(c,title("Формы и окончания",22),14);
 JSONArray tab=g.getJSONArray("table");for(int j=0;j<tab.length();j++){JSONArray row=tab.getJSONArray(j);LinearLayout rr=horizontal();rr.setBackground(shape(j%2==0?LIGHT:BG,7));for(int k=0;k<row.length();k++){TextView cell=text(row.getString(k),row.length()>3?12:14,j==0);cell.setPadding(d(5),d(12),d(3),d(12));rr.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}add(c,rr,3);}
 add(c,small(g.getString("tip")),3);}
 if(stage==2){add(c,title("Как это звучит",22),12);JSONArray ex=l.getJSONArray("examples");for(int j=0;j<ex.length();j++){String[] pair=ex.getString(j).split("\\|",2);final int k=j;LinearLayout line=vertical();line.setBackground(shape(BG,12));pad(line,12);
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
 ArrayList<String> opts=new ArrayList<>(Arrays.asList(LearningEngine.topicForms(unit,target)));Collections.shuffle(opts);choiceList(opts,target);}
 else if(type==2){TextView line=text(ru,18,true);line.setTextColor(WHITE);add(body,line,16);wordBank(de);}
 else if(type==3){add(body,button("🔊  ПРОСЛУШАТЬ",false,()->talk("listen"+qindex,de)),22);inputQuestion("Напиши услышанное предложение",de);}
 else if(type==5){TextView line=text(ru,18,true);line.setTextColor(WHITE);add(body,line,16);inputQuestion("Переведи на немецкий",de);}
 else if(type==6){addGermanPrompt(de,16);add(body,button("🔊  СЛУШАТЬ",false,()->talk("say"+qindex,de)),14);add(body,button("🎙  ПРОИЗНЕСТИ",false,()->recognize(de)),12);
 add(body,smallLight("После распознавания приложение сравнит текст с эталоном. Порог: 70%."),0);}
 else {String erroneous=makeError(de,focus);addGermanPrompt(erroneous,12);inputQuestion("Исправь ошибку в предложении",de);}
 }catch(Exception e){Toast.makeText(this,e.toString(),1).show();}
 }
 TextView titleLight(String s){TextView t=title(s,22);t.setTextColor(WHITE);return t;}
 TextView smallLight(String s){TextView t=small(s);t.setTextColor(0xffb7ccc5);return t;}
 void choiceList(ArrayList<String> options,String answer){LinearLayout list=vertical();for(String x:options){TextView b=text(x,17,false);b.setPadding(d(18),d(20),d(14),d(20));b.setBackground(shape(0xff263e39,16));b.setTextColor(WHITE);add(list,b,10);b.setOnClickListener(v->{chosen=x;for(int j=0;j<list.getChildCount();j++){View z=list.getChildAt(j);z.setBackground(shape(z==b?GREEN:0xff263e39,16));}buttonArea(answer);});}add(body,list,20);buttonArea(answer);}
 void buttonArea(String answer){bottom.removeAllViews();add(bottom,button("ПРОВЕРИТЬ",true,()->feedback(normal(chosen).equals(normal(answer)),answer)),0);}
 void wordBank(String de){ArrayList<String> parts=new ArrayList<>(Arrays.asList(de.replaceAll("[.!?]","").split(" ")));Collections.shuffle(parts);
 TextView output=titleLight("...");add(body,output,23);LinearLayout grid=vertical();ArrayList<String> built=new ArrayList<>();
 for(String word:parts){TextView choice=text(word,17,false);choice.setTextColor(WHITE);choice.setPadding(d(15),d(12),d(15),d(12));choice.setBackground(shape(0xff273f39,12));add(grid,choice,7);
 choice.setOnClickListener(v->{built.add(word);choice.setVisibility(View.GONE);chosen=String.join(" ",built);output.setText(chosen);});}
 add(body,grid,10);buttonArea(de);}
 void inputQuestion(String hint,String correct){EditText field=new EditText(this);field.setTextColor(WHITE);field.setHintTextColor(0xffa7b9b3);field.setHint(hint);field.setTextSize(18);field.setMinLines(2);field.setBackground(shape(0xff263e39,16));field.setPadding(d(18),d(15),d(18),d(15));add(body,field,20);
 bottom.removeAllViews();add(bottom,button("ПРОВЕРИТЬ",true,()->{chosen=field.getText().toString();feedback(normal(chosen).equals(normal(correct)),correct);}),0);}
 void feedback(boolean ok,String solution){if(answered)return;answered=true;if(ok)score++;else if(!repairMode)wrong.add(qindex);store.record("topic_"+unit+"_"+(repairMode?repairs.get(qindex):qindex),ok);
 bottom.removeAllViews();TextView msg=text((ok?"✓ Отлично!":"Правильный ответ:")+" \n"+solution+"\n"+expectedRu,16,true);msg.setTextColor(ok?GREEN:RED);msg.setPadding(d(12),d(10),d(12),d(12));msg.setBackground(shape(ok?LIGHT:0xffffeded,13));add(bottom,msg,10);
 add(bottom,button("ПРОДОЛЖИТЬ",true,()->next(ok)),0);
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
 void wordHint(String word){
   String basic=normal(word);HashMap<String,String> m=new HashMap<>();
   String[] pairs={"der=определённый артикль, муж. род","die=определённый артикль, жен. род/мн. число","das=определённый артикль, ср. род","ein=неопределённый артикль","eine=неопределённый артикль, жен. род","einen=неопределённый артикль, Akkusativ муж. рода","keine=нет, отрицание существительного","ich=я","du=ты","ist=есть, является","sind=являются","habe=имею","hat=имеет","brauche=нуждаюсь","wohnung=квартира (die)","universität=университет (die)","tisch=стол (der)","bahnhof=вокзал (der)","schwer=тяжёлый","alt=старый","groß=большой","wasser=вода (das)","student=студент (der)","auto=автомобиль (das)","lehrer=учитель (der)"};
   for(String p:pairs){String[] ab=p.split("=",2);m.put(ab[0],ab[1]);}
   new AlertDialog.Builder(this).setTitle(word).setMessage(m.getOrDefault(basic,"Полный перевод этого слова пока отсутствует в локальном словаре.")).setPositiveButton("OK",null).show();
 }
 void results(){if(!repairMode&&!wrong.isEmpty()){repairMode=true;repairs.addAll(wrong);qindex=0;showRepairIntro();return;}if(repairMode){for(int i=0;i<repairs.size();i++){} }layout(true);TextView trophy=text(score>=8?"★ Отличная работа!":"Урок завершён",28,true);trophy.setTextColor(GOLD);add(body,trophy,28);
 TextView points=text(score+" / "+questionOrder.size()+" правильных",25,true);points.setTextColor(WHITE);add(body,points,14);
 add(body,smallLight("Повторяй ошибочные формы. В следующих уроках будут снова встречаться знакомые слова."),20);
 boolean pass=wrong.isEmpty()||repairMode;if(pass&&!exam){store.complete(unit);prefs.edit().putBoolean("complete_"+unit,true).putInt("unlocked",Math.max(done(),unit+1)).apply();}
 add(bottom,button("НАЗАД К МАРШРУТУ",true,()->draw(1)),0);
 }
 void dictionary(){header("WÖRTER","Словарь","Слова с родом, множественным числом и переводом. Для повторения используй практику.");
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
 void practiceHub(){header("НАВЫКИ","Тренировка","Проверяй грамматику, лексику, аудирование и речь на материалах уже открытых тем.");
 String[] n={"Пробелы и окончания","Сборка предложений","Аудирование","Письмо и перевод","Произношение","Смешанное повторение"};
 for(String mode:n){LinearLayout c=card();add(c,title(mode,19),11);add(c,button("НАЧАТЬ",true,()->{if(mode.equals("Письмо и перевод"))writing();else startPractice(Math.min(done(),22),false);}),0);add(body,c,12);}
 }
 void camera(){header("WÖRTER UM MICH","Камера и перевод","Для распознавания предметов понадобится интернет и настроенный ключ облачного сервиса.");LinearLayout c=card();add(c,text("В этом выпуске модуль распознавания открывается отдельно, чтобы обучение и проверка ответов работали без интернета.",16,false),15);add(c,button("ОТКРЫТЬ КАМЕРУ",true,()->{Intent intent=new Intent(this,MainActivity.class);intent.putExtra("camera",true);startActivity(intent);}),0);add(body,c,12);}
 void startExam(int end){startPractice(end,true);}
 void recognize(String target){Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"de-DE");intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
 lastSpeechIndex=qindex;try{startActivityForResult(intent,63);}catch(Exception e){new AlertDialog.Builder(this).setMessage("Распознавание речи недоступно без установленной службы Android Speech.").setPositiveButton("OK",null).show();}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==63&&result==RESULT_OK&&data!=null){ArrayList<String> txt=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(txt!=null&&!txt.isEmpty()){String heard=txt.get(0);int pct=(int)Math.round(LearningEngine.similarity(expected,heard)*100);boolean ok=LearningEngine.acceptedSpeech(expected,heard);feedback(ok,expected);new AlertDialog.Builder(this).setTitle("Совпадение: "+pct+"%"+(ok?" ✓":"")).setMessage("Распознано: "+heard+"\nЭталон: "+expected+"\n"+(ok?"Порог 70% пройден.":"Попробуй ещё раз в конце урока.")).setPositiveButton("Понятно",null).show();}}}
 @Override protected void onPause(){stopAudio();super.onPause();}
 @Override protected void onDestroy(){stopAudio();if(tts!=null)tts.shutdown();super.onDestroy();}
}