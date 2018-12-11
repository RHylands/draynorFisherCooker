

import org.osbot.rs07.api.filter.Filter;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;


@ScriptManifest(author = "GearsBy", info = "fish n cook", name = "Draynor Fisher Cooker", version = 0.1, logo = "")
public class draynorFisherCooker extends Script {


    enum State {
            FISHING,
            COOKING,
            BANKING,
            DROPING,
        WALKING_BANK,
        WALKING_WOODS,
        WALKING_FISH,
        ERROR
    }


    boolean banking = true;
    boolean cooking = true;

    private static final Area DRAYNOR_BANK = new Area(1,1,1,1);
    private static final Area FISHING_SPOTS = new Area(1,1,1,1);
    private static final Area WOOD_TREES = new Area(1,1,1,1);
    private static final Area WILLOW_TREES = new Area(1,1,1,1);


    private static final String[] rawFish = {"Raw shrimp", "Raw anchovies"};

    Filter<Item> keepFilter = new Filter<Item>() {
        @Override
        public boolean match(Item i) {
            return (i.getName().endsWith("axe") || i.getName().contentEquals("Small fishing net") );
        }
    };

    @Override
    public void onStart() {
        log("Let's get started!");

    }

    @Override
    public int onLoop() throws InterruptedException {

        switch (getState()){
            case FISHING:
                fish();
                break;
            case BANKING:
                bank();
                break;
            case DROPING:
                drop();
                break;
            case COOKING:
                chopAndCook();
                break;
            case WALKING_BANK:
                walkToBank();
                break;
            case WALKING_FISH:
                walkToFish();
                break;
            case WALKING_WOODS:
                walkToWoods();
                break;
            case ERROR:
                break;

        }

      return 700;
    }

    @Override
    public void onExit() {
        log("Thanks ");
    }

    private State getState(){

        if(atFishSpot()){
            if (!(getInventory().contains("Small fishing net"))){
                log("No fishing net, exting");
                return State.ERROR;
            } else {
                if (((getInventory().getEmptySlotCount() == 1) && cooking) || (getInventory().getEmptySlotCount() == 0)){
                    if(cooking){
                        return State.WALKING_WOODS;
                    } else if (banking){
                        return State.WALKING_BANK;
                    } else {
                        return State.DROPING;
                    }
                } else {
                    return State.FISHING;
                }
            }

        }

        if(atBank()){
            if (getInventory().getEmptySlotCount() < 5){
                return State.BANKING;
            } else {
                return State.WALKING_FISH;
            }

        }

        if(nearsWoods()){
            if(getInventory().contains("Raw shrimp") || getInventory().contains("Raw anchovies")){
                chopAndCook();
            } else if (getInventory().getEmptySlotCount() > 3 ){
                return State.WALKING_FISH;
            } else if (!banking){
                return State.DROPING;
            } else {
                return State.WALKING_BANK;

            }

        }

        if(getInventory().getEmptySlotCount() > 4){
            return State.WALKING_FISH;
        } else if (banking){
            return State.BANKING;
        } else {
            return State.DROPING;
        }


    }


    private boolean atFishSpot(){
        return FISHING_SPOTS.contains(myPlayer());
    }

    private boolean nearsWoods() {
        if (getSkills().getStatic(Skill.WOODCUTTING) < 30){
            return WOOD_TREES.contains(myPlayer());
        } else {
            return WILLOW_TREES.contains(myPlayer());
        }
    }

    private boolean atBank(){
        return DRAYNOR_BANK.contains(myPlayer());

    }

    private void fish(){
        if(myPlayer().isAnimating() || myPlayer().isMoving()){
            return;
        }

        if(!atFishSpot()){
            return;
        }

        NPC fish = getNpcs().closest(o -> o.getName().equals("Fishing spot"));
        if (fish!= null){
            fish.interact("Net");
        }

    }

