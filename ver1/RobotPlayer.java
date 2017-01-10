package battlecode2017.ver1;
import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws Exception{
        switch(rc.getType()) {
            case ARCHON:
                Archon.run(rc);
            case GARDENER:
                Gardener.run(rc);
            case LUMBERJACK:
                Lumberjack.run(rc);
            case SCOUT:
                Scout.run(rc);
            case SOLDIER:
                Soldier.run(rc);
            case TANK:
                Tank.run(rc);
        }
    }
}