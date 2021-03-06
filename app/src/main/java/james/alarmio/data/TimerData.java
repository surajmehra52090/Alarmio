package james.alarmio.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import james.alarmio.receivers.TimerReceiver;

public class TimerData implements Parcelable {

    private int id;
    private long duration = 600000;
    private long endTime;

    public TimerData(int id) {
        this.id = id;
    }

    public TimerData(int id, Context context) {
        this.id = id;
        duration = PreferenceData.TIMER_DURATION.getSpecificValue(context, id);
        endTime = PreferenceData.TIMER_END_TIME.getSpecificValue(context, id);
    }

    protected TimerData(Parcel in) {
        id = in.readInt();
        duration = in.readLong();
        endTime = in.readLong();
    }

    public static final Creator<TimerData> CREATOR = new Creator<TimerData>() {
        @Override
        public TimerData createFromParcel(Parcel in) {
            return new TimerData(in);
        }

        @Override
        public TimerData[] newArray(int size) {
            return new TimerData[size];
        }
    };

    public void onIdChanged(int id, Context context) {
        PreferenceData.TIMER_DURATION.setValue(context, duration, id);
        PreferenceData.TIMER_END_TIME.setValue(context, endTime, id);
        onRemoved(context);
        this.id = id;
        if (isSet())
            set(context, (AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    }

    public void onRemoved(Context context) {
        cancel(context, (AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
        PreferenceData.TIMER_DURATION.setValue(context, null, id);
        PreferenceData.TIMER_END_TIME.setValue(context, null, id);
    }

    public boolean isSet() {
        return endTime > System.currentTimeMillis();
    }

    public long getRemainingMillis() {
        return Math.max(endTime - System.currentTimeMillis(), 0);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration, Context context) {
        this.duration = duration;
        PreferenceData.TIMER_DURATION.setValue(context, duration, id);
    }

    public void set(Context context, AlarmManager manager) {
        endTime = System.currentTimeMillis() + duration;
        setAlarm(context, manager);

        PreferenceData.TIMER_END_TIME.setValue(context, endTime, id);
    }

    public void setAlarm(Context context, AlarmManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            manager.setExact(AlarmManager.RTC_WAKEUP, endTime, getIntent(context));
        else manager.set(AlarmManager.RTC_WAKEUP, endTime, getIntent(context));
    }

    public void cancel(Context context, AlarmManager manager) {
        endTime = 0;
        manager.cancel(getIntent(context));

        PreferenceData.TIMER_END_TIME.setValue(context, endTime, id);
    }

    private PendingIntent getIntent(Context context) {
        Intent intent = new Intent(context, TimerReceiver.class);
        intent.putExtra(TimerReceiver.EXTRA_TIMER_ID, id);
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeLong(duration);
        parcel.writeLong(endTime);
    }
}
