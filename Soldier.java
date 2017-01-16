package airforcezero;

import battlecode.common.*;

public class Soldier {
    static RobotController rc;
    static MapLocation[] initialEnemyLocs;
    static MapLocation[] initialFriendLocs;
    static int testDest = 0;

    public static void run(RobotController rc) throws GameActionException {
        initialEnemyLocs = rc.getInitialArchonLocations(rc.getTeam().opponent());
        initialFriendLocs = rc.getInitialArchonLocations(rc.getTeam());
        Soldier.rc = rc;
        while (true) {
            //System.out.println(bugging);
            TreeInfo[] trees = rc.senseNearbyTrees();
            RobotInfo[] robots = rc.senseNearbyRobots();
            BulletInfo[] bullets = rc.senseNearbyBullets();
            MapLocation toMove = null;
            RobotInfo[] friend = new RobotInfo[robots.length];
            int friends = -1;
            RobotInfo[] enemy = new RobotInfo[robots.length];
            int enemies = -1;
            int len=robots.length;
            for (int i = 0; i <len; i++) {
                RobotInfo r = robots[i];
                //if (r.team==Team.NEUTRAL) System.out.println("NEUTRAL DETECTED");
                if (r.team == rc.getTeam()) {
                    friend[++friends] = r;
                } else {
                    enemy[++enemies] = r;
                }
            }
            if (enemies != -1 || bullets.length != 0) {
                toMove = micro(rc, trees, friend, friends, enemy, enemies, bullets);
            } else {
                toMove = Nav.soldierNav(rc, trees, robots);
            }
            shootOrMove(rc, toMove, trees, enemy, enemies, friend, friends, bullets);
            shakeATree(rc);
            //rc.move(toMove);
            Clock.yield();
        }
    }

    static void pickDest() {
        Nav.setDest(rc.getLocation().add(new Direction((float) (Math.random() * Math.PI * 2)), 8));
    }

    static MapLocation micro(RobotController rc, TreeInfo[] trees, RobotInfo[] friend, int friends, RobotInfo[] enemy, int enemies, BulletInfo[] bullets) {

        return rc.getLocation();
    }

    static void shootOrMove(RobotController rc, MapLocation toMove, TreeInfo[] trees, RobotInfo[] enemy, int enemies, RobotInfo[] friend, int friends, BulletInfo[] bullets) throws GameActionException {
        rc.move(toMove);


        if (rc.canFireSingleShot()) {
            MapLocation cur = toMove;
            trees=rc.senseNearbyTrees();
            enemy=rc.senseNearbyRobots(-1,rc.getTeam().opponent());
            enemies=enemy.length-1;
            friend=rc.senseNearbyRobots(-1,rc.getTeam());
            friends=friend.length-1;
            BodyInfo[] avoid = new BodyInfo[trees.length + friends + 1];
            int avoids = -1;
            int friendpos = 0;
            int treepos = 0;

            while (true) {
                if (treepos == trees.length) {
                    for (int i = friendpos; i <= friends; i++) {
                        avoid[++avoids] = friend[i];
                    }
                    break;
                } else if (friendpos > friends) {
                    for (int i = treepos; i < trees.length; i++) {
                        if (trees[i].team != rc.getTeam().opponent()) avoid[++avoids] = trees[i];
                    }
                    break;
                } else if (trees[treepos].team == rc.getTeam().opponent()) treepos++;
                else {
                    if (trees[treepos].getLocation().distanceTo(toMove) < friend[friendpos].getLocation().distanceTo(toMove)) {
                        avoid[++avoids] = trees[treepos++];

                    } else {
                        avoid[++avoids] = friend[friendpos++];
                    }
                }
            }
            float[] dists = new float[avoids + 1];
            float[] thetas = new float[avoids + 1];
            Direction[] dirs = new Direction[avoids + 1];
            for (int i = avoids; i >= 0; i--) {
                dists[i] = cur.distanceTo(avoid[i].getLocation());
                thetas[i] = (float) Math.asin(avoid[i].getRadius() / dists[i]);
                dirs[i] = cur.directionTo(avoid[i].getLocation());
            }
            for (int t = 0; t <= enemies; t++) {
                RobotInfo target = enemy[t];
                float d = toMove.distanceTo(target.location);
                float theta = (float) Math.asin(target.getRadius() / d);
                Direction dir = toMove.directionTo(target.location);
                Direction a1 = dir.rotateLeftRads(theta);
                Direction a2 = dir.rotateRightRads(theta);
                //rc.setIndicatorLine(toMove,toMove.add(a1,2),0,0,255);
                //rc.setIndicatorLine(toMove,toMove.add(a2,2),0,0,255);
                boolean valid = true;
                for (int i = 0; i < avoids; i++) {
                    if (d + 3< dists[i]) break;
                    rc.setIndicatorDot(avoid[i].getLocation(),255,0,0);
                    boolean a1contained = Math.abs(a1.radiansBetween(dirs[i])) <= thetas[i];
                    boolean a2contained = Math.abs(a2.radiansBetween(dirs[i])) <= thetas[i];
                    if (a1contained && a2contained || a1.radiansBetween(a2)>=0) {
                        valid = false;
                        break;
                    } else if (a1contained) {
                        a1 = dirs[i].rotateRightRads(thetas[i]+0.0001f);
                    } else if (a2contained) {
                        a2 = dirs[i].rotateLeftRads(thetas[i]+0.0001f);
                    }
                }
                if (valid) {
                    //rc.setIndicatorDot(target.location,0,255,0);
                    rc.setIndicatorLine(toMove,toMove.add(a1.rotateRightDegrees(a2.degreesBetween(a1)/2.0f),8),0,0,255);
                    if (d<3 && rc.canFirePentadShot()) {
                        rc.firePentadShot(a1.rotateRightDegrees(a2.degreesBetween(a1)/2.0f));
                    }else {
                        rc.fireSingleShot(a1.rotateRightDegrees(a2.degreesBetween(a1) / 2.0f));
                    }
                    break;
                }
            }
        }
    }

    static void shakeATree(RobotController rc) throws GameActionException {
        TreeInfo[] trees = rc.senseNearbyTrees(2);
        if (trees.length == 0) return;
        int maxBullets = trees[0].containedBullets;
        int bestTree = 0;
        for (int i = trees.length - 1; i != 0; i--) {
            if (trees[i].containedBullets > maxBullets) {
                maxBullets = trees[i].containedBullets;
                bestTree = i;
            }
        }
        rc.shake(trees[bestTree].ID);
    }
}