package uk.enchantedoasis.mobeconomy.mobeconomy;

import com.github.mjra007.sponge.Storage;
import com.github.mjra007.sponge.UsersDataBank;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import de.randombyte.holograms.api.HologramsService;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;
import uk.enchantedoasis.mobeconomy.mobeconomy.listeners.PlayerKillMob;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "mobeconomy",
        name = "mobeconomy",
        description = "d",
        url = "http://enchantedoasis.uk/",
        authors = {
                "mjra007"
        },
        dependencies = {
                @Dependency(id = "storage")
        }
)

public class MobEconomy {

    private static MobEconomy instance;

    private UsersDataBank usersDataBank;

    private EconomyService economyService;

    private HologramsService hologramsService;

    private PlayerKillMob playerKillMob;

    @Inject
    public org.slf4j.Logger Logger;

    @Inject @DefaultConfig(sharedRoot = false)
    private File configurationFile;

    @Inject @ConfigDir(sharedRoot = false)
    private Path configDir;

    private MainConfig config;

    ConfigurationLoader<CommentedConfigurationNode> loader ;

    CommentedConfigurationNode commentedConfigurationNode;

    MobEconomy(){}

    @Listener
    @SuppressWarnings("unchecked")
    public void onServerStart(GameStartedServerEvent event) {
        Logger.info("Enabling plugin Mob Economy...");
        instance = this;
        usersDataBank = Storage.getInstance().getUsersDataBank();

        loader = HoconConfigurationLoader.builder().setPath(configDir).build();
        File file = new File(configurationFile.getPath());

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) { e.printStackTrace(); }

        loader = HoconConfigurationLoader.builder().setFile(file).build();

        LoadConfig(false);

        playerKillMob = new PlayerKillMob();
        playerKillMob.moneyToDropPerEntityType = config.mobsMoneyDrop;
        Sponge.getEventManager().registerListeners(this, playerKillMob);

        Logger.info("Mob Economy initialised successfully!");
    }

    @Listener
    public void onGamePostInit(GamePostInitializationEvent event) {
        Optional<EconomyService> econService = Sponge.getServiceManager().provide(EconomyService.class);
        econService.ifPresent(service -> economyService = service);

        Optional<HologramsService> hologramsServiceOptional = Sponge.getServiceManager().provide(HologramsService.class);
         hologramsService = hologramsServiceOptional.orElseThrow(
                () -> new RuntimeException("HologramsAPI not available! Is the plugin 'holograms' installed?"));

    }


    @Listener
    public void onServerShutdown(GameStoppedServerEvent event) {
        playerKillMob.moneyToDropPerEntityType = config.mobsMoneyDrop;
        playerKillMob.holograms.forEach(HologramsService.Hologram::remove);
        LoadConfig(true);
    }

    public EconomyService getEconomyService(){
        return economyService;
    }

    public HologramsService getHologramsService(){
        return hologramsService;
    }

    public static MobEconomy getInstance(){
        return instance;
    }

    public UsersDataBank getUsersDataBank() {
        return usersDataBank;
    }

    public void LoadConfig(boolean isValueSet){
        try {
            commentedConfigurationNode = loader.load(ConfigurationOptions.defaults().withShouldCopyDefaults(true));
            config = isValueSet ? this.config : commentedConfigurationNode.getNode("config").getValue(TypeToken.of(MainConfig.class),MainConfig.class.newInstance());
            commentedConfigurationNode.getNode("config").setValue(TypeToken.of(MainConfig.class), config);
            loader.save(commentedConfigurationNode);
        } catch (IOException | IllegalAccessException | InstantiationException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }

}
