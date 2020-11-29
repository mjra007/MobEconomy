package uk.enchantedoasis.mobeconomy.mobeconomy;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.entity.EntityType;

import java.util.HashMap;

@ConfigSerializable
public class MainConfig {

    @Setting(comment = "Mobs drop")
    public HashMap<EntityType, Double> mobsMoneyDrop = new HashMap<>();

    public MainConfig(HashMap<EntityType, Double> mobsMoneyDrop){
        this.mobsMoneyDrop = mobsMoneyDrop;
    }

    public MainConfig(){}

}
