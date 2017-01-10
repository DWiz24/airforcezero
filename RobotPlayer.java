package battlecode2017.ver1;
import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException{
        switch(rc.getType()) {
            case ARCHON:
                Archon.run(rc);
                break;
            case GARDENER:
                Gardener.run(rc);
                break;
            case LUMBERJACK:
                Lumberjack.run(rc);
                break;
            case SCOUT:
                Scout.run(rc);
                break;
            case SOLDIER:
                Soldier.run(rc);
                break;
            case TANK:
                Tank.run(rc);
        }
    }
}