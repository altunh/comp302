package domain.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.Collections;

import domain.gamemap.TerritoryType;
import domain.maps.ClassicMap;
import domain.player.Player;
import domain.game.config.GameConfig;
import domain.gamemap.GameMap;
import domain.util.CoreUtils;
import domain.card.ChanceCard;
import domain.mapstate.MapState;
import domain.mapstate.TerritoryState;
import org.jetbrains.annotations.NotNull;

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
    private ChanceCard currentcard;
    private ArrayList<TerritoryState> initialTerrDistrubution;
    private Dice dice = new Dice();

    private TerritoryState selectedTerritory;
    private

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

    public void createGameMap() {
        map.createMap();
        mapState = MapState.createInstance(map);
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
                player.increaseNumberOfTerrirtories();
                territory.setOwner(player);
                List<TerritoryState> neighbors = mapState.getNeighborsOf(territory);
                for (TerritoryState neighbor : neighbors) {
                    if(territorycount>player.getNumberOfTerritories() && neighbor.getOwner()==null ){
                        player.addTerritory(neighbor);
                        neighbor.setOwner(player);
                        player.increaseNumberOfTerrirtories();
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
                        player.increaseNumberOfTerrirtories();
                        terr.setOwner(player);
                    }
                });
                if(terr.getOwner()==null){
                    // if we want everyone to have same number of territories and armies
                    //terr.setPlayable(false);
                    // if we want to give all enabled territories to random players
                    Player luckyPlayer = CoreUtils.chooseRandom(players);
                    luckyPlayer.addTerritory(terr);
                    luckyPlayer.increaseNumberOfTerrirtories();
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

    public void attackPhase(@NotNull TerritoryState attack, @NotNull TerritoryState defence) {

        if(attack.getArmies() > defence.getArmies() && attack.getArmies()>2) {
            int attackDice = Dice.roll();
            int defenceDice = Dice.roll();
            if(defenceDice <= attackDice){
                defence.setArmies(defence.getArmies()-1);
                if(defence.getArmies()==0){
                    defence.setOwner(attack.getOwner());
                    defence.setArmies(1);
                    attack.setArmies(attack.getArmies()-1);
                }
            }else{
                attack.setArmies(attack.getArmies()-2);
            }
        }

    }

    public void fortifyPhase(TerritoryState from, TerritoryState to) {
        if (from.getArmies()>1 && from.getOwner()==to.getOwner()) {
            from.setArmies(from.getArmies()-1);
            to.setArmies(to.getArmies()+1);
        }
    }

}
