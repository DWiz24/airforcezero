package airforcezero;
import battlecode.common.*;

import java.util.Random;
import java.lang.Math;

public class Lumberjack {
    //global stuff
    private static final int MIN_GARDENER_RANGE = 2, MAX_GARDENER_RANGE = 6;    //if protecting gardeners, will try to stay between these distances away from them

    private static RobotController rc;
    private static Random rng;
    private static MapLocation prevGardenerPos, treeLoc;   //main and off-route destinations

    private static int prevTreeNext, treeNext, treeChannel, treeID;
    private static boolean stayNear, isIdle, foundGardener, firstRound;

    private static RobotInfo[] allRobots, friendlyRobots, enemyRobots, friendlyGardeners;
    private static TreeInfo[] allTrees, friendlyTrees, neutralTrees, enemyTrees;
    private static int friendlyRobotCount, enemyRobotCount, friendlyTreeCount, neutralTreeCount, enemyTreeCount, friendlyGardenerCount;
    
    /*
    -(Temporary)2/3 of lumberjacks wander off to search for trees
    -When a neutral(Temporary) or enemy tree is found, report it
    -If nothing to chop, wander/stay near gardeners
    -If trees reported, go to first in list, chop
    
    Logic:
    //Updating block:
    -Update info
    -If trees in my range:
        -Report
        -If previously idle:
            -Become active
            -Update goals
    -If staying near gardeners, and gardener is in range:
        -Update prev gardener position
    -If previously idle:
        -If a tree was reported:
            -Become active
            -Update goals
    -Else (previously active):
        -If the channel of tree has been marked with a 0:
            -If the next index is empty and located right next to index of tree going after:
                -Become idle
                -If stays near gardeners:
                    -Go to previous gardener location
                -Else:
                    -Continue exploring
            -Else:
                -Update dest to next tree in array
    //Moving block
    -If idle, staying near gardeners, and near gardener:
        -Stay in range
    -Else if active and tree id is known:
        -Chop the tree
    -Else:
        -Move
        -If active and moving reaches tree:
            -Find the tree
            -Chop it

    -TODO:
    -(Important) running away
    -shaking trees
    -better travel code
    -bullet dodging
    -strategic determination of which trees to chop
    -combat (if necessary)
    */
    public static void run(RobotController rcArg){
        try {
            System.out.print("\nI am a lumberjack!");
            rc = rcArg;
            rng = new Random(rc.getID());
            prevGardenerPos = rc.getInitialArchonLocations(rc.getTeam())[0];
            treeLoc = null;
            treeChannel = -1;
            treeNext = rc.readBroadcast(15);
            if (treeNext == 0) { //in case this is the first lumberjack, set the index to default
                rc.broadcast(15, 16);
                treeNext = 16;
            }
            prevTreeNext = treeNext - 1;
            if (prevTreeNext == 15)
                prevTreeNext = 16;
            treeID = -1;
            firstRound = true;
            stayNear = false; //near gardeners or exploring
            isIdle = true;   //whether the lumberjacks have trees to chop
            if (rc.getID() % 3 == 0) {
                stayNear = true;
                System.out.print("\nI protect gardeners.");
            } else {
                System.out.print("\nI explore.");
            }

            //code below repeats every turn
            while (true) {
                //updating info about robots and trees around me
                updateInfo();
                prevTreeNext = treeNext;
                treeNext = rc.readBroadcast(15);

                //check trees
                reportTreesInRange();

                //gardener update
                foundGardener = false;
                if (stayNear && friendlyGardenerCount > 0) {
                    foundGardener = true;
                    prevGardenerPos = friendlyGardeners[0].location;
                }

                //read tree broadcasts
                updateTreeTarget();

                //move
                moveAndChop();

                //end of while loop - yield to end
                firstRound = false;
                float b=rc.getTeamBullets();
                if (rc.getRoundLimit()-rc.getRoundNum()<400) {
                    rc.donate(b-(b%10));
                }
                Clock.yield();
            }
        }
        catch(GameActionException e){
            e.printStackTrace();
        }
    }
    private static void updateInfo(){
        allRobots = rc.senseNearbyRobots();
        friendlyRobots = new RobotInfo[allRobots.length];
        enemyRobots = new RobotInfo[allRobots.length];
        friendlyGardeners = new RobotInfo[allRobots.length];
        allTrees = rc.senseNearbyTrees();
        friendlyTrees = new TreeInfo[allTrees.length];
        neutralTrees = new TreeInfo[allTrees.length];
        enemyTrees = new TreeInfo[allTrees.length];
        friendlyRobotCount = 0;
        enemyRobotCount = 0;
        friendlyGardenerCount = 0;
        friendlyTreeCount = 0;
        neutralTreeCount = 0;
        enemyTreeCount = 0;
        if(rc.getTeam() == Team.A) {
            for(RobotInfo info : allRobots) {
                switch(info.team) {
                    case A:
                        friendlyRobots[friendlyRobotCount] = info;
                        friendlyRobotCount++;
                        if(info.type == RobotType.GARDENER){
                            friendlyGardeners[friendlyGardenerCount] = info;
                            friendlyGardenerCount++;
                        }
                        break;
                    case B:
                        enemyRobots[enemyRobotCount] = info;
                        enemyRobotCount++;
                }
            }
            for(TreeInfo info : allTrees){
                switch(info.team) {
                    case A:
                        friendlyTrees[friendlyTreeCount] = info;
                        friendlyTreeCount++;
                        break;
                    case B:
                        enemyTrees[enemyTreeCount] = info;
                        enemyTreeCount++;
                        break;
                    case NEUTRAL:
                        neutralTrees[neutralTreeCount] = info;
                        neutralTreeCount++;
                }
            }
        }
        else{
            for (RobotInfo info : allRobots) {
                switch (info.team) {
                    case B:
                        if(info.type == RobotType.GARDENER){
                            friendlyGardeners[friendlyGardenerCount] = info;
                            friendlyGardenerCount++;
                        }
                        friendlyRobots[friendlyRobotCount] = info;
                        friendlyRobotCount++;
                        break;
                    case A:
                        enemyRobots[enemyRobotCount] = info;
                        enemyRobotCount++;
                }
            }
            for(TreeInfo info : allTrees){
                switch(info.team) {
                    case B:
                        friendlyTrees[friendlyTreeCount] = info;
                        friendlyTreeCount++;
                        break;
                    case A:
                        enemyTrees[enemyTreeCount] = info;
                        enemyTreeCount++;
                        break;
                    case NEUTRAL:
                        neutralTrees[neutralTreeCount] = info;
                        neutralTreeCount++;
                }
            }
        }
    }
    private static void reportTreesInRange() throws GameActionException{
        if (enemyTreeCount + neutralTreeCount > 0) {
            TreeInfo target = null;
            //check for dupes
            Integer[] toReportIndex = new Integer[enemyTreeCount + neutralTreeCount];
            Integer[] IDArray = new Integer[enemyTreeCount + neutralTreeCount];
            int index = 0;
            for (TreeInfo info : enemyTrees) {
                if(info == null)
                    break;
                if(target == null)
                    target = info;
                toReportIndex[index] = 1000 * round(info.location.x) + round(info.location.y);
                IDArray[index] = info.getID();
                index++;
            }
            for (TreeInfo info : neutralTrees) {
                if(info == null)
                    break;
                if(target == null)
                    target = info;
                toReportIndex[index] = 1000 * round(info.location.x) + round(info.location.y);
                IDArray[index] = info.getID();
                index++;
            }
            if (firstRound || !isIdle) {  //the beginning of the game, or trees already in array
                for (int i = 16; i < 30; i++) {
                    int existingTreeVal = rc.readBroadcast(i);
                    for (int i2 = 0; i2 < toReportIndex.length; i2++) {
                        Integer newTreeVal = toReportIndex[i2];
                        if (newTreeVal != null) {
                            if (newTreeVal == existingTreeVal)
                                toReportIndex[i2] = null;   //removes tree to report
                        }
                    }
                }
            }

            for (int i = 0; i < toReportIndex.length; i++) {
                Integer value = toReportIndex[i];
                if (value == null)
                    continue;
                rc.broadcast(treeNext, value);
                System.out.print("\nReported tree " + IDArray[i] + " to array location " + treeNext + ".");
                treeNext++;
                if(treeNext == 30)
                    treeNext = 16;
            }
            rc.broadcast(15, treeNext);

            if (isIdle) {
                System.out.print("\nBecoming active and going after tree " + target.getID() + ".");
                isIdle = false;
                treeChannel = prevTreeNext;
                treeLoc = target.location;
                Nav.setDest(treeLoc);
            }
        }
    }
    private static void reportTree(TreeInfo t) throws GameActionException{
        //IMPORTANT NOTE:
        //If using this method in your code, make a global private static int treeNext
        //Also, copy over the method directly after this one
        for(int i = 16; i < 30; i++) {
            if(round(t.location.x) == rc.readBroadcast(i)/1000 && round(t.location.y) == rc.readBroadcast(i))
                return;
        }

        treeNext = rc.readBroadcast(15);

        int value = 0;  //value to later report
        value += 1000 * round(t.location.x);
        value += round(t.location.y);

        rc.broadcast(treeNext, value);
        System.out.print("\nReported tree " + t.getID() + " to array location " + treeNext + ".");
        treeNext++;
        if(treeNext == 30)
            treeNext = 16;

        rc.broadcast(15, treeNext);
    }
    private static int round(double a){ //I'M CRYING RIGHT NOW,-DANIEL
        //Math.round(double a), but takes less than 10 bytecodes (hopefully).
        /*
        int base = (int)a;
        if(a - base > 0.5)
            return base+1;
        return base;
        */
        return (int) (a+0.5); //much better
    }
    private static void updateTreeTarget() throws GameActionException{
        if (isIdle) {
            if(prevTreeNext != treeNext) {
                isIdle = false;
                int target;
                target = rc.readBroadcast(prevTreeNext);
                System.out.print("\nTree update in the array sensed. Becoming active and going after tree at " + target);
                if(target != 0) {
                    treeChannel = prevTreeNext;
                    treeLoc = new MapLocation(target / 1000, target % 1000);
                    Nav.setDest(treeLoc);
                }
            }
        }
        else{
            int diff = treeNext - treeChannel;
            if(rc.readBroadcast(treeChannel) == 0){
                treeID = -1;
                if(rc.readBroadcast(treeNext) == 0 && (diff == 1 || diff == -13)){
                    System.out.print("\nTree target was removed and no other targets detected. Becoming idle.");
                    isIdle = true;
                    if(stayNear)
                        Nav.setDest(prevGardenerPos);
                    else{
                        pickDest();
                    }
                }
                else{
                    int position = treeChannel + 1;
                    int newTree = -1;
                    for(int i = 0; i < 15; i++){    //prevents infinite loops
                        if(position == 30)
                            position = 16;

                        newTree = rc.readBroadcast(position);
                        if(newTree != 0)
                            break;

                        position++;
                    }
                    System.out.print("\nTree target was removed. Going after tree at " + newTree);
                    treeChannel = position;
                    treeLoc = new MapLocation(newTree/1000, newTree%1000);
                    Nav.setDest(treeLoc);
                }
            }
        }
    }
    private static void moveAndChop() throws GameActionException{
        if(isIdle && stayNear && foundGardener){
            double distance = rc.getLocation().distanceTo(prevGardenerPos);
            if(distance > MAX_GARDENER_RANGE) {
                Direction d = rc.getLocation().directionTo(prevGardenerPos);
                if(rc.canMove(d)){
                    rc.move(d);
                    return;
                }
            }
            else if(distance < MIN_GARDENER_RANGE){
                Direction d = prevGardenerPos.directionTo(rc.getLocation());
                if(rc.canMove(d)){
                    rc.move(d);
                    return;
                }
            }
            for(int i = 0; i < 10; i++){    //prevents infinite while loops if stuck
                Direction d = new Direction(rng.nextFloat() * 2 * (float)Math.PI);
                if(rc.canMove(d)){
                    rc.move(d);
                    break;
                }
            }
        }
        else if(!isIdle && treeID != -1) {
            if (rc.canChop(treeID)) {
                if (rc.senseTree(treeID).getHealth() <= GameConstants.LUMBERJACK_CHOP_DAMAGE) {
                    System.out.print("\nChopped down my target!");
                    rc.broadcast(treeChannel, 0);
                }
                rc.chop(treeID);
            }
        }
        else{
            rc.move(Nav.lumberjackNav(rc, allTrees, allRobots));     //bugging
            if(!isIdle && rc.getLocation().distanceTo(treeLoc)<=4) {
                TreeInfo[] trees1 = rc.senseNearbyTrees(treeLoc, 1, rc.getTeam().opponent());
                TreeInfo[] trees2 = rc.senseNearbyTrees(treeLoc, 1, Team.NEUTRAL);
                if(trees1.length + trees2.length > 0) {
                    if(trees1.length == 0)
                        treeID = trees2[0].getID();
                    else
                        treeID = trees1[0].getID();
                    if (rc.canChop(treeID)) {
                        if(rc.senseTree(treeID).getHealth() <= GameConstants.LUMBERJACK_CHOP_DAMAGE) {
                            System.out.print("\nChopped down my target!");
                            rc.broadcast(treeChannel, 0);
                        }
                        rc.chop(treeID);
                    }
                }
            }
        }
    }
    public static void pickDest() {
        Nav.setDest(rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float)Math.PI), 10));
    }
}
