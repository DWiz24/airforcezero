package airforcezero;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

//stick your public methods in here
public class PublicMethods {
    static void donateBullets(RobotController rc) throws GameActionException{
        float b = rc.getTeamBullets();
        int v = rc.getTeamVictoryPoints();

        //1st priority: if enough bullets to win game
        if(b/rc.getVictoryPointCost() + v >= 1000)
            rc.donate(rc.getVictoryPointCost()*(1000 - v));

        //2nd priority: if game about to end (less than 400 rounds remaining)
        else if (rc.getRoundLimit() - rc.getRoundNum() < 400)
            rc.donate(b - (b % rc.getVictoryPointCost()));

        //3rd priority: if team has too many bullets (donate down to 100)
        else if(b > 100)
            rc.donate((b - 100) - ((b - 100) % rc.getVictoryPointCost()));
    }
}
