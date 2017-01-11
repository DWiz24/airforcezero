package airforcezero;

import battlecode.common.*;

public class Soldier {
    public static void run(RobotController rc) throws GameActionException {
        MapLocation[] initialEnemyLocs = rc.getInitialArchonLocations(rc.getTeam().opponent());
        MapLocation[] initialFriendLocs = rc.getInitialArchonLocations(rc.getTeam());
        int testDest = 1;
        MapLocation dest = initialEnemyLocs[0];
        float bugMinDist = 99999;
        int bugTree = -1;
        boolean treeOrNot = false;
        boolean bugging = false;
        while (true) {
            TreeInfo[] trees = rc.senseNearbyTrees();
            RobotInfo[] robots=rc.senseNearbyRobots();
            float distToDest = rc.getLocation().distanceTo(dest);
            if (distToDest < 3) {
                if (testDest >= initialEnemyLocs.length) {
                    dest = initialFriendLocs[testDest - initialEnemyLocs.length];
                    testDest++;
                } else {
                    dest = initialEnemyLocs[testDest];
                    testDest++;
                }
            }
            Direction toDest = rc.getLocation().directionTo(dest);
            if (!bugging) {
                if (rc.canMove(toDest)) {
                    rc.move(toDest);
                } else {
                    bugging = true;
                    bugMinDist = distToDest;
                    MapLocation move = rc.getLocation().add(toDest, 2);
                    float closest = 999;
                    for (int i = trees.length - 1; i >= 0; i--) {
                        TreeInfo thisTree = trees[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());
                        if (dist < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.radius) {
                            bugTree = thisTree.ID;
                            closest = dist;
                            treeOrNot=true;
                        }
                    }
                    for (int i = robots.length - 1; i >= 0; i--) {
                        RobotInfo thisTree = robots[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());
                        if (dist < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.type.bodyRadius) {
                            bugTree = thisTree.ID;
                            closest = dist;
                            treeOrNot=false;
                        }
                    }
                }
            }
            if (bugging) {
                float dist=rc.getLocation().add(toDest,2).distanceTo(dest);
                if (rc.canMove(toDest)&&dist<bugMinDist) {
                    bugging=false;
                    rc.move(toDest);
                } else {
                    BodyInfo following=treeOrNot?(rc.canSenseTree(bugTree)?rc.senseTree(bugTree):null):(rc.canSenseRobot(bugTree)?rc.senseRobot(bugTree):null);
                    if (following==null) {
                        bugging=false;
                    } else {
                        float distBtw=rc.getLocation().distanceTo(following.getLocation());
                        Direction ndir=new Direction(rc.getLocation().directionTo(following.getLocation()).radians-(float)Math.acos((distBtw*distBtw+4-(following.getRadius()+1)*(following.getRadius()+1))));
                    }
                }
            }
                Clock.yield();
            }
        }
    }