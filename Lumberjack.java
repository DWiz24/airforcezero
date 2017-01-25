package airforcezero;
import battlecode.common.*;

import java.util.Random;
import java.lang.Math;

public class Lumberjack {
    //global stuff

    static final boolean DEBUG1 = false, DEBUG2 = false;  //set to false to make them shut up

    //constants
    private static final int MIN_GARDENER_RANGE = 5, MAX_GARDENER_RANGE = 7;    //if protecting gardeners, will try to stay between these distances away from them
    private static final float MAX_TREE_UNCERT = 0.5f;   //maximum uncertainty when reporting trees

    //general
    private static RobotController rc;
    private static Random rng;

    //variables related to my robot's behavior
    private static MapLocation prevGardenerPos;   //previous gardener sighting
    private static boolean stayNear, isIdle, foundGardener, firstRound, printedThisTurn;

    //info about my tree target
    private static TreeInfo treeInfo;  //used when tree is seen
    private static MapLocation treeLoc, nearTreeLoc; //can be an approximation | used for approaching
    private static boolean nearTree;    //if able to chop tree
    private static int treeChannel;

    //channels
    private static int prevTreeNext, treeNext = -1; //previous and current turn next tree channel to write to

    //info about my surroundings, updates every turn
    private static RobotInfo[] allRobots, friendlyRobots, enemyRobots, friendlyGardeners;
    private static TreeInfo[] allTrees, friendlyTrees, neutralTrees, enemyTrees;
    private static int friendlyRobotCount, enemyRobotCount, friendlyTreeCount, neutralTreeCount, enemyTreeCount, friendlyGardenerCount;
    
    /*
    -2/3 of lumberjacks wander off to search for trees
    -Trees found, report them
        -Cuts down tree
    -If nothing to chop, wander/stay near gardeners
    -If trees reported, choose one and chop
        
    -TODO:
    -strategic determination of which trees to chop
    -Choosing a good location to stand while cutting trees
    -(Important) running away
    -bullet dodging
    -combat (if necessary) + calling combat
    */
    public static void run(RobotController rcArg) throws GameActionException{
        //general
        rc = rcArg;
        rng = new Random(rc.getID());

        //variables related to my robot's behavior
        prevGardenerPos = rc.getInitialArchonLocations(rc.getTeam())[0];
        stayNear = false;
        printedThisTurn = false;
        if(DEBUG1){
            System.out.print("\nI am a lumberjack!");
            if (rc.getID() % 3 == 0) {
                stayNear = true;
                System.out.print("\nI protect gardeners.");
            } else {
                System.out.print("\nI explore.");
            }
            printedThisTurn = true;
        }
        isIdle = true;
        foundGardener = false;
        firstRound = true;

        //info about my tree target
        setTreeTarget(null, -1);

        //channels
        treeNext = rc.readBroadcast(15);
        if (treeNext == 0) { //in case this is the first lumberjack, set the index to default
            rc.broadcast(15, 16);
            treeNext = 16;
            prevTreeNext = 16;
        }
        else {
            prevTreeNext = treeNext - 1;
            if (prevTreeNext == 15)
                prevTreeNext = 29;
        }

        //code below repeats every turn
        while (true) {
            //updating info about robots and trees around me
            updateInfo();
            prevTreeNext = treeNext;
            treeNext = rc.readBroadcast(15);

            //report + check trees
            reportTreesInRange(rc, neutralTrees, neutralTreeCount, enemyTrees, enemyTreeCount);

            //gardener update
            foundGardener = false;
            if (stayNear && friendlyGardenerCount > 0) {
                foundGardener = true;
                prevGardenerPos = friendlyGardeners[0].location;
            }

            //check for changes
            checkTreeArrayChange();

            //move, shake, and chop
            MapLocation move = determineMove();
            if(move != null)
                rc.move(move);
            shakeATree();
            if (!isIdle && nearTree) {
                chopTree();   //chop tree
            }
            
            if(printedThisTurn){
               System.out.println();
               printedThisTurn = false;
            }

            PublicMethods.donateBullets(rc);
            
            //end of while loop - yield to end
            firstRound = false;
            Clock.yield();
        }
    }

