package domain.game;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.Collections;

import domain.maps.ClassicMap;
import domain.player.Player;
import domain.game.config.GameConfig;
import domain.gamemap.GameMap;
import domain.util.CoreUtils;
//import domain.card.ChanceCard;
import domain.mapstate.MapState;
import domain.mapstate.TerritoryState;

public class Game {

    private static GameConfig config;
    private MapState mapState;
    private final GameMap map = new ClassicMap();
    private final List<Player> players = new ArrayList<>();
    private int playerCount;
    private Phase phase = Phase.Draft;
    private Player currentplayer;

    private int phaseCounter;
    private int turnCounter;
    private int roundCounter;
    private int draftArmies;
    //private ChanceCard currentcard;
    private ArrayList<TerritoryState> initialTerrDistrubution;
    private Dice dice = new Dice();
    private TerritoryState aiTerr;

    public void nextPhase() {
            if(phase == Phase.Draft) {
                phase = Phase.Attack;
            } else if (phase == Phase.Attack) {
                phase = Phase.Fortify;
            } else if (phase == Phase.Fortify) {
                phase = Phase.Draft;
                if (players.indexOf(currentplayer) == players.size()){
                    roundCounter++;
                }
                currentplayer = players.get((players.indexOf(currentplayer) + 1) % players.size());
                doDraftPhase();
            }
    }

    private static class GameContainer {
        private static Game instance;
    }

    public static Game getInstance() {
        if (GameContainer.instance == null) {
            GameContainer.instance = new Game();
        }
        return GameContainer.instance;
    }