    private void chopAndCook(){
        if(!nearsWoods()){
            log("Not in woods :(");
            return;
        }

        if (!inventory.contains("Tinderbox") || !(inventory.contains("Raw anchovies")||inventory.contains("Raw shrimp")) ){
            log("Missing tinder or raw fish");
            return;
        }

        if(!(inventory.getEmptySlotCount() > 0)){
            //no room please drop something
        }

        String treeType = "";
        String logType = "";
        if(getSkills().getStatic(Skill.WOODCUTTING) < 30){
              treeType = "Tree";
              logType = "Logs";
        } else {
              treeType = "Willow tree";
              logType = "Willow logs";
        }

        int i = 0;

        while((getInventory().contains("Raw anchovies") || getInventory().contains("Raw shrimp")) && (i<5)) {
            log("We have raw fish to cook");
            i += 1;

            RS2Object fire = getObjects().closest(rs2Object -> rs2Object.getName().equals("Fire"));

            if(!getInventory().contains("Logs") && fire == null) {
                log("Looking for tree to cut");
                Entity tree = objects.closest(treeType);
                if (tree != null) {
                    log("Cutting tree");
                    tree.interact("Chop Down");
                    final String LOG = logType;
                    new ConditionalSleep(25000, 500) {
                        @Override
                        public boolean condition() throws InterruptedException {
                            return getInventory().contains(LOG);
                        }
                    }.sleep();
                } else {
                    //didn't find tree
                    log("Couldn't find tree");
                }
            }

            if (getInventory().contains(logType)){
                inventory.interact("Use", "Tinderbox");
                log("Selecting tinderbox");
                new ConditionalSleep(2000, 100) {
                    @Override
                    public boolean condition() throws InterruptedException {
                        return (getInventory().isItemSelected());
                    }
                }.sleep();

                inventory.interact("Use", logType);
                log("Waiting for fire to light");
                final String LOG = logType;
                new ConditionalSleep(25000, 500) {
                    @Override
                    public boolean condition() throws InterruptedException {
                        return !getInventory().contains(LOG) && !myPlayer().isAnimating() && !myPlayer().isMoving()
                                ;
                    }
                }.sleep();


            }else {
                log("Don't have any logs");
            }

            for(int j =0; j<2; j++) {
                log("Looking for our fire");
                if (fire != null && fire.isVisible()) {
                    log("Found our fire");
                    if (getInventory().contains(rawFish[j])) {
                        log("Selecting chicken to cook");
                        getInventory().interact("Use", rawFish[j]);
                        new ConditionalSleep(2500, 250) {
                            @Override
                            public boolean condition() throws InterruptedException {
                                return getInventory().isItemSelected();
                            }
                        }.sleep();
                        fire.interact("Use");
                        log("Using on fire");

                        //Wait to get to fire and interact
                        sleepUntilWidget(3000, 270, 12);

                        RS2Widget optionMenu = getWidgets().get(270, 14);
                        if (optionMenu != null) {
                            if (optionMenu.isVisible()) {
                                log("Cook menu open");
                                optionMenu.interact("Cook");
                                log("Cooking ");
                                long timer = System.currentTimeMillis();
                                long startTimer = System.currentTimeMillis();
                                long timeOut = 15 * 1000;//Max 15 seconds to cook single item
                                long maxTimeOut = 3 * 60 * 1000;//3 minute max timeout
                                long rawCount = getInventory().getAmount(rawFish[j]);
                                int cookLevel = getSkills().getStatic(Skill.COOKING);

                                while (((System.currentTimeMillis() - timer) < timeOut) && (System.currentTimeMillis() - startTimer < maxTimeOut)) {
                                    if (getInventory().getAmount(rawFish[j]) != rawCount) {
                                        log("We've used a raw chicken");
                                        rawCount = getInventory().getAmount("Raw chicken");
                                        timer = System.currentTimeMillis();
                                    }


                                    //level up check
                                    if (cookLevel != getSkills().getStatic(Skill.COOKING) || dialogues.inDialogue()) {
                                        log("We have leveled up!");
                                        cookLevel = getSkills().getStatic(Skill.COOKING);
                                        if (dialogues.inDialogue()) {
                                            dialogues.clickContinue();
                                        }
                                        if (fire.exists() && getInventory().contains(rawFish[j])) {
                                            fire.interact("Use");
                                            log("Using chicken on fire");

                                            //Wait to get to fire and interact
                                            sleepUntilWidget(3000, 270, 14);


                                        } else {
                                            log("Resetting timer");
                                            timer = 0; //force end of while as no more fire :/
                                        }
                                    }

                                }
                                log("No longer using fire");
                            }

                        } else {
                            log("Can't find option menu (NULL)");
                        }
                    } else {
                        log("No raw chicken in inv");
                    }

                } else {
                    log("Cound'nt find fire or not visable");
                }
            }

            if(i>4){
                log("Looped too many times trying to cook: ERROR");
            }


        }


    }

    private void drop(){
        getInventory().dropAllExcept(keepFilter);
    }

    private void bank(){
        if(atBank()){
            if(!getBank().isOpen()) {

                NPC banker = npcs.closest("Banker");
                if(banker != null){
                    banker.interact("Bank");
                    new ConditionalSleep(3000){
                        @Override
                        public boolean condition() {
                            return (getBank().isOpen());
                        }
                    }.sleep();
                }
            }

            if(getBank().isOpen()){
                getBank().depositAllExcept("Tinderbox", "Bronze axe", "Iron axe", "Steel axe", "Black axe",
                        "Mithril axe", "Adamant axe", "Rune axe", "Dragon axe", "Small fishing net");
                new ConditionalSleep(3000){
                    @Override
                    public boolean condition() throws InterruptedException {
                        return!(getInventory().contains("Raw shrimp") || getInventory().contains("Cooked shrimp"));
                    }
                }.sleep();

                if(getBank().isOpen()){
                    getBank().close();
                    new ConditionalSleep(2000){
                        @Override
                        public boolean condition() throws InterruptedException {
                            return getBank().isOpen();
                        }
                    }.sleep();
                }
            }
        }
    }
    private void sleepUntilWidget(int time, int rootID, int childID){
        new ConditionalSleep(time) {
            @Override
            public boolean condition() throws InterruptedException {
                RS2Widget optionMenu = getWidgets().get(rootID, childID);
                return !(optionMenu == null); // must be a better way?
            }
        }.sleep();
    }

    private void walkToFish(){
        getWalking().webWalk(FISHING_SPOTS);
    }

    private void walkToWoods(){
        if(getSkills().getStatic(Skill.WOODCUTTING) < 30){
            getWalking().webWalk(WOOD_TREES);
        } else {
            getWalking().webWalk(WILLOW_TREES);
        }

    }

    private void walkToBank(){
        getWalking().webWalk(DRAYNOR_BANK);
    };

}