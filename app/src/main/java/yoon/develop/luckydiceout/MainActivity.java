package yoon.develop.luckydiceout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.content.SharedPreferences;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "main-activity-action";

    // Field to hold values
    private String _userEmail;
    private long _score;
    private long _totalRollCount;
    private long _doublesCount;
    private long _triplesCount;

    private TextView _userText;
    private TextView _highScoreText;
    private TextView _rollResult;
    private TextView _totalRollsText;
    private TextView _scoreText;

    // Field to hold random number generator
    private Random _rand;

    // ArrayList to hold all three dice ImageViews
    private ArrayList<ImageView> _diceImageViews;
    private ArrayList<Integer> _dice;

    // Firebase
    private FirebaseAuth mAuth;
    private final int MIN_SESSION_DURATION = 5000;
    private FirebaseAnalytics mFBAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFBAnalytics = FirebaseAnalytics.getInstance(this); // retrieve an instance of the Analytics package
        mFBAnalytics.setMinimumSessionDuration(MIN_SESSION_DURATION); // wait 5 seconds before counting this as a session

        //
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Roll dice button listener
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _totalRollCount++;
                rollDice(view);
                rollResults();
            }
        });

        // Exit button listener
        Button fabSignOut = (Button) findViewById(R.id.fabSignOut);
        fabSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                signUserOut();
            }
        });

        // Set initial values
        _score = 0;
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
        _scoreText = (TextView) findViewById(R.id.scoreText);
        _totalRollsText = (TextView) findViewById(R.id.rollCountText);

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

        // Get user email
        Intent intent = getIntent();
        String action = intent.getStringExtra(SignInActivity.USER_EMAIL);
        _userEmail = !TextUtils.isEmpty(action) && action != null ? action : null;

        // Get saved scores
        SharedPreferences sharedPref = this.getSharedPreferences(_userEmail, Context.MODE_PRIVATE);
        long totalRolls = sharedPref.getLong("total_rolls", 0);
        long highScore = sharedPref.getLong("high_score", 0);
        long doubles = sharedPref.getLong("doubles", 0);
        long triples = sharedPref.getLong("triples", 0);
        _userText.setText("User: " + _userEmail);
        _highScoreText.setText("High Score: " + highScore);
    }

    @Override
    public void onBackPressed()
    {
        finish();
        signUserOut();
        super.onBackPressed();
    }

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
            _totalRollCount++;
            rollResults();
        }

        return dieValue;
    }

    public void rollDice(View v) {
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
            msg = "You rolled a triple " + _dice.get(0) + "! You scored " + scoreDelta + " points!";
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

        // Update the app to display the result message
        _rollResult.setText(msg);
        _scoreText.setText("Score: " + _score);
        _totalRollsText.setText("Rolls: " + _totalRollCount);

        // Log the score as an Analytics event
        /*Bundle params = new Bundle();
        params.putString("testUser", "testUser");
        params.putInt("score", _score);
        mFBAnalytics.logEvent("score", params);*/

        // Get saved scores
        SharedPreferences sharedPref = this.getSharedPreferences(_userEmail, Context.MODE_PRIVATE);
        long totalRolls = sharedPref.getLong("total_rolls", 0);
        long highScore = sharedPref.getLong("high_score", 0);
        long doubles = sharedPref.getLong("doubles", 0);
        long triples = sharedPref.getLong("triples", 0);

        // Save scores
        SharedPreferences.Editor editor = sharedPref.edit();
        if(_totalRollCount > totalRolls) editor.putLong("total_rolls", _score);
        if(_score > highScore) editor.putLong("high_score", _score);
        if(_doublesCount > doubles) editor.putLong("doubles", _doublesCount);
        if(_triplesCount > triples) editor.putLong("triples", _triplesCount);
        editor.commit();
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
}
