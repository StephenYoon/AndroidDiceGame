package yoon.develop.luckydiceout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.content.SharedPreferences;

import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public static final String EXTRA_MESSAGE = "main-activity-action";

    // Field to hold values
    private String _userEmail;
    private long _score;
    private long _highScore;
    private long _totalRollCount;
    private long _doublesCount;
    private long _triplesCount;

    private TextView _userText;
    private TextView _highScoreText;
    private TextView _doublesCountText;
    private TextView _triplesCountText;
    private TextView _totalRollsText;
    private TextView _rollResult;
    private TextView _scoreText;

    // Field to hold random number generator
    private Random _rand;

    // ArrayList to hold all three dice ImageViews
    private ArrayList<ImageView> _diceImageViews;
    private ArrayList<Integer> _dice;

    // Firebase
    private final int MIN_SESSION_DURATION = 5000;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference _users;
    private UserData _userData;
    private boolean _initialUpdate;

    // Sensors
    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user email
        Intent intent = getIntent();
        String action = intent.getStringExtra(SignInActivity.USER_EMAIL);
        _userEmail = !TextUtils.isEmpty(action) && action != null ? action : null;

        // Firestore cloud db
        db = FirebaseFirestore.getInstance();
        _users = db.collection("users");
        _initialUpdate = true;
        getUserData();

        //
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Roll dice button listener
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollDice(view);
                rollResults();
            }
        });

        // Set initial values
        _score = 0;
        _highScore = 0;
        _totalRollCount = 0;
        _doublesCount = 0;
        _triplesCount = 0;
        _dice = new ArrayList<Integer>(){{
            add(1);
            add(1);
            add(1);
        }};;

        // Create greeting
        Toast.makeText(getApplicationContext(), "Good Luck!", Toast.LENGTH_SHORT).show();

        // Link instances to widgets in the activity view
        _userText = (TextView) findViewById(R.id.userText);
        _highScoreText = (TextView) findViewById(R.id.highScoreText);
        _rollResult = (TextView) findViewById(R.id.rollResult);

        _doublesCountText = (TextView) findViewById(R.id.doublesCountText);
        _triplesCountText = (TextView) findViewById(R.id.triplesCountText);
        _totalRollsText = (TextView) findViewById(R.id.rollCountText);
        _scoreText = (TextView) findViewById(R.id.scoreText);

        // Initialize the random number generator
        _rand = new Random();

        // Access the dice ImageView widgets
        ImageView die1image = (ImageView) findViewById(R.id.die1Image);
        ImageView die2image = (ImageView) findViewById(R.id.die2Image);
        ImageView die3image = (ImageView) findViewById(R.id.die3Image);

        // Add click listeners to dice. NOTE: refactor this repeated code.
        /*
        die1image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollDie(0, true);
            }
        });

        die2image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollDie(1, true);
            }
        });

        die3image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollDie(2, true);
            }
        });
        */

        // Build ArrayList with dice ImageView instances
        _diceImageViews = new ArrayList<ImageView>();
        _diceImageViews.add(die1image);
        _diceImageViews.add(die2image);
        _diceImageViews.add(die3image);

        // Get saved scores
        SharedPreferences sharedPref = this.getSharedPreferences(_userEmail, Context.MODE_PRIVATE);
        _totalRollCount = sharedPref.getLong("total_rolls", 0);
        _highScore = sharedPref.getLong("high_score", 0);

        _userText.setText("User: " + _userEmail);
        _highScoreText.setText("High Score: " + _highScore);
        _doublesCountText.setText("Doubles: " + _doublesCount);
        _triplesCountText.setText("Triples: " + _triplesCount);
        _totalRollsText.setText("Total Rolls: " + _totalRollCount);
        _scoreText.setText("Score: " + _score);

        // Sensors
        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    @Override
    public void onBackPressed()
    {
        finish();
        signUserOut();
        super.onBackPressed();
    }

    // available if you want to include an actual Exit button in activity_main.xml
    private void signUserOut() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra(EXTRA_MESSAGE, "sign-out");
        startActivity(intent);
    }

    public int rollDie(int dieIndex, boolean calculateResults){
        int dieValue = _rand.nextInt(6)+1;
        _dice.set(dieIndex, dieValue);
        String imageName = "die_" + dieValue + ".png";

        try {
            InputStream stream = getAssets().open(imageName);
            Drawable d = Drawable.createFromStream(stream,null);
            _diceImageViews.get(dieIndex).setImageDrawable(d);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(calculateResults){
            rollResults();
        }

        return dieValue;
    }

    public void rollDice(View v) {
        _totalRollCount++;
        for (int dieOfSet = 0; dieOfSet < 3; dieOfSet++) {
            _dice.set(dieOfSet, rollDie(dieOfSet, false));
        }
    }

    public void rollResults(){
        // Build message with the result
        String msg;

        // Run the scoring logic to determine points scored for the roll
        if (_dice.get(0)== _dice.get(1) && _dice.get(0) == _dice.get(2)) {
            // Triples
            int scoreDelta = _dice.get(0)*100;
            msg = "You rolled a triple " + _dice.get(0) + " for " + scoreDelta + " points!";
            _score += scoreDelta;
            _triplesCount++;
        } else if (_dice.get(0) == _dice.get(1) || _dice.get(0) == _dice.get(2) || _dice.get(1) == _dice.get(2)) {
            // Doubles
            msg = "You rolled doubles for 50 points!";
            _score += 50;
            _doublesCount++;
        } else {
            msg = "You didn't score this roll. Try again!";
        }

        // Save scores
        SharedPreferences sharedPref = this.getSharedPreferences(_userEmail, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if(_score > _highScore) {
            _highScore = _score;
        };
        editor.putLong("total_rolls", _totalRollCount);
        editor.putLong("high_score", _highScore);
        editor.commit();

        // Update the app to display the result message
        _rollResult.setText(msg);
        _totalRollsText.setText("Total Rolls: " + _totalRollCount);
        _highScoreText.setText("High Score: " + _highScore);
        _triplesCountText.setText("Triples: " + _triplesCount);
        _doublesCountText.setText("Doubles: " + _doublesCount);
        _scoreText.setText("Score: " + _score);

        // Firestore the high score
        if(_highScore > _userData.HighScore || _initialUpdate){
            upsertUserData(_userEmail, _highScore, _totalRollCount);
            _initialUpdate = false;
        }
    }

    private void getUserData(){
        try{
            DocumentReference docRef = _users.document(_userEmail);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if(!document.exists()){
                            upsertUserData(_userEmail, 0, 0);
                            _userData = new UserData(0, 0);
                        }
                        else if (document != null && document.exists()) {
                            _userData = document.toObject(UserData.class); //task.getResult().getData();
                            //Log.d("MainActivity", "DocumentSnapshot data: " + _userData);
                        } else {
                            //Log.d("MainActivity", "No such document");
                        }
                    } else {
                        //Log.d("MainActivity", "get failed with ", task.getException());
                    }
                }
            });
        }
        catch(Exception ex){
            // ...
        }
    }

    private void upsertUserData(String email, long highScore, long totalRolls){
        try{
            _users.document(email).set(new UserData(highScore, totalRolls));
        }
        catch(Exception ex){
            // ...
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*** Sensor logic ***/
    @Override
    public void onResume() {
        super.onResume();
        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorMan.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float)Math.sqrt(x*x + y*y + z*z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.7f + delta;
            // Make this higher or lower according to how much motion you want to detect
            if(mAccel > 3){
                rollDice(null);
                rollResults();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }
}