    //helper methods
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
    private static void checkTreeArrayChange() throws GameActionException{
        if (isIdle) {
            if(prevTreeNext != treeNext) {
                isIdle = false;
                chooseBestTree(prevTreeNext, treeNext);
                if(DEBUG1){
                    System.out.print("\nTree update in the array sensed. Becoming active and going after tree at " + treeLoc.x + ", " + treeLoc.y + ".");
                    printedThisTurn = true;
                }
            }
        }
        else{
            int diff = treeNext - treeChannel;
            if(rc.readBroadcast(treeChannel) == 0){
                setTreeTarget(null, -1);
                if(rc.readBroadcast(treeNext) == 0 && (diff == 1 || diff == -13)){
                    if(DEBUG1) {
                        System.out.print("\nTree target was removed and no other targets detected. Becoming idle.");
                        printedThisTurn = true;
                    }
                    isIdle = true;
                    if(stayNear)
                        Nav.setDest(prevGardenerPos);   //edit me later
                    else{
                        pickDest(true);
                    }
                }
                else{
                    chooseBestTree();
                    if(DEBUG1) {
                        System.out.print("\nTree target was removed. Going after tree at " + treeLoc.x + ", " + treeLoc.y + ".");
                        printedThisTurn = true;
                    }
                }
            }
        }
    }
    private static TreeInfo findTree(MapLocation approx, float uncert){
        //finds tree within max uncertainty of approx
        for(TreeInfo info : enemyTrees){
            if(info == null)
                break;

            float dx = info.location.x - approx.x;
            if(dx < 0)
                dx = 0 - dx;

            float dy = info.location.y - approx.y;
            if(dy < 0)
                dy = 0 - dy;

            float dmax = dx;
            if(dy > dx)
                dmax = dy;

            if(dmax <= MAX_TREE_UNCERT)   //if goes below max uncert limit
                return info;
        }
        for(TreeInfo info : neutralTrees){
            if(info == null)
                break;

            float dx = info.location.x - approx.x;
            if(dx < 0)
                dx = 0 - dx;

            float dy = info.location.y - approx.y;
            if(dy < 0)
                dy = 0 - dy;

            float dmax = dx;
            if(dy > dx)
                dmax = dy;

            if(dmax <= uncert)   //if goes below max uncert limit
                return info;
        }
        return null;
    }
    private static void chopTree() throws GameActionException{
        //update health
        if(rc.canSenseLocation(treeInfo.location))   //make faster later
            treeInfo = rc.senseTreeAtLocation(treeInfo.location);
        else
            treeInfo = findTree(treeInfo.location, MAX_TREE_UNCERT);

        //chops trees
        boolean reset = false;
        if (treeInfo.getHealth() <= GameConstants.LUMBERJACK_CHOP_DAMAGE) {
            if(DEBUG1) {
                System.out.print("\nChopped down my target!");
                printedThisTurn = true;
            }
            rc.broadcast(treeChannel, 0);
            reset = true;

            //not using setTreeTarget because still need to keep treeInfo
            treeLoc = null;
            nearTree = false;
        }
        rc.chop(treeInfo.getID());
        if(reset)
            treeInfo = null;
    }
    private static float expandingPriorityThreshold(){
        //950 - 800
        return 950f - (rc.getRoundNum())/20f;
    }

    //movement stuff
    private static MapLocation determineMove() throws GameActionException{
        if(nearTree)    //base case: stand still while chopping
            return null;

        if(isIdle){
            if(stayNear && foundGardener){
                //staying in range of gardeners code
                float distance = rc.getLocation().distanceTo(prevGardenerPos);
                if(distance > MAX_GARDENER_RANGE) {
                    Direction d = rc.getLocation().directionTo(prevGardenerPos);
                    if(rc.canMove(d)) {
                        if(DEBUG2) {
                            System.out.print("\nMoved towards gardener.");
                            printedThisTurn = true;
                        }
                        return rc.getLocation().add(d);
                    }
                }
                else if(distance < MIN_GARDENER_RANGE){
                    Direction d = prevGardenerPos.directionTo(rc.getLocation());
                    if(rc.canMove(d)) {
                        if (DEBUG2) {
                            System.out.print("\nMoved away from gardener.");
                            printedThisTurn = true;
                        }
                        return rc.getLocation().add(d);
                    }
                }
                for (int i = 0; i < 10; i++) {    //prevents infinite while loops if stuck
                    Direction d = new Direction(rng.nextFloat() * 2 * (float) Math.PI);
                    if (rc.canMove(d)) {
                        if (DEBUG2) {
                            System.out.print("\nMoved in range of gardener.");
                            printedThisTurn = true;
                        }
                        return rc.getLocation().add(d);
                    }
                }
            }
        }
        else{
            //update tree stuff
            float d = rc.getLocation().distanceTo(treeLoc);
            if(treeInfo == null && d <= RobotType.LUMBERJACK.sensorRadius + GameConstants.NEUTRAL_TREE_MAX_RADIUS){  //17
                treeInfo = findTree(treeLoc, MAX_TREE_UNCERT);  //check tree
                if(treeInfo != null) {
                    if(DEBUG2) {
                        System.out.print("\nSaw my tree!");
                        printedThisTurn = true;
                    }
                    treeLoc = treeInfo.location;
                    Nav.setDest(treeLoc);
                }
            }
            if(treeInfo != null && d <= RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + treeInfo.radius) //2.25-11.75
                pickNearTreeLoc(treeInfo);
        }
        return Nav.lumberjackNav(rc, allTrees, allRobots);
    }
    private static void pickNearTreeLoc(TreeInfo tree) throws GameActionException{
        //Does exactly what it says.
        //Takes over approaching trees when close (dist <= 1 + maxTreeRadius).
        //TODO: make gardeners leave room for others.
        if(nearTreeLoc == null){
            nearTreeLoc = tree.location.add(tree.location.directionTo(rc.getLocation()), tree.radius + RobotType.LUMBERJACK.bodyRadius + 0.001f);
            Nav.setDest(nearTreeLoc);
        }
    }

