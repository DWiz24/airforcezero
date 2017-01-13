package airforcezero;

import battlecode.common.*;

class Nav {
    static MapLocation dest = null;
    static float bugMinDist = 99999;
    static int bugTree = -1;
    static boolean treeOrNot = false;
    static boolean bugging = false;
    static MapLocation prevLoc=null;
    static boolean hitWall=false;
    static boolean left=true;
    static MapLocation bugstart=null;
    static int lastMinUpdate=0;
    static void setDest(MapLocation nDest) {
        bugging=false;
        bugMinDist=9999;
        dest=nDest;
    }
    static MapLocation soldierNav(RobotController rc, TreeInfo[] trees, RobotInfo[] robots) throws GameActionException {
        if (dest==null||rc.getLocation().distanceTo(dest)<=4) {
            Soldier.pickDest();
        }
        Direction toDest = rc.getLocation().directionTo(dest);
        float distToDest = rc.getLocation().distanceTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>150) {
            Soldier.pickDest();
        }
        for (int tries=4; tries>=0; tries--){

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
                        Soldier.pickDest();
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
                        /*if (left) {
                            rc.setIndicatorDot(following.getLocation(),0,0,0);
                        } else {
                            rc.setIndicatorDot(following.getLocation(),255,255,255);
                        } */
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 4 - distBtw * distBtw) / (-4 * distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                        MapLocation theMove=rc.getLocation().add(ndir, 2);
                        if (!rc.onTheMap(theMove,1)) {
                            if (hitWall) {
                                System.out.println("YAYY!");
                                Soldier.pickDest();
                            } else {
                                hitWall=true;
                                left=!left;
                                //bugging=false;
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
        return rc.getLocation();
    }

}