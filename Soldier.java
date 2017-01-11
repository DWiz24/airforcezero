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
            //System.out.println(bugging);
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
                    //System.out.println("lol");
                    bugging = true;
                    bugMinDist = distToDest;
                    MapLocation move = rc.getLocation().add(toDest, 2);
                    float closest = 999f;
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
                    if (closest==999f) {
                        bugging=false;
                    }
                }
            }
            if (bugging) {
                float destdist=rc.getLocation().add(toDest,2).distanceTo(dest);
                if (rc.canMove(toDest)&&destdist<bugMinDist) {
                    bugging=false;
                    rc.move(toDest);
                } else {
                    BodyInfo following=treeOrNot?(rc.canSenseTree(bugTree)?rc.senseTree(bugTree):null):(rc.canSenseRobot(bugTree)?rc.senseRobot(bugTree):null);
                    if (following==null) {
                        bugging=false;
                    } else {
                        float distBtw=rc.getLocation().distanceTo(following.getLocation());
                        float cosp=((following.getRadius()+1)*(following.getRadius()+1)-4-distBtw*distBtw)/(-2*distBtw);
                        System.out.println(cosp);
                        float f=(float)Math.acos(cosp);
                        Direction ndir=new Direction(rc.getLocation().directionTo(following.getLocation()).radians-0.005f-f);
                        //System.out.println(f);
                        if (rc.canMove(ndir)) {
                            rc.move(ndir);

                        } else {
                            bugging = true;
                            bugMinDist = distToDest;
                            MapLocation move = rc.getLocation().add(ndir, 2);
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
                            if (closest==999f) {
                                bugging=false;
                            }
                        }
                    }
                }
            }
                Clock.yield();
            }
        }
    }