    //tree handling stuff
    public static void reportTreesInRange(RobotController rc, TreeInfo[] neutral, int neutralCount, TreeInfo[] enemy, int enemyCount) throws GameActionException{
        //Please use this method!
        //specify the arrays of and numbers of neutral and enemy trees within your vision,
        //and this method will handle everything else
        if (enemyCount + neutralCount > 0) {
            if(rc.getType() == RobotType.LUMBERJACK)
                treeNext = rc.readBroadcast(15);

            //for later reference
            Integer[] locationValueArray = new Integer[enemyCount + neutralCount];
            TreeInfo[] treeArray = new TreeInfo[enemyCount + neutralCount];

            //goes through all trees and combines arrays, while computing int from location
            int index = 0;
            for (TreeInfo info : enemy) {
                if(index > 5)
                    break;
                if(info == null)
                    break;
                locationValueArray[index] = locationToInt(info.location);
                treeArray[index] = info;
                index++;
            }
            for (TreeInfo info : neutral) {
                if(index > 5)
                    break;
                if(info == null)
                    break;
                locationValueArray[index] = locationToInt(info.location);
                treeArray[index] = info;
                index++;
            }
            //here it checks if trees have already been reported
            if (rc.getType() != RobotType.LUMBERJACK || (firstRound || !isIdle || prevTreeNext != treeNext)) {  //the beginning of the game, or trees already in array (for lumberjacks)
                for (int i = 16; i < 30; i++) {
                    int existingTreeLocationVal = isolateLocation(rc.readBroadcast(i));
                    for (int i2 = 0; i2 < locationValueArray.length; i2++) {
                        Integer newTreeLocationVal = locationValueArray[i2];
                        if (newTreeLocationVal != null) {
                            if (newTreeLocationVal == existingTreeLocationVal)
                                locationValueArray[i2] = null;   //removes tree to report
                        }
                    }
                }
            }

            float threshold = expandingPriorityThreshold();
            for (int i = 0; i < locationValueArray.length; i++) {
                Integer locationValue = locationValueArray[i];
                if (locationValue == null)
                    continue;
                int staticPriority = staticPriorityOfTree(treeArray[i]);
                int priority = (int)dynamicPriorityFromBase(treeArray[i]) + staticPriority;
                if(priority < threshold)
                    continue;
                rc.broadcast(treeNext, priorityToInt(staticPriority) + locationValue);
                if(DEBUG1) {
                    System.out.print("\nReported tree " + treeArray[i].getID() + " to array location " + treeNext + ".");
                    printedThisTurn = true;
                }
                treeNext++;
                if(treeNext == 30)
                    treeNext = 16;
            }
            rc.broadcast(15, treeNext);
        }
    }
    private static void chooseBestTree() throws GameActionException{
        chooseBestTree(16, 29, null);
    }
    private static void chooseBestTree(MapLocation exclude) throws GameActionException{
        chooseBestTree(16, 29, exclude);
    }
    private static void chooseBestTree(int lower, int upper) throws GameActionException{
        chooseBestTree(lower, upper, null);
    }
    private static void chooseBestTree(int lower, int upper, MapLocation exclude) throws GameActionException{
        int bestPriority = -1;
        int bestChannel = -1;
        int bestValue = -1;

        for(int i = lower; i < upper+1; i++){
            int value = rc.readBroadcast(i);
            if(value == 0)
                continue;

            //check for exclusion
            if(exclude != null) {
                MapLocation existing = intToLocation(value);
                float dx = existing.x - exclude.x;
                if (dx < 0)
                    dx = 0 - dx;
                float dy = existing.y - exclude.y;
                if (dy < 0)
                    dy = 0 - dy;
                if (dx < MAX_TREE_UNCERT && dy < MAX_TREE_UNCERT)
                    continue;
            }

            int priority = intToPriority(value) + dynamicPriorityOfTree(intToLocation(value));

            if(priority > bestPriority){
                bestPriority = priority;
                bestChannel = i;
                bestValue = value;
            }
        }

        setTreeTarget(bestValue, bestChannel);
    }   //with limits
    private static int staticPriorityOfTree(TreeInfo tree){
        int priority = 0;

        //enemy trees
        if(tree.getTeam() == rc.getTeam().opponent())
            priority += 500;
        //bullets
        int b = tree.containedBullets;
        if(b > 1000)
            b = 1000;
        priority += b / 4;

        //contained robot
        if(tree.containedRobot != null)
        switch(tree.containedRobot){
            case ARCHON:
                priority += 10;    //314
                break;
            case GARDENER:
                priority += 9;  //300
                break;
            case SOLDIER:
                priority += 5;  //150
                break;
            case LUMBERJACK:
                priority += 5;  //150
                break;
            case SCOUT:
                priority += 3;  //100
                break;
        }

        return priority;
    }
    private static int dynamicPriorityOfTree(TreeInfo treeInfo){
        //expanding circle goes from ~7 to ~28 distance
        int priority = 0;

        priority += 0.25f * dynamicPriorityFromBase(treeInfo);
        priority += 0.75f * dynamicPriorityFromMe(treeInfo);

        return priority;
    }
    private static int dynamicPriorityOfTree(MapLocation treeLoc){
        //expanding circle goes from ~7 to ~28 distance
        int priority = 0;

        priority += 0.25f * dynamicPriorityFromBase(treeLoc);
        priority += 0.75f * dynamicPriorityFromMe(treeLoc);

        return priority;
    }
    private static float dynamicPriorityFromBase(TreeInfo treeInfo){
        return 7.071067812f * (141.4213562f - prevGardenerPos.distanceTo(treeInfo.location) + treeInfo.radius);
    }
    private static float dynamicPriorityFromMe(TreeInfo treeInfo){
        return 7.071067812f * (141.4213562f - rc.getLocation().distanceTo(treeInfo.location) + treeInfo.radius);
    }
    private static float dynamicPriorityFromBase(MapLocation treeLoc){
        return 7.071067812f * (141.4213562f - prevGardenerPos.distanceTo(treeLoc) + 0.5f);
    }
    private static float dynamicPriorityFromMe(MapLocation treeLoc){
        return 7.071067812f * (141.4213562f - rc.getLocation().distanceTo(treeLoc) + 0.5f);
    }
    private static void setTreeTarget(TreeInfo tree, int channel){
        treeInfo = tree;
        nearTree = false;
        nearTreeLoc = null;
        if(tree == null){
            treeLoc = null;
            //not changing treeChannel cause using it later
        }
        else{
            treeLoc = tree.location;
            treeChannel = channel;
        }
        Nav.setDest(treeLoc);
    }
    private static void setTreeTarget(int value, int channel){  //alternative way of setting target by integer value
        treeInfo = null;
        nearTree = false;
        nearTreeLoc = null;
        treeLoc = intToLocation(value);
        treeChannel = channel;
        Nav.setDest(treeLoc);
    }
    //conversions
    private static int isolateLocation(int i){
        return i % 1000000;
    }
    private static int isolatePriority(int i){
        return i / 1000000 * 1000000;
    }
    private static int locationToInt(MapLocation loc){
        return 1000 * round(loc.x) + round(loc.y);
    }
    private static int priorityToInt(int p){
        return 1000000 * p;
    }
    private static MapLocation intToLocation(int i){
        return new MapLocation((i / 1000) % 1000, i % 1000);
    }
    private static int intToPriority(int i){
        return i / 1000000;
    }

    //integration methods
    public static void pickDest(boolean reached) throws GameActionException{
        if(DEBUG2) {
            if (reached)
                System.out.print("\nReached my destination!");
            else
                System.out.print("\nGot stuck!");
            printedThisTurn = true;
        }

        if(isIdle) {
            Nav.setDest(rc.getLocation().add(new Direction(rng.nextFloat() * 2 * (float) Math.PI), 20));   //explorer code
        }
        else{
            if(reached) { //reached tree to chop - stay in place and chop
                nearTree = true;
            }
            else{
                chooseBestTree(treeLoc);
                Nav.setDest(treeLoc);
                if(DEBUG1) {
                    System.out.print("\nGoing after tree at " + treeLoc.x + ", " + treeLoc.y + ".");
                    printedThisTurn = true;
                }
            }
        }
    }
    private static void shakeATree() throws GameActionException {   //shakes a tree within range
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

    //methods to preserve bytecodes
    private static int round(float a){   //should take less bytecodes than Math.round
        return (int) (a+0.5f);
    }
}