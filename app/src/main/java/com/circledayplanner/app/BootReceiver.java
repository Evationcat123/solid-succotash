package com.circledayplanner.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        PlannerStore store=PlannerStore.get(context);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        for(Event e:store.all()) {
            if(!e.reminder || e.date==null || e.date.isEmpty()) continue;
            try {
                String[] parts=e.date.split("-"); Calendar c=Calendar.getInstance(); c.set(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])-1,Integer.parseInt(parts[2]),e.startMinute/60,e.startMinute%60,0);c.set(Calendar.MILLISECOND,0);c.add(Calendar.MINUTE,-10);
                if(c.before(Calendar.getInstance())) continue;
                Intent in=new Intent(context,ReminderReceiver.class).putExtra("title",e.title).putExtra("text","In 10 Minuten beginnt "+e.title+".");
                PendingIntent pi=PendingIntent.getBroadcast(context,e.id.hashCode(),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
            } catch(Exception ignored) {}
        }
    }
}