    public static void destroyInstance() {
        GameContainer.instance = null;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getRoundCount() {
        return roundCounter;
    }

    private Game() {
        findAndSetConfig();
        initializePlayers();
    }

    public Phase getPhase() {
        return phase;
    }

    private void initializePlayers() {
        IntStream.range(0, config.getInitialPlayers())
                .forEach(i -> addPlayer());
    }

    public void doDraftPhase() {
        if (currentplayer.getFullName().equals("ai")){
            aiDraft();
        }
            draftArmies = Math.floorDiv(currentplayer.getTerritoryCount(), 2);

    }

    public void aiDraft(){
        System.out.println("hi");
        draftArmies = Math.floorDiv(currentplayer.getTerritoryCount(), 2);
        aiTerr = CoreUtils.chooseRandom(currentplayer.getTerritories());
            List<TerritoryState> neighbors = mapState.getNeighborsOf(aiTerr);
            int bugcount =0;
            while(true){
                bugcount++;
                for(TerritoryState neighbor : neighbors){
                    if(!neighbor.getOwner().getFullName().equals("ai")){
                        break;
                    }
                }
                aiTerr = CoreUtils.chooseRandom(currentplayer.getTerritories());
                neighbors = mapState.getNeighborsOf(aiTerr);
                if (bugcount==39) break;
            }
        while(draftArmies>0){
            setDraftArmies(aiTerr);
        }
        nextPhase();
    }
    private void aiAttack(){

    }

    public int getDraftArmies() {
        return draftArmies;
    }

    public void setDraftArmies(TerritoryState state) {
        if (draftArmies > 0) {
            state.addArmies(1);
            draftArmies--;
        }
    }

    public Player getCurrentplayer(){ return currentplayer; }

    public void createGameMap() {
        map.createMap();
        mapState = MapState.createInstance(map);
    }

    public Player addPlayer() {
        if (getPlayersCount() < config.getMaximumPlayers()) {
            Player player = new Player();
            players.add(player);
            return player;
        }
        return null;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPlayersCount() {
        return players.size();
    }

    public void shufflePlayers() {
        Collections.shuffle(players);
    }

    public void selectTerritories() {
       mapState.getTerritoryStates().forEach(territory ->{
           if(territory.isPlayable() && (territory.getOwner()==null)){
               List<TerritoryState> neighbors = mapState.getNeighborsOf(territory);
               Player player = currentplayer;
               player.addTerritory(territory);
               territory.setOwner(player);
               for(TerritoryState neighbor : neighbors){
                   if(neighbor.isPlayable() && (neighbor.getOwner()==null)){
                       player.addTerritory(neighbor);
                       neighbor.setOwner(player);
                   }
               }
           }
       });
    }

    public void shareTerritories() {
        players.forEach(player -> {
            player.setNumberOfTerritories(0);
        });
        int territorycount = Math.floorDiv(mapState.getTerritoryStates().size(), players.size());
        mapState.getTerritoryStates().forEach(territory -> {
            Player player = CoreUtils.chooseRandom(players);
            if (territory.isPlayable()  && territorycount>player.getNumberOfTerritories() && territory.getOwner()==null) {
                player.addTerritory(territory);
                player.increaseNumberOfTerritories();
                territory.setOwner(player);
                List<TerritoryState> neighbors = mapState.getNeighborsOf(territory);
                for (TerritoryState neighbor : neighbors) {
                    if(territorycount>player.getNumberOfTerritories() && neighbor.getOwner()==null ){
                        player.addTerritory(neighbor);
                        neighbor.setOwner(player);
                        player.increaseNumberOfTerritories();
                    }

                }
            }
        });
//        for (int i =0; i< players.size(); i++){
//
//            System.out.println(players.get(i).getFullName());
//            System.out.println(players.get(i).getNumberOfTerritories());
//        }
        mapState.getTerritoryStates().forEach(terr ->{
            if(terr.getOwner()==null){
                players.forEach(player -> {
                    if(player.getNumberOfTerritories()<territorycount){
                        player.addTerritory(terr);
                        player.increaseNumberOfTerritories();
                        terr.setOwner(player);
                    }
                });
                if(terr.getOwner()==null){
                    // if we want everyone to have same number of territories and armies
                    //terr.setPlayable(false);
                    // if we want to give all enabled territories to random players
                    Player luckyPlayer = CoreUtils.chooseRandom(players);
                    luckyPlayer.addTerritory(terr);
                    luckyPlayer.increaseNumberOfTerritories();
                    terr.setOwner(luckyPlayer);



                }
            }

        });
//        for (int i =0; i< players.size(); i++){
//
//            System.out.println(players.get(i).getFullName());
//            System.out.println(players.get(i).getNumberOfTerritories());
//        }

        int armies = getInitialArmies();
        mapState.getTerritoryStates().forEach(state -> {
            if(state.getOwner()!=null){
                state.setArmies(Math.floorDiv(armies,state.getOwner().getNumberOfTerritories()));
            }
        });

        currentplayer = players.get(0);
        doDraftPhase();
    }



    public int getInitialArmies(){
        int armies;
        int players = getPlayerCount();
        switch(players) {
            case 2:
                armies = 40;
                break;
            case 3:
                armies = 35;
                break;
            case 4:
                armies = 30;
                break;
            case 5:
                armies = 25;
                break;
            case 6:
                armies = 20;
                break;
            default:
                armies = 40;
                break;
        }
        return armies;
    }

    public void attackPhase(TerritoryState attack, TerritoryState defence) {
        if (currentplayer.getFullName().equals("ai")){
            aiAttack();
        }
        if(attack.getArmies() > defence.getArmies() && attack.getArmies()>2){
            int attackDice = Dice.roll();
            int defenceDice = Dice.roll();
            if(defenceDice <= attackDice){
                defence.setArmies(defence.getArmies()-1);
                if(defence.getArmies()==0){
                    defence.setOwner(attack.getOwner());
                    defence.setArmies(1);
                    attack.setArmies(attack.getArmies()-1);
                    attack.getOwner().increaseNumberOfTerritories();

                }
            }else{
                attack.setArmies(attack.getArmies()-2);
            }
        }


//        boolean condition;
        //This could be work on clicked
//        if (startLocation.getArmies() > attackLocation.getArmies()){
//            condition = true;
//            while(condition){
//                //Roll the dice
//                boolean diceValue;
//                int diceValueStart = dice.roll();
//                int diceValueAttack = dice.roll();
//                if(diceValueStart > diceValueAttack){
//                    diceValue = true;
//                }
//                else{
//                    diceValue = false;
//                }
//                //Check the dice condition
//                if (diceValue){//To be true if start location won.
//                    attackLocation.setArmies(attackLocation.getArmies()-1);
//                }
//                else {
//                    startLocation.setArmies(startLocation.getArmies() - 1);
//                }
//                //Check the current value of territories
//                if (startLocation.getArmies() <= attackLocation.getArmies()){
//                    condition = false;
//                    break;
//                }
//                //Look at the current condition.
//                //If wanted add a click here.
//                if (startLocation.getArmies() > attackLocation.getArmies()){
//                    condition = true;
//                }
//            }
//        }
    }

    public void fortifyPhase(TerritoryState from, TerritoryState to) {
        if (from.getArmies()>1 && from.getOwner()==to.getOwner()) {
            from.setArmies(from.getArmies()-1);
            to.setArmies(to.getArmies()+1);
        }
    }

    private void findAndSetConfig() {
        try {
            config = GameConfig.scanConfig();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
