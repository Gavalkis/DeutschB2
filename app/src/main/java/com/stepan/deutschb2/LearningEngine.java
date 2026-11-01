package com.stepan.deutschb2;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Pure quiz validation, spaced repetition and local progress. No UI dependencies. */
public final class LearningEngine {
    private LearningEngine(){}
    public static String normalize(String s) {
        if(s==null)return "";
        return Normalizer.normalize(s,Normalizer.Form.NFC).toLowerCase(Locale.GERMAN)
          .replaceAll("[.,!?;:„“\"'«»()\\[\\]]","").replaceAll("\\s+"," ").trim();
    }
    public static int distance(String a,String b){
        a=normalize(a);b=normalize(b);int[] old=new int[b.length()+1],cur=new int[b.length()+1];
        for(int j=0;j<=b.length();j++)old[j]=j;
        for(int i=1;i<=a.length();i++){cur[0]=i;for(int j=1;j<=b.length();j++)
            cur[j]=Math.min(Math.min(cur[j-1]+1,old[j]+1),old[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
            int[] x=old;old=cur;cur=x;
        }return old[b.length()];
    }
    public static double similarity(String a,String b){
        a=normalize(a);b=normalize(b);int max=Math.max(a.length(),b.length());
        return max==0?1:Math.max(0,1.0-(double)distance(a,b)/max);
    }
    public static boolean acceptedSpeech(String reference,String recognized){return similarity(reference,recognized)>=0.70;}
    public static boolean acceptedAnswer(String reference,String given){return normalize(reference).equals(normalize(given));}
    public static boolean capitalizationOnly(String reference,String given){
        return acceptedAnswer(reference,given)&&!reference.trim().equals(given.trim());
    }
    public static String[] topicForms(int topic,String correct) {
        String c=correct.trim(),low=c.toLowerCase(Locale.GERMAN);
        String[] forms;
        if(topic==0||topic==1||topic==2||topic==3||topic==5||topic==23)
            forms=new String[]{"der","die","das","den","dem","ein","eine","einen","einem","einer","kein","keine","keinen","keinem"};
        else if(topic==4)forms=new String[]{"ich","mich","mir","du","dich","dir","er","ihn","ihm","sie","ihr","wir","uns","euch","ihnen"};
        else if(topic==9)forms=new String[]{"kann","kannst","muss","musst","will","möchte","können","müssen","studieren","arbeiten","gehen"};
        else if(topic>=10&&topic<=12)forms=new String[]{"auf","an","ein","mit","um","gelernt","gesehen","gegangen","gefahren","hat","ist","war","hatte","musste"};
        else forms=new String[]{"der","die","das","den","dem","einen","einem","einer","bin","ist","hat","habe","haben","lernen","gelernt","nicht","kein","weil","dass","wenn","wird","werden"};
        LinkedHashSet<String> result=new LinkedHashSet<>();result.add(c);
        for(String option:forms){if(result.size()>=4)break;
            if(!option.equalsIgnoreCase(c))result.add(option);
        }
        return result.toArray(new String[0]);
    }
    public static final long[] SRS_MINUTES={10,30,60,180,360,1440,4320,14400,28800,43200,86400};
    public static class Store{
        private final SharedPreferences sp;
        public Store(Context ctx){sp=ctx.getSharedPreferences("course_v5",Context.MODE_PRIVATE);}
        public int unlocked(){return sp.getInt("unlocked",0);}
        public boolean completed(int lesson){return sp.getBoolean("complete_"+lesson,false);}
        public void complete(int lesson){sp.edit().putBoolean("complete_"+lesson,true).putInt("unlocked",Math.max(lesson+1,unlocked())).apply();}
        public void record(String key,boolean correct){
            String prefix="review_"+key+"_";
            int step=correct?Math.min(SRS_MINUTES.length-1,sp.getInt(prefix+"stage",-1)+1):0;
            long next=System.currentTimeMillis()+SRS_MINUTES[step]*60000L;
            sp.edit().putInt(prefix+"stage",step).putLong(prefix+"due",next)
             .putInt("answers",sp.getInt("answers",0)+1).putInt("right",sp.getInt("right",0)+(correct?1:0))
             .putInt(prefix+"errors",sp.getInt(prefix+"errors",0)+(correct?0:1)).apply();
        }
        public long due(String key){return sp.getLong("review_"+key+"_due",0);}
        public int errors(String key){return sp.getInt("review_"+key+"_errors",0);}
        public int dueCount(){
            long now=System.currentTimeMillis();int n=0;
            for(String key:sp.getAll().keySet())if(key.startsWith("review_")&&key.endsWith("_due")&&sp.getLong(key,0)<=now)n++;
            return n;
        }
        public int answers(){return sp.getInt("answers",0);}
        public int right(){return sp.getInt("right",0);}
        public long minutes(){return sp.getLong("total_ms",0)/60000;}
        public int words(){return sp.getInt("known_words",0);}
        public void markWord(){sp.edit().putInt("known_words",words()+1).apply();}
        public void addSession(long millis){sp.edit().putLong("total_ms",sp.getLong("total_ms",0)+Math.max(0,millis)).apply();}
        public int streak(){return sp.getInt("streak",0);}
        public void touchToday(){
            Calendar cal=Calendar.getInstance();
            int today=(int)(cal.getTimeInMillis()/86400000L);
            int last=sp.getInt("last_day",-10);
            if(last!=today){
                int streak=last==today-1?sp.getInt("streak",0)+1:1;
                sp.edit().putInt("streak",streak).putInt("last_day",today).apply();
            }
        }
        public int lastDay(){return sp.getInt("last_day",-10);}
        public String essay(int n){return sp.getString("essay_"+n,"");}
        public void saveEssay(int n,String text){sp.edit().putString("essay_"+n,text).apply();}
    }
}
