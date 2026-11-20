package com.lexi26.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    DB db; LinearLayout root, content; String tab="학습"; ArrayList<Word> queue=new ArrayList<>(); int pos=0; boolean flipped=false;
    int seconds=26; TextView timer, activeCard; Handler handler=new Handler(); Runnable tick; TextToSpeech tts;
    static final int REQ_EXPORT=71, REQ_IMPORT=72;
    static class Word { long id; String language,word,meaning,example; int level,repetitions; long interval,due; double ease;
        Word(long i,String l,String w,String m,String e,int v,int r,long in,long d,double ef){id=i;language=l;word=w;meaning=m;example=e;level=v;repetitions=r;interval=in;due=d;ease=ef;} }
    public void onCreate(Bundle b){super.onCreate(b);db=new DB(this);tts=new TextToSpeech(this,status->{});show();}
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xff20243a);return t;}
    Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
    void show(){root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(12));root.setBackgroundColor(0xfff7f7fb);setContentView(root);
      TextView title=text("Lexi26",28);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xff5146d8);root.addView(title);
      root.addView(text("다국어 단어 암기 · 오프라인 학습",13));
      LinearLayout nav=new LinearLayout(this);for(String s:new String[]{"학습","단어장","목표","추가","통계"})nav.addView(button(s,()->{tab=s;show();}),new LinearLayout.LayoutParams(0,dp(52),1));root.addView(nav);
      ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);content.setPadding(0,dp(14),0,dp(14));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
      if(tab.equals("학습"))study();else if(tab.equals("추가"))add();else if(tab.equals("단어장"))list();else if(tab.equals("목표"))goals();else stats();
    }
    void study(){
      content.addView(text("전체 "+db.count()+"개   ·   암기 완료 "+db.known()+"개",16));
      Spinner filter=new Spinner(this);ArrayList<String> opts=new ArrayList<>();opts.add("전체");opts.addAll(Arrays.asList(db.languages()));filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));content.addView(filter);
      TextView card=text("학습을 시작해 보세요",25);activeCard=card;card.setGravity(17);card.setTypeface(null,Typeface.BOLD);card.setTextColor(0xff29254f);card.setBackgroundColor(0xffffffff);card.setPadding(dp(16),dp(38),dp(16),dp(38));LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(-1,dp(230));cardLp.setMargins(0,dp(12),0,dp(12));content.addView(card,cardLp);
      timer=text("26초 스키밍",16);timer.setGravity(17);content.addView(timer);
      content.addView(text("오늘 복습할 단어: "+db.dueCount(filter.getSelectedItem().toString())+"개",16));
      content.addView(button("26초 자동 스키밍 시작",()->{queue=db.words(filter.getSelectedItem().toString(),false);pos=0;flipped=false;seconds=26;renderCard(card);startTimer();}));
      content.addView(button("플래시카드 시작",()->{queue=db.words(filter.getSelectedItem().toString(),false);pos=0;flipped=false;stopTimer();renderCard(card);}));
      content.addView(button("오늘 복습 시작",()->{queue=db.dueWords(filter.getSelectedItem().toString());pos=0;flipped=false;stopTimer();renderCard(card);}));
      content.addView(button("뜻 보기 / 가리기",()->{flipped=!flipped;card.animate().rotationY(80f).setDuration(100).withEndAction(()->{renderCard(card);card.setRotationY(-80f);card.animate().rotationY(0f).setDuration(130).start();}).start();}));
      content.addView(button("발음 듣기",()->speakCurrent()));
      content.addView(button("다음 단어 건너뛰기",()->{if(pos<queue.size()){pos++;flipped=false;renderCard(card);}}));
      content.addView(text("기억 정도를 선택하면 다음 복습일이 자동으로 정해집니다.",13));
      LinearLayout row=new LinearLayout(this);for(String a:new String[]{"다시","어려움","알겠음","쉬움"})row.addView(button(a,()->answer(a,card)),new LinearLayout.LayoutParams(0,dp(58),1));content.addView(row);
      content.addView(button("취약 단어 복습",()->{queue=db.words(filter.getSelectedItem().toString(),true);pos=0;flipped=false;stopTimer();renderCard(card);}));
    }
    void renderCard(TextView c){
      String next;
      if(queue.size()==0) next="해당 단어가 없습니다.\n'추가' 탭에서 단어를 등록하세요.";
      else if(pos>=queue.size()){next="학습 완료!\n수고했어요.";stopTimer();}
      else {Word w=queue.get(pos);next=w.language+"  ·  "+(pos+1)+"/"+queue.size()+"\n\n"+w.word+(flipped?"\n\n"+w.meaning+(w.example.isEmpty()?"":"\n"+w.example):"\n\n뜻을 떠올려 보세요");}
      c.animate().cancel();c.setAlpha(0.25f);c.setTranslationY(dp(9));c.setText(next);c.animate().alpha(1f).translationY(0f).setDuration(210).start();
    }
    void speakCurrent(){if(queue.size()==0||pos>=queue.size()){toast("먼저 학습을 시작하세요.");return;}Word w=queue.get(pos);if(tts!=null){tts.setLanguage(locale(w.language));tts.speak(w.word,TextToSpeech.QUEUE_FLUSH,null,"lexi26");}}
    Locale locale(String l){switch(l){case "영어":return Locale.US;case "프랑스어":return Locale.FRENCH;case "일본어":return Locale.JAPANESE;case "독일어":return Locale.GERMAN;case "스페인어":return new Locale("es");case "이탈리아어":return Locale.ITALIAN;case "러시아어":return new Locale("ru");case "중국어":return Locale.CHINESE;case "한국어":return Locale.KOREAN;default:return Locale.getDefault();}}
    void answer(String a,TextView c){if(queue.size()==0||pos>=queue.size())return;Word w=queue.get(pos);int quality=a.equals("다시")?1:(a.equals("어려움")?3:(a.equals("알겠음")?4:5));db.review(w,quality);pos++;flipped=false;renderCard(c);}
    void startTimer(){stopTimer();seconds=26;flipped=false;tick=new Runnable(){public void run(){if(seconds>0){timer.setText("다음 카드까지 "+seconds+"초");seconds--;handler.postDelayed(this,1000);}else{flipped=true;if(pos<queue.size()){Word w=queue.get(pos);timer.setText("뜻: "+w.meaning+"  ·  3초 뒤 다음 단어");}renderActiveCard();tick=new Runnable(){public void run(){pos++;flipped=false;if(pos>=queue.size()){timer.setText("자동 스키밍 완료");stopTimer();return;}renderActiveCard();seconds=26;startTimer();}};handler.postDelayed(tick,3000);}}};timer.setText("첫 단어 · 26초");handler.postDelayed(tick,1000);}
    void renderActiveCard(){if(activeCard!=null)renderCard(activeCard);}
    void stopTimer(){if(tick!=null)handler.removeCallbacks(tick);tick=null;}

    void goals(){
      content.addView(text("학습 목표 설정",22));
      content.addView(text("학습 언어와 목표 수준을 선택하세요. 기본 어휘 팩은 오프라인으로 제공됩니다.",14));
      String[] langs={"영어","프랑스어"}; Spinner lang=new Spinner(this);
      lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,langs));
      String saved=getPreferences(0).getString("goal_language","프랑스어"); lang.setSelection(saved.equals("영어")?0:1);
      content.addView(text("학습 언어",16)); content.addView(lang);
      String[] levels={"A1","A2","B1","B2","C1","C2"};
      Spinner current=new Spinner(this); current.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,levels));
      content.addView(text("현재 수준",16)); content.addView(current);
      Spinner target=new Spinner(this); target.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,levels)); target.setSelection(2);
      content.addView(text("목표 수준",16)); content.addView(target);
      EditText days=field("목표까지 남은 일수 (예: 180)"); days.setInputType(2); days.setText(getPreferences(0).getString("goal_days","180"));
      EditText daily=field("하루 신규 단어 목표 (예: 10)"); daily.setInputType(2); daily.setText(getPreferences(0).getString("daily_new","10"));
      content.addView(button("목표 저장",()->{
        String d=val(days),n=val(daily);
        if(d.isEmpty()||n.isEmpty()){toast("일수와 일일 단어 수를 입력하세요.");return;}
        try { if(Integer.parseInt(d)<1||Integer.parseInt(n)<1) throw new NumberFormatException(); }
        catch(Exception ex){toast("일수와 일일 단어 수는 1 이상이어야 합니다.");return;}
        getPreferences(0).edit().putString("goal_language",lang.getSelectedItem().toString()).putString("goal_current",current.getSelectedItem().toString()).putString("goal_target",target.getSelectedItem().toString()).putString("goal_days",d).putString("daily_new",n).apply();
        toast("학습 목표를 저장했습니다.");show();
      }));
      String gl=getPreferences(0).getString("goal_language","프랑스어");
      int total=db.words(gl,false).size();
      int daysLeft=Integer.parseInt(getPreferences(0).getString("goal_days","180"));
      int perDay=Integer.parseInt(getPreferences(0).getString("daily_new","10"));
      content.addView(text("현재 "+gl+" 기본 어휘 팩: "+total+"개",17));
      content.addView(text("계획 기간 "+daysLeft+"일 · 하루 신규 "+perDay+"개 · 기간 내 학습 가능량 "+(daysLeft*perDay)+"개",15));
      content.addView(text("주의: A1~C2 선택은 학습 목표 기록용이며, 기본 팩은 공식 CEFR 전체 어휘 목록이 아닌 자체 구성 입문 어휘입니다.",13));
    }

    void add(){content.addView(text("새 단어 등록",21));EditText language=field("언어 (예: 영어, 프랑스어, 일본어)");EditText word=field("단어 / 표현");EditText meaning=field("뜻");EditText example=field("예문 (선택)");
      content.addView(button("저장하기",()->{String l=val(language),w=val(word),m=val(meaning);if(l.isEmpty()||w.isEmpty()||m.isEmpty()){toast("언어, 단어, 뜻을 입력하세요.");return;}db.add(l,w,m,val(example));toast("단어를 저장했습니다.");tab="단어장";show();}));
      content.addView(button("예시 단어 12개 추가",()->{String[][] s={{"영어","ephemeral","덧없는, 일시적인"},{"영어","ubiquitous","어디에나 있는"},{"영어","ambiguous","모호한"},{"영어","resilient","회복력 있는"},{"프랑스어","éphémère","덧없는"},{"프랑스어","pourtant","그럼에도 불구하고"},{"프랑스어","néanmoins","그럼에도"},{"프랑스어","épanouir","꽃피우다"},{"일본어","曖昧","애매함, 모호함"},{"일본어","懐かしい","그리운"},{"일본어","穏やか","온화한"},{"일본어","努力","노력"}};for(String[] x:s)db.add(x[0],x[1],x[2],"");toast("예시 단어를 추가했습니다.");show();}));
    }
    EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);content.addView(e);return e;}
    String val(EditText e){return e.getText().toString().trim();}
    void list(){content.addView(text("저장된 단어",21));for(Word w:db.words("전체",false)){TextView t=text(w.language+" · "+w.word+"\n"+w.meaning+"\n숙련도 "+w.level+"/5 · 다음 복습: "+(w.due<=System.currentTimeMillis()?"지금":new Date(w.due).toLocaleString()),16);t.setPadding(0,dp(10),0,dp(10));content.addView(t);}}
    void stats(){content.addView(text("학습 통계",22));content.addView(text("전체 단어: "+db.count(),18));content.addView(text("암기 완료: "+db.known(),18));content.addView(text("누적 복습: "+db.reviewCount(),18));content.addView(text("언어별 단어 수",18));for(String l:db.languages()){int n=db.words(l,false).size();TextView t=text(l+"  "+n+"개",15);t.setPadding(0,dp(8),0,dp(8));content.addView(t);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,db.count()));bar.setProgress(n);content.addView(bar);}
      content.addView(button("CSV 내보내기",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"lexi26-words.csv");startActivityForResult(i,REQ_EXPORT);}));
      content.addView(button("CSV 가져오기",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,REQ_IMPORT);}));
      content.addView(text("CSV 형식: language,word,meaning,example (UTF-8)",12));
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{if(request==REQ_EXPORT){OutputStream out=getContentResolver().openOutputStream(uri);out.write(db.csv().getBytes(StandardCharsets.UTF_8));out.close();toast("CSV 파일을 저장했습니다.");}else if(request==REQ_IMPORT){InputStream in=getContentResolver().openInputStream(uri);BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));String line;int n=0;boolean first=true;while((line=br.readLine())!=null){if(first&&line.toLowerCase().startsWith("language,")){first=false;continue;}first=false;List<String> cols=parseCsv(line);if(cols.size()>=3){db.add(cols.get(0),cols.get(1),cols.get(2),cols.size()>3?cols.get(3):"");n++;}}br.close();toast(n+"개 단어를 가져왔습니다.");tab="단어장";show();}}catch(Exception e){toast("파일 처리 실패: "+e.getMessage());}}
    List<String> parseCsv(String line){ArrayList<String>a=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(q&&i+1<line.length()&&line.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if(c==','&&!q){a.add(b.toString());b.setLength(0);}else b.append(c);}a.add(b.toString());return a;}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){stopTimer();if(tts!=null)tts.shutdown();super.onDestroy();}
    static class DB extends SQLiteOpenHelper{

      DB(Context c){super(c,"lexi26.db",null,3);}
      @Override public void onOpen(SQLiteDatabase d){
        super.onOpen(d);
        Cursor x=d.rawQuery("SELECT COUNT(*) FROM words",null); x.moveToFirst(); int n=x.getInt(0); x.close();
        if(n==0) seed(d);
      }
      void seed(SQLiteDatabase d){
        String[][] en={
          {"hello","안녕하세요"},{"goodbye","안녕히 가세요"},{"please","부디, 제발"},{"thank you","감사합니다"},{"yes","네"},{"no","아니요"},{"water","물"},{"food","음식"},{"bread","빵"},{"house","집"},
          {"school","학교"},{"book","책"},{"friend","친구"},{"family","가족"},{"mother","어머니"},{"father","아버지"},{"day","날"},{"night","밤"},{"today","오늘"},{"tomorrow","내일"},
          {"to be","~이다, 있다"},{"to have","가지다"},{"to go","가다"},{"to come","오다"},{"to eat","먹다"},{"to drink","마시다"},{"to speak","말하다"},{"to read","읽다"},{"to write","쓰다"},{"to learn","배우다"},
          {"big","큰"},{"small","작은"},{"good","좋은"},{"bad","나쁜"},{"beautiful","아름다운"},{"happy","행복한"},{"sad","슬픈"},{"fast","빠른"},{"slow","느린"},{"important","중요한"}
        };
        String[][] fr={
          {"bonjour","안녕하세요"},{"au revoir","안녕히 가세요"},{"merci","감사합니다"},{"s'il vous plaît","부탁합니다"},{"oui","네"},{"non","아니요"},{"eau","물"},{"nourriture","음식"},{"pain","빵"},{"maison","집"},
          {"école","학교"},{"livre","책"},{"ami","친구"},{"famille","가족"},{"mère","어머니"},{"père","아버지"},{"jour","날"},{"nuit","밤"},{"aujourd’hui","오늘"},{"demain","내일"},
          {"être","~이다, 있다"},{"avoir","가지다"},{"aller","가다"},{"venir","오다"},{"manger","먹다"},{"boire","마시다"},{"parler","말하다"},{"lire","읽다"},{"écrire","쓰다"},{"apprendre","배우다"},
          {"grand","큰"},{"petit","작은"},{"bon","좋은"},{"mauvais","나쁜"},{"beau","아름다운"},{"heureux","행복한"},{"triste","슬픈"},{"rapide","빠른"},{"lent","느린"},{"important","중요한"}
        };
        for(String[] a:en) seedOne(d,"영어",a[0],a[1]);
        for(String[] a:fr) seedOne(d,"프랑스어",a[0],a[1]);
      }
      void seedOne(SQLiteDatabase d,String l,String w,String m){
        ContentValues v=new ContentValues(); v.put("language",l); v.put("word",w); v.put("meaning",m); v.put("example",""); v.put("level",0); v.put("due",System.currentTimeMillis()); d.insert("words",null,v);
      }

      public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE words(id INTEGER PRIMARY KEY AUTOINCREMENT,language TEXT,word TEXT,meaning TEXT,example TEXT,level INTEGER DEFAULT 0,repetitions INTEGER DEFAULT 0,interval_days INTEGER DEFAULT 0,due INTEGER DEFAULT 0,ease REAL DEFAULT 2.5)");d.execSQL("CREATE TABLE reviews(id INTEGER PRIMARY KEY AUTOINCREMENT,word_id INTEGER,reviewed_at INTEGER)");}
      public void onUpgrade(SQLiteDatabase d,int o,int n){if(o<2){d.execSQL("ALTER TABLE words ADD COLUMN repetitions INTEGER DEFAULT 0");d.execSQL("ALTER TABLE words ADD COLUMN interval_days INTEGER DEFAULT 0");d.execSQL("ALTER TABLE words ADD COLUMN due INTEGER DEFAULT 0");d.execSQL("ALTER TABLE words ADD COLUMN ease REAL DEFAULT 2.5");d.execSQL("CREATE TABLE IF NOT EXISTS reviews(id INTEGER PRIMARY KEY AUTOINCREMENT,word_id INTEGER,reviewed_at INTEGER)");}}
      void add(String l,String w,String m,String e){ContentValues v=new ContentValues();v.put("language",l);v.put("word",w);v.put("meaning",m);v.put("example",e);v.put("level",0);v.put("due",System.currentTimeMillis());getWritableDatabase().insert("words",null,v);}
      int count(){return scalar("SELECT COUNT(*) FROM words");}int known(){return scalar("SELECT COUNT(*) FROM words WHERE level>=4");}int reviewCount(){return scalar("SELECT COUNT(*) FROM reviews");}
      int dueCount(String l){Cursor c;if(l.equals("전체"))c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM words WHERE due<=?",new String[]{""+System.currentTimeMillis()});else c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM words WHERE due<=? AND language=?",new String[]{""+System.currentTimeMillis(),l});c.moveToFirst();int n=c.getInt(0);c.close();return n;}
      int scalar(String q){Cursor c=getReadableDatabase().rawQuery(q,null);c.moveToFirst();int n=c.getInt(0);c.close();return n;}
      String[] languages(){Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT language FROM words ORDER BY language",null);ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(c.getString(0));c.close();return a.toArray(new String[0]);}
      ArrayList<Word> words(String l,boolean weak){String q="SELECT id,language,word,meaning,example,level,repetitions,interval_days,due,ease FROM words"+(l.equals("전체")?"":" WHERE language=?")+(weak?(l.equals("전체")?" WHERE ":" AND ")+"level<3":"")+" ORDER BY level ASC,id DESC";return query(q,l.equals("전체")?null:new String[]{l});}
      ArrayList<Word> dueWords(String l){String q="SELECT id,language,word,meaning,example,level,repetitions,interval_days,due,ease FROM words WHERE due<=?"+(l.equals("전체")?"":" AND language=?")+" ORDER BY due";return query(q,l.equals("전체")?new String[]{""+System.currentTimeMillis()}:new String[]{""+System.currentTimeMillis(),l});}
      ArrayList<Word> query(String q,String[] args){Cursor c=getReadableDatabase().rawQuery(q,args);ArrayList<Word>a=new ArrayList<>();while(c.moveToNext())a.add(new Word(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getInt(5),c.getInt(6),c.getLong(7),c.getLong(8),c.getDouble(9)));c.close();return a;}
      void review(Word w,int quality){SQLiteDatabase d=getWritableDatabase();int reps=w.repetitions;long interval=w.interval;double ease=w.ease;if(quality<3){reps=0;interval=1;}else{if(reps==0)interval=1;else if(reps==1)interval=6;else interval=Math.max(1,Math.round(interval*ease));reps++;ease=Math.max(1.3,ease+(0.1-(5-quality)*(0.08+(5-quality)*0.02)));if(quality==3)interval=Math.max(1,Math.round(interval*0.7));}int level=quality<3?0:Math.min(5,w.level+1);ContentValues v=new ContentValues();v.put("repetitions",reps);v.put("interval_days",interval);v.put("ease",ease);v.put("level",level);v.put("due",System.currentTimeMillis()+interval*86400000L);d.update("words",v,"id=?",new String[]{""+w.id});ContentValues r=new ContentValues();r.put("word_id",w.id);r.put("reviewed_at",System.currentTimeMillis());d.insert("reviews",null,r);}
      String csv(){StringBuilder b=new StringBuilder("language,word,meaning,example\n");Cursor c=getReadableDatabase().rawQuery("SELECT language,word,meaning,example FROM words",null);while(c.moveToNext()){for(int i=0;i<4;i++){if(i>0)b.append(',');String s=c.getString(i)==null?"":c.getString(i);b.append('"').append(s.replace("\"","\"\"")).append('"');}b.append("\n");}c.close();return b.toString();}
    }
}