package airforcezero;
import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            try {
                switch (rc.getType()) {
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
            } catch (Throwable t) {

                t.printStackTrace();
                //System.out.println(t);
            }
        }
    }
}