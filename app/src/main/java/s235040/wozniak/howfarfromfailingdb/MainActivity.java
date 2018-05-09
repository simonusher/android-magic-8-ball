package s235040.wozniak.howfarfromfailingdb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends Activity implements SensorEventListener {
    @BindView(R.id.mainMessageTextView) TextView mainMessageTextView;
    @BindView(R.id.resultTextView) TextView resultTextView;
    @BindView(R.id.resultMessageTextView) TextView resultMessageTextView;
    @BindView(R.id.subjectLogoImageView) ImageView subjectLogoImageView;
    @BindView(R.id.ballImageView) ImageView ballImageView;
    @BindView(R.id.ballContainer) ConstraintLayout ballContainer;


    private enum State {
        WORKING,
        CHANGING
    }
    private State state = State.WORKING;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor proximitySensor;
    private float gravity[] = new float[3];
    private float linear_acceleration[] = new float[3];
    private static final float PROXIMITY_THRESHOLD = 1;
    private int counter = 0;

    SoundPool soundPool;
    int waterSoundId;

    private enum Subject {
        BAZY,
        J2ME
    }
    private Subject subject = Subject.BAZY;
    private Random random = new Random();
    private static final int BORDER_VALUE = 10;
    private static final float[] MARKS = {3, 3.5f, 4, 4.5f, 5, 5.5f};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initializeSounds();
        initializeSensors();
    }

    private void initializeSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if(accelerometer != null){
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(proximitySensor != null){
            mSensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void initializeSounds() {
        SoundPool.Builder soundPoolBuilder;
        AudioAttributes audioAttributes;
        AudioAttributes.Builder audioAttributesBuilder;
        audioAttributesBuilder = new AudioAttributes.Builder();
        audioAttributesBuilder.setUsage(AudioAttributes.USAGE_GAME);
        audioAttributesBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
        audioAttributes = audioAttributesBuilder.build();
        soundPoolBuilder = new SoundPool.Builder();
        soundPoolBuilder.setAudioAttributes(audioAttributes);
        soundPool = soundPoolBuilder.build();
        waterSoundId = soundPool.load(this, R.raw.water, 1);
    }

    public void onSensorChanged(SensorEvent event){
        int sensorType = event.sensor.getType();
        if(sensorType == Sensor.TYPE_ACCELEROMETER && state == State.WORKING){
            handleAccelerometerEvent(event);
        }
        else if(sensorType == Sensor.TYPE_PROXIMITY){
            handleProximityEvent(event);

        }
    }

    private void handleAccelerometerEvent(SensorEvent event) {
        isolateValues(event);
        handleCounter();
    }

    private void isolateValues(SensorEvent event) {
        final float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
    }

    private void handleCounter() {
        if(linear_acceleration[0] >= BORDER_VALUE || linear_acceleration[1] >= BORDER_VALUE || linear_acceleration[2] >= BORDER_VALUE){
            counter++;
        }
        if(counter > 3){
            float mark = randomizeMark();
            handleMark(mark);
            counter = 0;
        }
    }


    private void handleProximityEvent(SensorEvent event) {
        float proximity = event.values[0];
        if(state == State.WORKING){
            if(proximity <= PROXIMITY_THRESHOLD){
                state = State.CHANGING;
            }
        }
        else if (state == State.CHANGING){
            if(proximity > PROXIMITY_THRESHOLD){
                state = State.WORKING;
                changeToOtherSubject();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public float randomizeMark(){
        int res = random.nextInt(10);
        final float mark;
        if(res > 6){
            int markIndex = random.nextInt(MARKS.length);
            mark = MARKS[markIndex];
        }
        else {
            mark = 2;
        }
        return mark;
    }

    public void handleMark(final float mark){
        final Animation animShake = AnimationUtils.loadAnimation(this, R.anim.shake);
        animShake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                clearFields();
                soundPool.play(waterSoundId, 1, 1, 0, 0 , 1);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                resultTextView.setText(Float.toString(mark));
                if(mark > 2){
                    resultMessageTextView.setText(R.string.successMsg);
                    resultMessageTextView.setTextColor(Color.GREEN);
                }
                else {
                    if(subject == Subject.BAZY){
                        resultMessageTextView.setText(R.string.accessFailureMessage);
                    }
                    else if(subject == Subject.J2ME){
                        resultMessageTextView.setText(R.string.failureMsg);
                    }
                    resultMessageTextView.setTextColor(Color.RED);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        ballContainer.startAnimation(animShake);
    }

    public void changeToOtherSubject(){
        int animDirection = 0;
        if(subject == Subject.BAZY){
            subjectLogoImageView.setImageResource(R.drawable.j2me);
            subject = Subject.J2ME;
            animDirection = 1;

        }
        else if(subject == Subject.J2ME){
            subjectLogoImageView.setImageResource(R.drawable.access);
            subject = Subject.BAZY;
            animDirection = -1;
        }
        animateLogo(animDirection);
        clearFields();
    }

    private void animateLogo(int animDirection) {
        if (animDirection != 0) {
            int animDegrees = 0;
            if(animDirection == -1){
                animDegrees = -360;
            }
            else if(animDirection == 1){
                animDegrees = 360;
            }
            subjectLogoImageView.animate().rotationBy(animDegrees).setDuration(600);
        }
    }


    public void clearFields(){
        resultTextView.setText("");
        resultMessageTextView.setText("");
    }
}
