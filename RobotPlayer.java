package airforcezero;
import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        Soldier.rc=rc;
        while (true) {
            try {
                switch (rc.getType()) {
                    case ARCHON:
                    	rc.broadcast(0, rc.readBroadcast(0) + 1);
                        Archon.run(rc);
                        break;
                    case GARDENER:
                    	rc.broadcast(1, rc.readBroadcast(1) + 1);
                        Gardener.run(rc);
                        break;
                    case SOLDIER:
                        rc.broadcast(3, rc.readBroadcast(3) + 1);
                        Soldier.run(rc);
                        break;
                    case LUMBERJACK:
                        rc.broadcast(2, rc.readBroadcast(2) + 1);
                        Lumberjack.run(rc);
                        break;
                    case TANK:
                    	rc.broadcast(4, rc.readBroadcast(4) + 1);
                        Tank.run(rc);
                    case SCOUT:
                    	rc.broadcast(5, rc.readBroadcast(5) + 1);
                        Scout.run(rc);
                        break;
                }
            } catch (Throwable t) {

                t.printStackTrace();
                //System.out.println(t);
            }
        }
    }
}