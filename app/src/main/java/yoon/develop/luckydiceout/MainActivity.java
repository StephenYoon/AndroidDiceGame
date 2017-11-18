package yoon.develop.luckydiceout;

import android.content.Intent;
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
    private int _score;
    private int _totalRollCount;
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
        FloatingActionButton fabSignOut = (FloatingActionButton) findViewById(R.id.fabSignOut);
        fabSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUserOut();
            }
        });

        // Set initial values
        _score = 0;
        _totalRollCount = 0;
        _dice = new ArrayList<Integer>(){{
            add(1);
            add(1);
            add(1);
        }};;

        // Create greeting
        Toast.makeText(getApplicationContext(), "Good Luck!", Toast.LENGTH_SHORT).show();

        // Link instances to widgets in the activity view
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
        } else if (_dice.get(0) == _dice.get(1) || _dice.get(0) == _dice.get(2) || _dice.get(1) == _dice.get(2)) {
            // Doubles
            msg = "You rolled doubles for 50 points!";
            _score += 50;
        } else {
            msg = "You didn't score this roll. Try again!";
        }

        // Update the app to display the result message
        _rollResult.setText(msg);
        _scoreText.setText("Score: " + _score);
        _totalRollsText.setText("Rolls: " + _totalRollCount);

        // Log the score as an Analytics event
        Bundle params = new Bundle();
        params.putString("testUser", "testUser");
        params.putInt("score", _score);
        mFBAnalytics.logEvent("score", params);
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
