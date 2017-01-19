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
        bugMinDist=Math.min(bugMinDist,distToDest);
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
                    return rc.getLocation().add(toDest, 2);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 2);
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
                float destdist = rc.getLocation().add(toDest, 2).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 2);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 4 - distBtw * distBtw) / (-4 * distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                        MapLocation theMove=rc.getLocation().add(ndir, 2);
                        if (!(rc.canSenseAllOfCircle(theMove,1) && rc.onTheMap(theMove,1))) {
                            if (hitWall) {
                                //System.out.println("YAYY!");
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
        if (dest==null||rc.getLocation().distanceTo(dest)<=4) {
            Lumberjack.pickDest();
            lastMinUpdate=rc.getRoundNum();
        }

        //debug
        if(rc.getTeam() == Team.A)
            rc.setIndicatorDot(dest, 255, 0, 0);
        else
            rc.setIndicatorDot(dest, 0, 0, 255);

        Direction toDest = rc.getLocation().directionTo(dest);
        float distToDest = rc.getLocation().distanceTo(dest);
        if (distToDest<bugMinDist) {
            bugMinDist=distToDest;
            lastMinUpdate=rc.getRoundNum();
        }
        bugMinDist=Math.min(bugMinDist,distToDest);
        if (rc.getRoundNum()-lastMinUpdate>150) {
            Lumberjack.pickDest();
            lastMinUpdate=rc.getRoundNum();
        }
        for (int tries=5; tries>=0; tries--){

            if (!bugging) {
                if (rc.canMove(toDest)) {
                    return rc.getLocation().add(toDest, 1.5f);
                } else {
                    bugging = true;
                    MapLocation move = rc.getLocation().add(toDest, 1.5f);
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
                        Lumberjack.pickDest();
                        lastMinUpdate=rc.getRoundNum();
                    }
                    if (bugging)
                        left=toDest.degreesBetween(rc.getLocation().directionTo(prevLoc))>0;
                }
            }

            if (bugging) {
                float destdist = rc.getLocation().add(toDest, 2).distanceTo(dest);
                if (rc.canMove(toDest) && destdist < bugMinDist) {
                    bugging = false;
                    return rc.getLocation().add(toDest, 1.5f);
                } else {
                    BodyInfo following = treeOrNot ? (rc.canSenseTree(bugTree) ? rc.senseTree(bugTree) : null) : (rc.canSenseRobot(bugTree) ? rc.senseRobot(bugTree) : null);

                    if (following == null || following.getLocation() != prevLoc) {
                        bugging = false;
                    } else {
                        float distBtw = rc.getLocation().distanceTo(following.getLocation());
                        float cosp = ((following.getRadius() + 1) * (following.getRadius() + 1) - 2.25f - distBtw * distBtw) / (3 * distBtw);
                        float f = (float) Math.acos(cosp);
                        Direction ndir = new Direction(rc.getLocation().directionTo(following.getLocation()).radians  +(left? -f- 0.005f:f + 0.005f));
                        MapLocation theMove=rc.getLocation().add(ndir, 1.5f);
                        if (rc.canSenseAllOfCircle(theMove, 1) && rc.onTheMap(theMove,1)) {
                            if (hitWall) {
                                //System.out.println("YAYY!");
                                Lumberjack.pickDest();
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
        if (rc.getRoundNum()-lastMinUpdate>150) {
            Archon.pickDest();
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
                        Archon.pickDest();
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
}