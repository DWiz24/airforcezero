package airforcezero;

import battlecode.common.*;

public class Soldier {
    static RobotController rc;
    static MapLocation[] initialEnemyLocs;
    static MapLocation[] initialFriendLocs;
    static int testDest=0;
    public static void run(RobotController rc) throws GameActionException {
        initialEnemyLocs = rc.getInitialArchonLocations(rc.getTeam().opponent());
        initialFriendLocs = rc.getInitialArchonLocations(rc.getTeam());
        Soldier.rc=rc;
        while (true) {
            //System.out.println(bugging);
            TreeInfo[] trees = rc.senseNearbyTrees();
            RobotInfo[] robots=rc.senseNearbyRobots();
            BulletInfo[] bullets=rc.senseNearbyBullets();
            MapLocation toMove= null;
            RobotInfo[] friend=new RobotInfo[robots.length];
            int friends=-1;
            RobotInfo[] enemy=new RobotInfo[robots.length];
            int enemies=-1;
            for (int i=robots.length-1; i>=0; i--) {
                RobotInfo r=robots[i];
                if (r.team==Team.NEUTRAL) System.out.println("NEUTRAL DETECTED");
                if (r.team==rc.getTeam()) {
                    friend[++friends]=r;
                } else {
                    enemy[++enemies]=r;
                }
            }
            if (enemies!=0 || bullets.length!=0) {
                toMove=micro(rc,trees,friend,friends,enemy,enemies,bullets);
            } else {
                Nav.soldierNav(rc, trees, robots);
            }
            rc.move(toMove);
            Clock.yield();
            }
        }
    static void pickDest() {
        Nav.setDest(rc.getLocation().add(new Direction((float)(Math.random()*Math.PI*2)),12));
    }
    static MapLocation micro(RobotController rc, TreeInfo[] trees, RobotInfo[] friend, int friends, RobotInfo[] enemy, int enemies, BulletInfo[] bullets) {

        return null;
    }
}