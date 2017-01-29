package airforcezero;

import battlecode.common.*;

//stick your public methods in here
public class PublicMethods {
    static void donateBullets(RobotController rc) throws GameActionException{ //call this every turn
        float b = rc.getTeamBullets();
        int v = rc.getTeamVictoryPoints();

        //1st priority: if enough bullets to win game
        if(b/rc.getVictoryPointCost() + v >= 1000)
            rc.donate(rc.getVictoryPointCost() * (1000 - v));

        //2nd priority: if game about to end (less than 400 rounds remaining)
        else if (rc.getRoundLimit() - rc.getRoundNum() < 400)
            rc.donate(b - (b % rc.getVictoryPointCost()));

        //3rd priority: if not beginning of game and if team has too many bullets (donate down to 150)
        else if(b > 150 && rc.getRoundNum() > 50)
            rc.donate((b - 150) - ((b - 150) % rc.getVictoryPointCost()));
    }
    
    static boolean isAboutToDie(RobotController rc, float lastTurnHealth) {
    	float deltaHealth = lastTurnHealth - rc.getHealth();
    	return deltaHealth>rc.getHealth()/2f;
    }
}
