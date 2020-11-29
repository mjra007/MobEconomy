package uk.enchantedoasis.mobeconomy.mobeconomy;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import de.randombyte.holograms.api.HologramsService;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.data.DataMutateResult;
 import net.luckperms.api.model.user.User;
 import net.luckperms.api.node.types.MetaNode;
 import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.ProviderRegistration;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.spongepowered.api.text.format.TextColors;

@Plugin(
        id = "mobeconomy",
        name = "mobeconomy",
        authors = {
                "mjra007"
        },
        dependencies = {
                @Dependency(id = "luckperms")
        }
)

public class MobEconomy implements CommandExecutor {

    private static MobEconomy instance;

    public static final String PLAYER_MULTIPLIER_METADATA = "mobeconomy.playermultiplier";

    private LuckPerms luckyPermsProvider;

    private EconomyService economyService;

    private HologramsService hologramsService;

    private PlayerKillMobListener playerKillMob;

    @Inject
    public org.slf4j.Logger Logger;

    @Inject @DefaultConfig(sharedRoot = false)
    private File configurationFile;

    @Inject @ConfigDir(sharedRoot = false)
    private Path configDir;

    private MainConfig config;

    ConfigurationLoader<CommentedConfigurationNode> loader ;

    CommentedConfigurationNode commentedConfigurationNode;

    CommandSpec commandMultiplierSet;
    MobEconomy(){}

    @Listener
    @SuppressWarnings("unchecked")
    public void onServerStart(GameStartedServerEvent event) {
        Logger.info("Enabling plugin Mob Economy...");
        instance = this;

        loader = HoconConfigurationLoader.builder().setPath(configDir).build();
        File file = new File(configurationFile.getPath());

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) { e.printStackTrace(); }

        loader = HoconConfigurationLoader.builder().setFile(file).build();

        loadConfig(false);
        loadLuckyPermsProvider();

        playerKillMob = new PlayerKillMobListener();
        playerKillMob.moneyToDropPerEntityType = config.mobsMoneyDrop;
        Sponge.getEventManager().registerListeners(this, playerKillMob);

        commandMultiplierSet = CommandSpec.builder()
            .description(Text.of("Set player multiplier for money drops from mobs!"))
            .permission("mobeconomy.multiplier")
            .arguments(
                GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))),
                GenericArguments.onlyOne(GenericArguments.doubleNum(Text.of("multiplier"))))
            .executor(MobEconomy.getInstance())
            .build();

        Sponge.getCommandManager().register(this, commandMultiplierSet, "multiplier");

        Logger.info("Mob Economy initialised successfully!");
    }

    private void loadLuckyPermsProvider() {
        Optional<ProviderRegistration<LuckPerms>> provider = Sponge.getServiceManager().getRegistration(LuckPerms.class);
        provider.ifPresent(
            luckPermsProviderRegistration -> luckyPermsProvider = luckPermsProviderRegistration
                .getProvider());
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
        loadConfig(true);
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

    public LuckPerms getLuckyPermsProvider() {return this.luckyPermsProvider;}

    public void loadConfig(boolean isValueSet){
        try {
            commentedConfigurationNode = loader.load(ConfigurationOptions.defaults().withShouldCopyDefaults(true));
            config = isValueSet ? this.config : commentedConfigurationNode.getNode("config").getValue(TypeToken.of(MainConfig.class),MainConfig.class.newInstance());
            commentedConfigurationNode.getNode("config").setValue(TypeToken.of(MainConfig.class), config);
            loader.save(commentedConfigurationNode);
        } catch (IOException | IllegalAccessException | InstantiationException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (src instanceof Player) {
            Player playerExecuting = (Player) src;
            Player player = args.<Player>getOne("player").get();
            Double multiplier = args.<Double>getOne("multiplier").get();
            DataMutateResult result = setPlayerMultiplier(player.getUniqueId(), multiplier);

            if(result.wasSuccessful()){
                playerExecuting.sendMessage(Text
                    .builder("Successfully set player's multiplier to "+multiplier+" !")
                    .color(TextColors.GREEN).build());
                return CommandResult.success();
            }else{
                playerExecuting.sendMessage(Text
                    .builder("Could not perform multiplier change for player "+player.getName())
                    .color(TextColors.RED).build());
               return CommandResult.empty();
            }
        }
        else if(src instanceof CommandBlockSource) {
            src.sendMessage(Text.of("Command can only be executed by player or console!"));
            return CommandResult.empty();
        }else if(src instanceof ConsoleSource){
            ConsoleSource consoleSource = (ConsoleSource) src;
            Player player = args.<Player>getOne("player").get();
            Double multiplier = args.<Double>getOne("multiplier").get();
            DataMutateResult result = setPlayerMultiplier(player.getUniqueId(), multiplier);

            if(result.wasSuccessful()){
                consoleSource.sendMessage(Text
                    .builder("Successfully set player's multiplier to "+multiplier+" !")
                    .color(TextColors.GREEN).build());
                return CommandResult.success();
            }else{
                consoleSource.sendMessage(Text
                    .builder("Could not perform multiplier change for player "+player.getName())
                    .color(TextColors.RED).build());
                return CommandResult.empty();
            }
        }
        return CommandResult.empty();
    }

    public double getPlayerMultiplier(UUID player){
        User user = null;
        try {
            user = MobEconomy.getInstance().getLuckyPermsProvider().getUserManager().loadUser(player).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        assert user != null;
        CachedMetaData metaData = user.getCachedData().getMetaData();

        String value = metaData.getMetaValue(MobEconomy.PLAYER_MULTIPLIER_METADATA);
        return value == null ? 1 : Double.parseDouble(value);
    }

    public DataMutateResult setPlayerMultiplier(UUID player, Double multiplier){
        User user = null;
        try {
            user = MobEconomy.getInstance().getLuckyPermsProvider().getUserManager().loadUser(player).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        assert user != null;
        DataMutateResult result;
        CachedMetaData metaData = user.getCachedData().getMetaData();
        if(metaData.getMeta().containsKey(PLAYER_MULTIPLIER_METADATA)){
            String value = metaData.getMetaValue(MobEconomy.PLAYER_MULTIPLIER_METADATA);
            assert value != null;
            user.data().remove(MetaNode.builder(PLAYER_MULTIPLIER_METADATA,value).build());
            this.luckyPermsProvider.getUserManager().saveUser(user);
            result = user.data().add(MetaNode.builder(PLAYER_MULTIPLIER_METADATA,multiplier.toString()).build());
        }else{
            result = user.data().add(MetaNode.builder(PLAYER_MULTIPLIER_METADATA,multiplier.toString()).build());
        }
        this.luckyPermsProvider.getUserManager().saveUser(user);
        return result;
    }
}
