package yoon.develop.luckydiceout;

import java.util.Calendar;
import java.util.Date;

public class UserData {
    public UserData(){
        HighScore = 0;
        TotalRolls = 0;
        ForceUpdate = false;
        LastUpdated = Calendar.getInstance().getTime();
    }

    public UserData(long highScore, long totalRolls, Date lastUpdated){
        HighScore = highScore;
        TotalRolls = totalRolls;
        ForceUpdate = false;
        LastUpdated = lastUpdated;
    }

    public long HighScore;

    public long TotalRolls;

    public Date LastUpdated;

    public boolean ForceUpdate;
}
