package airforcezero;

import battlecode.common.*;

public class Soldier {
    static MapLocation dest = null;
    static float bugMinDist = 99999;
    static int bugTree = -1;
    static boolean treeOrNot = false;
    static boolean bugging = false;
    static MapLocation prevLoc=null;
    static boolean hitWall=false;
    static boolean left=true;
    static MapLocation bugstart=null;
    static MapLocation[] initialEnemyLocs;
    static MapLocation[] initialFriendLocs;
    static int testDest=0;
    public static void run(RobotController rc) throws GameActionException {
        initialEnemyLocs = rc.getInitialArchonLocations(rc.getTeam().opponent());
        initialFriendLocs = rc.getInitialArchonLocations(rc.getTeam());
        pickDest();
        while (true) {
            //System.out.println(bugging);
            TreeInfo[] trees = rc.senseNearbyTrees();
            RobotInfo[] robots=rc.senseNearbyRobots();

            MapLocation toMove=nav(rc,trees,robots);
            rc.move(toMove);
            Clock.yield();
            }
        }
        static MapLocation nav(RobotController rc, TreeInfo[] trees, RobotInfo[] robots) throws GameActionException {
            if (rc.getLocation().distanceTo(dest)<=4) {
                pickDest();
            }
            Direction toDest = rc.getLocation().directionTo(dest);
            float distToDest = rc.getLocation().distanceTo(dest);
            bugMinDist=Math.min(bugMinDist,distToDest);
            while (true) {

                if (!bugging) {
                    if (rc.canMove(toDest)) {
                        return rc.getLocation().add(toDest, 2);
                    } else {
                        bugging = true;
                        MapLocation move = rc.getLocation().add(toDest, 2);
                        hitWall = false;
                        bugstart = rc.getLocation();
                        float closest = 999f;
                        for (int i = trees.length - 1; i >= 0; i--) {
                            TreeInfo thisTree = trees[i];
                            float dist = thisTree.location.distanceTo(rc.getLocation());
                            if (dist < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.radius) {
                                bugTree = thisTree.ID;
                                closest = dist;
                                treeOrNot = true;
                                prevLoc = thisTree.location;
                            }
                        }
                        for (int i = robots.length - 1; i >= 0; i--) {
                            RobotInfo thisTree = robots[i];
                            float dist = thisTree.location.distanceTo(rc.getLocation());
                            if (dist < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.type.bodyRadius) {
                                bugTree = thisTree.ID;
                                closest = dist;
                                treeOrNot = false;
                                prevLoc = thisTree.location;
                            }
                        }
                        if (closest == 999f) {
                            pickDest();
                        }
                    }
                }

                if (bugging) {
                    float destdist = rc.getLocation().add(toDest, 2).distanceTo(dest);
                    if (rc.canMove(toDest) && destdist < bugMinDist) {
                        bugging = false;
                        return rc.getLocation().add(toDest, 2);
                    } else {
                        BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                        if (following == null || following.getLocation() != prevLoc) {
                            bugging = false;
                        } else {
                            if (left) {
                                rc.setIndicatorDot(following.getLocation(),0,0,0);
                            } else {
                                rc.setIndicatorDot(following.getLocation(),255,255,255);
                            }
                            float distBtw = rc.getLocation().distanceTo(following.getLocation());
                            float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 4 - distBtw * distBtw) / (-4 * distBtw);
                            float f = (float) Math.acos(cosp);
                            Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                            MapLocation theMove=rc.getLocation().add(ndir, 2);
                            if (!rc.onTheMap(theMove)) {
                                if (hitWall) {
                                    System.out.println("YAYY!");
                                    pickDest();
                                } else {
                                    hitWall=true;
                                    left=!left;
                                }
                            } else if (rc.canMove(ndir)) {
                                return theMove;
                            } else {
                                bugging = true;
                                MapLocation move = rc.getLocation().add(ndir, 2);
                                float closest = 999;
                                for (int i = trees.length - 1; i >= 0; i--) {
                                    TreeInfo thisTree = trees[i];
                                    float dist = thisTree.location.distanceTo(rc.getLocation());
                                    if (dist < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.radius) {
                                        bugTree = thisTree.ID;
                                        closest = dist;
                                        treeOrNot = true;
                                        prevLoc = thisTree.location;
                                    }
                                }
                                for (int i = robots.length - 1; i >= 0; i--) {
                                    RobotInfo thisTree = robots[i];
                                    float dist = thisTree.location.distanceTo(rc.getLocation());
                                    if (dist < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.type.bodyRadius) {
                                        bugTree = thisTree.ID;
                                        closest = dist;
                                        treeOrNot = false;
                                        prevLoc = thisTree.location;
                                    }
                                }
                                //if (bugging && prevBugTree == bugTree) System.out.println("HOLD UP");
                            }
                        }
                    }
                }
            }
        }
        static void pickDest() {
        bugging=false;
        bugMinDist=9999;
            if (testDest-initialEnemyLocs.length==initialFriendLocs.length) testDest=0;
            if (testDest >= initialEnemyLocs.length) {

                dest = initialFriendLocs[testDest - initialEnemyLocs.length];
                testDest++;
            } else {
                dest = initialEnemyLocs[testDest];
                testDest++;
            }
        }
    }