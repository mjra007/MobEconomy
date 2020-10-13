package uk.enchantedoasis.mobeconomy.mobeconomy;

import com.github.mjra007.DynamicDataStorage.DataKey;

public interface DataKeys {

    /*
        All mob drops will be multiplied by this value
     */
     DataKey<Double> PLAYER_MOB_DROP_MULTIPLIER = DataKey.makeKeyFor(Double.TYPE, "MOB_DROP_MULTIPLIER");

}
