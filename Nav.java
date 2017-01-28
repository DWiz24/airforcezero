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

            if (Soldier.importantDest) {
                rc.broadcast(Soldier.whichDest, 0);
            }
            Soldier.pickDest();
        }
        Direction toDest = rc.getLocation().directionTo(dest);
        float distToDest = rc.getLocation().distanceTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        //rc.setIndicatorDot(dest,255,0,0);

        //bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>60) {
            Soldier.pickDest();
        }
        float[] treeDists=new float[trees.length];
        float[] robotDists=new float[robots.length];
        for (int i=trees.length-1; i>=0; i--) treeDists[i]=rc.getLocation().distanceTo(trees[i].location);
        for (int i=robots.length-1; i>=0; i--) robotDists[i]=rc.getLocation().distanceTo(robots[i].location);
        for (int tries=5; tries>=0; tries--){

            if (!bugging) {
                if (rc.canMove(toDest)) {
                    return rc.getLocation().add(toDest, 0.8f);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 0.8f);
                    hitWall = false;
                    bugstart = rc.getLocation();
                    float closest = 999f;
                    for (int i = trees.length - 1; i >= 0; i--) {
                        TreeInfo thisTree = trees[i];
                        if (treeDists[i] < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.radius) {
                            bugTree = thisTree.ID;
                            closest = treeDists[i];
                            treeOrNot = true;
                            prevLoc = thisTree.location;
                        }
                    }
                    for (int i = robots.length - 1; i >= 0; i--) {
                        RobotInfo thisTree = robots[i];
                        if (robotDists[i] < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.type.bodyRadius) {
                            bugTree = thisTree.ID;
                            closest = robotDists[i];
                            treeOrNot = false;
                            prevLoc = thisTree.location;
                        }
                    }

                    if (closest == 999f) {
                        Soldier.pickDest();
                    }
                    if (bugging)
                    left=toDest.degreesBetween(rc.getLocation().directionTo(prevLoc))>0;
                }
            }

            if (bugging) {
                float destdist = rc.getLocation().add(toDest, 0.8f).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 0.8f);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        rc.setIndicatorLine(dest,dest.add(toDest.opposite(),bugMinDist),0,0,0);
                        rc.setIndicatorDot(following.getLocation(),0,255,0);
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 0.64f - distBtw * distBtw) / (-1.6f * distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.00004f:f + 0.00004f));
                        MapLocation theMove=rc.getLocation().add(ndir, 0.8f);
                        //rc.setIndicatorDot(theMove,255,255,255);
                        if (!(rc.canSenseAllOfCircle(theMove,1) && rc.onTheMap(theMove,1))) {
                            if (hitWall) {
                                //System.out.println("YAYY!");
                                Soldier.pickDest();
                            } else {
                                hitWall=true;
                                left=!left;
                                //bugging=false;
                            }
                        } else if (rc.canMove(theMove)) {
                            return theMove;
                        } else {
                            bugging = true;
                            MapLocation move = theMove;
                            float closest = 999;
                            rc.setIndicatorDot(theMove,255,0,0);
                            for (int i = trees.length - 1; i >= 0; i--) {
                                TreeInfo thisTree = trees[i];
                                if (treeDists[i] < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.radius) {
                                    bugTree = thisTree.ID;
                                    closest = treeDists[i];
                                    treeOrNot = true;
                                    prevLoc = thisTree.location;
                                }
                            }
                            for (int i = robots.length - 1; i >= 0; i--) {
                                RobotInfo thisTree = robots[i];
                                if (robotDists[i] < closest && thisTree.location.distanceTo(move) <= 1 + thisTree.type.bodyRadius) {
                                    bugTree = thisTree.ID;
                                    closest = robotDists[i];
                                    treeOrNot = false;
                                    prevLoc = thisTree.location;
                                }
                            }
                        }
                    }
                }
            }
        }
        return rc.getLocation();
    }
    static MapLocation lumberjackNav(RobotController rc, TreeInfo[] trees, RobotInfo[] robots) throws GameActionException {
        if (dest == null || dest.equals(rc.getLocation()))  //base case
            Lumberjack.pickDest(true);  //update upon reaching

        if(Lumberjack.DEBUG1){
            if(rc.getTeam() == Team.A)
                rc.setIndicatorDot(dest, 255, 0, 0);
            else
                rc.setIndicatorDot(dest, 0, 0, 255);
        }

        float distToDest = rc.getLocation().distanceTo(dest);
        if(distToDest < 0.75f && rc.canMove(dest)) {
            Lumberjack.pickDest(true);  //update upon reaching
            return dest;
        }

        Direction toDest = rc.getLocation().directionTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>50) {    //stuck (can't get closer for 50 turns)
            Lumberjack.pickDest(false);
            lastMinUpdate=rc.getRoundNum();
        }
        for (int tries=5; tries>=0; tries--){

            if (!bugging) {
                if (rc.canMove(toDest)) {
                    return rc.getLocation().add(toDest, 0.75f);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 0.75f);
                    hitWall = false;
                    bugstart = rc.getLocation();
                    float closest = 999f;
                    for (int i = trees.length - 1; i >= 0; i--) {
                        TreeInfo thisTree = trees[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());   //treeDists[i]
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

                    if (closest == 999f) {  //blocked by robots
                        Lumberjack.pickDest(false);
                        lastMinUpdate=rc.getRoundNum();
                    }
                    if (bugging)
                        left=toDest.degreesBetween(rc.getLocation().directionTo(prevLoc))>0;
                }
            }

            if (bugging) {
                float destdist = rc.getLocation().add(toDest, 0.75f).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 0.75f);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 0.5625f - distBtw * distBtw) / (1.5f * distBtw);
                        System.out.println(cosp);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.00004f:f + 0.00004f));
                        MapLocation theMove=rc.getLocation().add(ndir, 0.75f);
                        if (!(rc.canSenseAllOfCircle(theMove,1) && rc.onTheMap(theMove,1))) {
                            if (hitWall) {  //hit edge of map
                                //System.out.println("YAYY!");
                                Lumberjack.pickDest(false);
                                lastMinUpdate=rc.getRoundNum();
                            } else {
                                hitWall=true;
                                left=!left;
                                //bugging=false;
                            }
                        } else if (rc.canMove(ndir)) {
                            return theMove;
                        } else {
                            bugging = true;
                            MapLocation move = rc.getLocation().add(ndir, 0.75f);
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
                        }
                    }
                }
            }
        }
        return rc.getLocation();
    }
    static MapLocation gardenerNav(RobotController rc, TreeInfo[] trees, RobotInfo[] robots) throws GameActionException {
        if (dest==null||rc.getLocation().distanceTo(dest)<=4) {
            Gardener.pickDest();
        }
        Direction toDest = rc.getLocation().directionTo(dest);
        float distToDest = rc.getLocation().distanceTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>150) {
            Gardener.pickDest();
        }
        for (int tries=5; tries>=0; tries--){

            if (!bugging) {
                if (rc.canMove(toDest)) {
                    return rc.getLocation().add(toDest, 1);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 1);
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
                        Gardener.pickDest();
                    }
                    if (bugging)
                        left=toDest.degreesBetween(rc.getLocation().directionTo(prevLoc))>0;
                }
            }

            if (bugging) {
                float destdist = rc.getLocation().add(toDest, 1).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 1);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 1 - distBtw * distBtw) / (2 * distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                        MapLocation theMove=rc.getLocation().add(ndir, 2);
                        if (rc.canSenseAllOfCircle(theMove, 1) && !rc.onTheMap(theMove,1)) {
                            if (hitWall) {
                                //System.out.println("YAYY!");
                                Gardener.pickDest();
                            } else {
                                hitWall=true;
                                left=!left;
                                //bugging=false;
                            }
                        } else if (rc.canMove(ndir)) {
                            return theMove;
                        } else {
                            bugging = true;
                            MapLocation move = rc.getLocation().add(ndir, 1.5f);
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
                        }
                    }
                }
            }
        }
        return rc.getLocation();
    }
    static MapLocation tankNav(RobotController rc, TreeInfo[] trees, RobotInfo[] robots) throws GameActionException {
        if (dest==null||rc.getLocation().distanceTo(dest)<=4) {
            Tank.pickDest();
        }
        Direction toDest = rc.getLocation().directionTo(dest);
        float distToDest = rc.getLocation().distanceTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>150) {
            Tank.pickDest();
        }
        for (int tries=5; tries>=0; tries--){

            if (!bugging) {
                if (rc.canMove(toDest)) {
                    return rc.getLocation().add(toDest, 1);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 1);
                    hitWall = false;
                    bugstart = rc.getLocation();
                    float closest = 999f;
                    for (int i = trees.length - 1; i >= 0; i--) {
                        TreeInfo thisTree = trees[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());
                        if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.radius) {
                            bugTree = thisTree.ID;
                            closest = dist;
                            treeOrNot = true;
                            prevLoc = thisTree.location;
                        }
                    }
                    for (int i = robots.length - 1; i >= 0; i--) {
                        RobotInfo thisTree = robots[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());
                        if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.type.bodyRadius) {
                            bugTree = thisTree.ID;
                            closest = dist;
                            treeOrNot = false;
                            prevLoc = thisTree.location;
                        }
                    }

                    if (closest == 999f) {
                        Tank.pickDest();
                    }
                    if (bugging)
                        left=toDest.degreesBetween(rc.getLocation().directionTo(prevLoc))>0;
                }
            }

            if (bugging) {
                float destdist = rc.getLocation().add(toDest, 1).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 1);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 2) * (following.getRadius() + 2) - 1 - distBtw * distBtw) / (2 * distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                        MapLocation theMove=rc.getLocation().add(ndir, 1);
                        if (rc.canSenseAllOfCircle(theMove, 1) && !rc.onTheMap(theMove,2)) {
                            if (hitWall) {
                                //System.out.println("YAYY!");
                                Tank.pickDest();
                            } else {
                                hitWall=true;
                                left=!left;
                                //bugging=false;
                            }
                        } else if (rc.canMove(ndir)) {
                            return theMove;
                        } else {
                            bugging = true;
                            MapLocation move = rc.getLocation().add(ndir, 1);
                            float closest = 999;
                            for (int i = trees.length - 1; i >= 0; i--) {
                                TreeInfo thisTree = trees[i];
                                float dist = thisTree.location.distanceTo(rc.getLocation());
                                if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.radius) {
                                    bugTree = thisTree.ID;
                                    closest = dist;
                                    treeOrNot = true;
                                    prevLoc = thisTree.location;
                                }
                            }
                            for (int i = robots.length - 1; i >= 0; i--) {
                                RobotInfo thisTree = robots[i];
                                float dist = thisTree.location.distanceTo(rc.getLocation());
                                if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.type.bodyRadius) {
                                    bugTree = thisTree.ID;
                                    closest = dist;
                                    treeOrNot = false;
                                    prevLoc = thisTree.location;
                                }
                            }
                        }
                    }
                }
            }
        }
        return rc.getLocation();
    }
    static MapLocation archonNav(RobotController rc, TreeInfo[] trees, RobotInfo[] robots) throws GameActionException {
        if (dest==null||rc.getLocation().distanceTo(dest)<=4) {
            Archon.pickDest();
        }
        Direction toDest = rc.getLocation().directionTo(dest);
        float distToDest = rc.getLocation().distanceTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>60) {
            Archon.pickDest();
        }
        for (int tries=5; tries>=0; tries--){

            if (!bugging) {
                if (rc.canMove(toDest)) {
                    return rc.getLocation().add(toDest, 0.5f);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 0.5f);
                    hitWall = false;
                    bugstart = rc.getLocation();
                    float closest = 999f;
                    for (int i = trees.length - 1; i >= 0; i--) {
                        TreeInfo thisTree = trees[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());
                        if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.radius) {
                            bugTree = thisTree.ID;
                            closest = dist;
                            treeOrNot = true;
                            prevLoc = thisTree.location;
                        }
                    }
                    for (int i = robots.length - 1; i >= 0; i--) {
                        RobotInfo thisTree = robots[i];
                        float dist = thisTree.location.distanceTo(rc.getLocation());
                        if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.type.bodyRadius) {
                            bugTree = thisTree.ID;
                            closest = dist;
                            treeOrNot = false;
                            prevLoc = thisTree.location;
                        }
                    }

                    if (closest == 999f) {
                        Archon.pickDest();
                    }
                    if (bugging)
                        left=toDest.degreesBetween(rc.getLocation().directionTo(prevLoc))>0;
                }
            }

            if (bugging) {
                float destdist = rc.getLocation().add(toDest, 0.5f).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 0.5f);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 2) * (following.getRadius() + 2) - 0.25f - distBtw * distBtw) / (distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                        MapLocation theMove=rc.getLocation().add(ndir, 0.5f);
                        if (rc.canSenseAllOfCircle(theMove, 1) && !rc.onTheMap(theMove,2)) {
                            if (hitWall) {
                                //System.out.println("YAYY!");
                                Archon.pickDest();
                            } else {
                                hitWall=true;
                                left=!left;
                                //bugging=false;
                            }
                        } else if (rc.canMove(ndir)) {
                            return theMove;
                        } else {
                            bugging = true;
                            MapLocation move = rc.getLocation().add(ndir, 0.5f);
                            float closest = 999;
                            for (int i = trees.length - 1; i >= 0; i--) {
                                TreeInfo thisTree = trees[i];
                                float dist = thisTree.location.distanceTo(rc.getLocation());
                                if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.radius) {
                                    bugTree = thisTree.ID;
                                    closest = dist;
                                    treeOrNot = true;
                                    prevLoc = thisTree.location;
                                }
                            }
                            for (int i = robots.length - 1; i >= 0; i--) {
                                RobotInfo thisTree = robots[i];
                                float dist = thisTree.location.distanceTo(rc.getLocation());
                                if (dist < closest && thisTree.location.distanceTo(move) <= 2 + thisTree.type.bodyRadius) {
                                    bugTree = thisTree.ID;
                                    closest = dist;
                                    treeOrNot = false;
                                    prevLoc = thisTree.location;
                                }
                            }
                        }
                    }
                }
            }
        }
        return rc.getLocation();
    }
}