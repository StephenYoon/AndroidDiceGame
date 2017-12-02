package yoon.develop.luckydiceout;

public class UserData {
    public UserData(){
        HighScore = 0;
        TotalRolls = 0;
    }

    public UserData(long highScore, long totalRolls){
        HighScore = highScore;
        TotalRolls = totalRolls;
    }

    public long HighScore;

    public long TotalRolls;
}
