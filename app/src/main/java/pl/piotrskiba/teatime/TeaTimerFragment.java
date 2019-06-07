package pl.piotrskiba.teatime;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.piotrskiba.teatime.services.CountDownTimerService;

public class TeaTimerFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    @BindView(R.id.sb_timer)
    SeekBar mTimerSeekBar;

    @BindView(R.id.tv_seekbar_title)
    TextView mTimerSeekBarTitle;

    @BindView(R.id.pb_timer)
    CircularProgressBar mTimerProgressBar;

    @BindView(R.id.tv_timer)
    TextView mTimerTextView;

    @BindView(R.id.btn_start_timer)
    Button mTimerStartButton;

    @BindView(R.id.btn_stop_timer)
    Button mTimerStopButton;

    @BindView(R.id.btn_disable_alarm)
    Button mDisableAlarmButton;


    private int mTeaIndex;
    private int mTotalBrewingTime;

    private TimerUpdateReceiver mTimerUpdateReceiver;

    public boolean showAlarmLayout = false;

    public static boolean inForeground = true;

    public TeaTimerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_tea_timer, container, false);

        ButterKnife.bind(this, rootView);

        if(savedInstanceState != null && savedInstanceState.containsKey(Constants.EXTRA_INDEX)){
            mTeaIndex = savedInstanceState.getInt(Constants.EXTRA_INDEX);
        }

        return rootView;
    }

    public void setTeaIndex(int index){
        mTeaIndex = index;
    }

    private void populateFragment() {
        mTimerSeekBar.setOnSeekBarChangeListener(this);
        if(showAlarmLayout) {
            showAlarmLayout();
        }
        else if(isServiceRunning(CountDownTimerService.class)) {
            String tea_id = getResources().getStringArray(R.array.tea_ids)[mTeaIndex];
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            int timeleft = sharedPreferences.getInt(getString(R.string.pref_timeleft_key, tea_id), 0);
            if(timeleft > 0){
                setDefaultSeekBarProgress();
                showBrewingLayout();

                updateTimerText(timeleft);

                mTotalBrewingTime = sharedPreferences.getInt(getString(R.string.pref_total_time_key, tea_id), 1);

                int progress = (int)((float)timeleft/mTotalBrewingTime*1000);
                mTimerProgressBar.setProgress(progress);
            }
            else{
                setDefaultSeekBarProgress();
            }
        }
        else {
            setDefaultSeekBarProgress();
        }

        mTimerStartButton.setOnClickListener(this);
        mTimerStopButton.setOnClickListener(this);
        mDisableAlarmButton.setOnClickListener(this);
    }

    private void setDefaultSeekBarProgress() {
        int max = getResources().getIntArray(R.array.tea_max_brewing_time)[mTeaIndex];

        if (max != 0) {
            int min = getResources().getIntArray(R.array.tea_min_brewing_time)[mTeaIndex];
            float defVal = getResources().getIntArray(R.array.tea_default_brewing_time)[mTeaIndex];
            int sbval = (int)((defVal-min) / (max-min) * 100);
            mTimerSeekBar.setProgress(sbval);

            onProgressChanged(mTimerSeekBar, mTimerSeekBar.getProgress(), false);
        }
    }

    private int getSeekBarValue(int progress){
        int min = getResources().getIntArray(R.array.tea_min_brewing_time)[mTeaIndex];
        int max = getResources().getIntArray(R.array.tea_max_brewing_time)[mTeaIndex];

        progress = (int)((float)progress/100 * (max-min));
        progress /= 5;
        progress *= 5;
        progress += min;

        return progress;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int seconds = getSeekBarValue(progress);
        updateTimerText(seconds);
    }

    private void updateTimerText(int seconds){
        int minutes = 0;

        while(seconds >= 60){
            seconds -= 60;
            minutes += 1;
        }

        mTimerTextView.setText(getString(R.string.timer_format, minutes, seconds));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_start_timer) {
            startBrewing();
        }
        else if(v.getId() == R.id.btn_stop_timer) {
            cancelBrewing();
        }
        else if(v.getId() == R.id.btn_disable_alarm){
            ((TeaDetailsActivity) getContext()).stopAlarm();
            setDefaultSeekBarProgress();
            showDefaultLayout();
        }
    }

    private void startBrewing(){
        mTotalBrewingTime = getSeekBarValue(mTimerSeekBar.getProgress());

        Intent timerService = new Intent(getContext(), CountDownTimerService.class);
        timerService.putExtra(Constants.EXTRA_INDEX, mTeaIndex);
        timerService.putExtra(Constants.EXTRA_SECONDS, mTotalBrewingTime);
        getContext().getApplicationContext().startService(timerService);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String tea_id = getResources().getStringArray(R.array.tea_ids)[mTeaIndex];
        editor.putInt(getString(R.string.pref_total_time_key, tea_id), mTotalBrewingTime);
        editor.commit();

        showBrewingLayout();
    }

    private void cancelBrewing(){
        getContext().getApplicationContext().stopService(new Intent(getContext(), CountDownTimerService.class));
        showDefaultLayout();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String tea_id = getResources().getStringArray(R.array.tea_ids)[mTeaIndex];
        editor.putInt(getString(R.string.pref_timeleft_key, tea_id), 0);
        editor.commit();

        int seconds = getSeekBarValue(mTimerSeekBar.getProgress());
        updateTimerText(seconds);
    }

    private void showBrewingLayout(){
        mTimerStartButton.setVisibility(View.GONE);
        mTimerStopButton.setVisibility(View.VISIBLE);
        mDisableAlarmButton.setVisibility(View.GONE);
        mTimerSeekBar.setVisibility(View.INVISIBLE);
        mTimerSeekBarTitle.setVisibility(View.INVISIBLE);
    }

    private void showDefaultLayout(){
        mTimerStartButton.setVisibility(View.VISIBLE);
        mTimerStopButton.setVisibility(View.GONE);
        mDisableAlarmButton.setVisibility(View.GONE);
        mTimerSeekBar.setVisibility(View.VISIBLE);
        mTimerSeekBarTitle.setVisibility(View.VISIBLE);
        mTimerProgressBar.setProgressWithAnimation(1000);
    }

    private void showAlarmLayout(){
        updateTimerText(0);
        mTimerProgressBar.setProgress(0);

        mTimerStartButton.setVisibility(View.GONE);
        mTimerStopButton.setVisibility(View.GONE);
        mDisableAlarmButton.setVisibility(View.VISIBLE);
        mTimerSeekBar.setVisibility(View.INVISIBLE);
        mTimerSeekBarTitle.setVisibility(View.INVISIBLE);
    }

    private boolean isServiceRunning(Class serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTimerUpdateReceiver == null)
            mTimerUpdateReceiver = new TimerUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(Constants.TIMER_UPDATE_ACTION);
        getContext().registerReceiver(mTimerUpdateReceiver, intentFilter);

        inForeground = true;

        populateFragment();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mTimerUpdateReceiver != null)
            getContext().unregisterReceiver(mTimerUpdateReceiver);

        inForeground = false;
    }

    private class TimerUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.TIMER_UPDATE_ACTION)) {
                int teaIndex = intent.getIntExtra(Constants.EXTRA_INDEX, -1);
                if(teaIndex == mTeaIndex) {
                    int seconds = intent.getIntExtra(Constants.EXTRA_SECONDS, -1);

                    if(seconds == 0){
                        showAlarmLayout();
                        ((TeaDetailsActivity) getContext()).startAlarm();
                    }
                    else {
                        updateTimerText(seconds);

                        int progress = (int) ((float) seconds / mTotalBrewingTime * 1000);
                        mTimerProgressBar.setProgress(progress);
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(Constants.EXTRA_INDEX, mTeaIndex);
    }
}
