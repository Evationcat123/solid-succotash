package com.circledayplanner.app;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements PlannerView.Listener {
    private PlannerStore store; private PlannerView planner; private Calendar selected=Calendar.getInstance(); private TextView dateText,progressText,nextText; private ActivityResultLauncher<String> notificationPermission;
    private final int[] palette={0xFF6750A4,0xFF00639A,0xFF2E7D5B,0xFFAA5D00,0xFFC53864,0xFF65558F,0xFF7A5962,0xFF3F6B8A};
    @Override protected void onCreate(Bundle b){super.onCreate(b); store=PlannerStore.get(this); createChannel(); requestNotif(); buildUi(); refresh();}
    private void requestNotif(){notificationPermission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{});if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);}
    private void createChannel(){NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("planner","Terminerinnerungen",NotificationManager.IMPORTANCE_HIGH));}
    private TextView tv(String s,float size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(resolve(android.R.attr.textColorPrimary));return t;}
    private int resolve(int attr){android.util.TypedValue v=new android.util.TypedValue();getTheme().resolveAttribute(attr,v,true);return v.data;}
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));
        MaterialToolbar bar=new MaterialToolbar(this);bar.setTitle("Circle Day Planner");bar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);bar.setNavigationOnClickListener(v->showCalendar());
        bar.inflateMenu(0);Menu m=bar.getMenu();MenuItem cal=m.add("Kalender");cal.setIcon(android.R.drawable.ic_menu_my_calendar);cal.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);cal.setOnMenuItemClickListener(x->{showCalendar();return true;});MenuItem set=m.add("Einstellungen");set.setIcon(android.R.drawable.ic_menu_preferences);set.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);set.setOnMenuItemClickListener(x->{startActivity(new Intent(this,SettingsActivity.class));return true;});root.addView(bar,new LinearLayout.LayoutParams(-1,dp(60)));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setOrientation(LinearLayout.VERTICAL);dateText=tv("",20);dateText.setTypeface(null,1);progressText=tv("",13);nextText=tv("",13);header.addView(dateText);header.addView(progressText);header.addView(nextText);header.setPadding(dp(4),dp(2),dp(4),dp(8));root.addView(header);
        planner=new PlannerView(this);planner.setListener(this);root.addView(planner,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);actions.setPadding(0,dp(8),0,0);
        MaterialButton today=new MaterialButton(this);today.setText("Heute");today.setOnClickListener(v->{selected=Calendar.getInstance();refresh();});actions.addView(today,new LinearLayout.LayoutParams(0,dp(52),1));
        Space sp=new Space(this);actions.addView(sp,new LinearLayout.LayoutParams(dp(10),1));MaterialButton add=new MaterialButton(this);add.setText("＋  Termin");add.setIconResource(android.R.drawable.ic_input_add);add.setOnClickListener(v->showEditor(null));actions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(actions);
        setContentView(root);
    }
    private void refresh(){String key=TimeUtils.dateKey(selected);dateText.setText(TimeUtils.header(selected));Calendar today=Calendar.getInstance();int mins=selected.get(Calendar.YEAR)==today.get(Calendar.YEAR)&&selected.get(Calendar.DAY_OF_YEAR)==today.get(Calendar.DAY_OF_YEAR)?TimeUtils.nowMinute():selected.before(today)?1440:0;int percent=(int)Math.round(mins/14.4);progressText.setText(mins>0?percent+" % des Tages vorbei  ·  "+TimeUtils.time(mins)+" von 24:00 Stunden": "Noch nicht gestartet");Event n=null;for(Event e:store.forDate(key)){if(e.startMinute>mins&&(n==null||e.startMinute<n.startMinute))n=e;}nextText.setText(n==null?"Keine weiteren Termine":"Als Nächstes  ·  "+TimeUtils.time(n.startMinute)+"  "+n.title);planner.setEvents(store.forDate(key));planner.setThemeColors(isLight(),getSurface(),0xFF6750A4,resolve(android.R.attr.textColorPrimary));}
    private boolean isLight(){int ui=getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK;return ui!=android.content.res.Configuration.UI_MODE_NIGHT_YES;}
    private int getSurface(){return isLight()?0xFFF9F7FC:0xFF121212;}
    @Override public void onEventTap(Event e){showEditor(e);}
    @Override public void onEventMoved(Event e){if(e.reminder){cancelReminder(e);scheduleReminder(e);}else{cancelReminder(e);}store.upsert(e);PlannerWidgetProvider.updateAll(this);refresh();}
    @Override public void onDateSwipe(int dir){selected.add(Calendar.DAY_OF_YEAR,dir);refresh();}
    private void showCalendar(){
        final BottomSheetDialog dialog=new BottomSheetDialog(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(16),dp(18),dp(24));
        TextView title=tv(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(selected.getTime()),21); title.setTypeface(null,1); box.addView(title);
        LinearLayout weekdays=new LinearLayout(this); weekdays.setWeightSum(7); String[] wd={"Mo","Di","Mi","Do","Fr","Sa","So"}; for(String w:wd){TextView t=tv(w,12);t.setGravity(Gravity.CENTER);weekdays.addView(t,new LinearLayout.LayoutParams(0,dp(30),1));} box.addView(weekdays);
        Calendar first=(Calendar)selected.clone(); first.set(Calendar.DAY_OF_MONTH,1); int dayOffset=(first.get(Calendar.DAY_OF_WEEK)+5)%7; int max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row=null; for(int i=0;i<42;i++){if(i%7==0){row=new LinearLayout(this);row.setWeightSum(7);grid.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));} int day=i-dayOffset+1; if(day<1||day>max){Space sp=new Space(this);row.addView(sp,new LinearLayout.LayoutParams(0,dp(44),1));continue;} final Calendar cell=(Calendar)first.clone();cell.set(Calendar.DAY_OF_MONTH,day); String key=TimeUtils.dateKey(cell); MaterialButton b=new MaterialButton(this);b.setText(String.valueOf(day));b.setTextSize(13);b.setInsetTop(0);b.setInsetBottom(0);b.setPadding(0,0,0,0); if(store.hasEvents(key)){b.setTextColor(Color.WHITE);b.setBackgroundColor(0xFF6750A4);} else if(TimeUtils.dateKey(selected).equals(key)){b.setStrokeWidth(dp(2));} b.setOnClickListener(v->{selected.setTime(cell.getTime());dialog.dismiss();refresh();});row.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));}
        box.addView(grid); TextView hint=tv("Gefüllte Tage enthalten geplante Termine.",12);hint.setPadding(dp(4),dp(8),0,0);box.addView(hint);dialog.setContentView(box);dialog.show();
    }
    private TextInputLayout field(String hint, LinearLayout box, String value){TextInputLayout l=new TextInputLayout(this);l.setHint(hint);TextInputEditText e=new TextInputEditText(this);e.setText(value);l.addView(e);box.addView(l,new LinearLayout.LayoutParams(-1,dp(70)));return l;}
    private void showEditor(@Nullable Event original){final Event e=original==null?new Event():original; if(original==null)e.date=TimeUtils.dateKey(selected); BottomSheetDialog dialog=new BottomSheetDialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(18),dp(22),dp(24));TextView title=tv(original==null?"Neuer Termin":"Termin bearbeiten",22);title.setTypeface(null,1);box.addView(title);
        TextInputLayout tl=field("Titel",box,e.title); TextInputLayout cat=field("Kategorie",box,e.category);TextInputLayout note=field("Notiz",box,e.note);
        LinearLayout times=new LinearLayout(this);times.setOrientation(LinearLayout.HORIZONTAL);times.setWeightSum(2);Spinner start=new Spinner(this);Spinner end=new Spinner(this);String[] timesArr=new String[96];for(int i=0;i<96;i++)timesArr[i]=TimeUtils.time(i*15);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,timesArr);start.setAdapter(a);end.setAdapter(a);start.setSelection(Math.max(0,Math.min(95,e.startMinute/15)));end.setSelection(Math.max(0,Math.min(95,(e.endMinute-1)/15)));times.addView(start,new LinearLayout.LayoutParams(0,dp(58),1));times.addView(end,new LinearLayout.LayoutParams(0,dp(58),1));box.addView(times);
        Spinner colors=new Spinner(this);String[] names={"Violett","Blau","Grün","Orange","Pink","Indigo","Rose","Petrol"};colors.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));int ci=0;for(int i=0;i<palette.length;i++)if(palette[i]==e.color)ci=i;colors.setSelection(ci);box.addView(colors,new LinearLayout.LayoutParams(-1,dp(58)));
        Spinner icons=new Spinner(this);String[] iconNames={"⏱  Zeit","📚  Lernen","🏫  Schule","🍽  Essen","⚽  Sport","😴  Schlaf","🎯  Termin","✨  Freizeit"};icons.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,iconNames));box.addView(icons,new LinearLayout.LayoutParams(-1,dp(58)));
        CheckBox rem=new CheckBox(this);rem.setText("10 Minuten vorher erinnern");rem.setChecked(e.reminder);box.addView(rem);
        MaterialButton save=new MaterialButton(this);save.setText("Speichern");save.setOnClickListener(v->{e.title=String.valueOf(((TextInputEditText)tl.getEditText()).getText()).trim();if(e.title.isEmpty())e.title="Termin";e.category=String.valueOf(((TextInputEditText)cat.getEditText()).getText()).trim();e.note=String.valueOf(((TextInputEditText)note.getEditText()).getText()).trim();e.startMinute=start.getSelectedItemPosition()*15;e.endMinute=Math.max(e.startMinute+15,(end.getSelectedItemPosition()+1)*15);e.color=palette[colors.getSelectedItemPosition()];e.icon=iconNames[icons.getSelectedItemPosition()];boolean oldReminder=e.reminder;e.reminder=rem.isChecked();if(oldReminder)cancelReminder(e);store.upsert(e);if(e.reminder)scheduleReminder(e);PlannerWidgetProvider.updateAll(this);refresh();dialog.dismiss();});box.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));
        if(original!=null){MaterialButton del=new MaterialButton(this);del.setText("Termin löschen");del.setOnClickListener(v->{new MaterialAlertDialogBuilder(this).setTitle("Termin löschen?").setMessage(e.title).setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,w)->{store.delete(e.id);cancelReminder(e);PlannerWidgetProvider.updateAll(this);refresh();dialog.dismiss();}).show();});box.addView(del,new LinearLayout.LayoutParams(-1,dp(52)));}
        dialog.setContentView(box);dialog.show();
    }
    private void scheduleReminder(Event e){android.app.AlarmManager am=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,e.startMinute/60);c.set(Calendar.MINUTE,e.startMinute%60);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);c.add(Calendar.MINUTE,-10);Calendar selectedDate=(Calendar)selected.clone();c.set(Calendar.YEAR,selectedDate.get(Calendar.YEAR));c.set(Calendar.MONTH,selectedDate.get(Calendar.MONTH));c.set(Calendar.DAY_OF_MONTH,selectedDate.get(Calendar.DAY_OF_MONTH));if(c.before(Calendar.getInstance()))return;Intent in=new Intent(this,ReminderReceiver.class);in.putExtra("title",e.title);in.putExtra("text","In 10 Minuten beginnt "+e.title+".");PendingIntent pi=PendingIntent.getBroadcast(this,e.id.hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);}
    private void cancelReminder(Event e){Intent in=new Intent(this,ReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,e.id.hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((android.app.AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);}
}
