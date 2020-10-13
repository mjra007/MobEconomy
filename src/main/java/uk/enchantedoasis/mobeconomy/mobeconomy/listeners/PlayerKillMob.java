package uk.enchantedoasis.mobeconomy.mobeconomy.listeners;

import io.netty.util.internal.ConcurrentSet;
import de.randombyte.holograms.api.HologramsService;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import uk.enchantedoasis.mobeconomy.mobeconomy.DataKeys;
import uk.enchantedoasis.mobeconomy.mobeconomy.MobEconomy;
import uk.enchantedoasis.mobeconomy.mobeconomy.MobsKilledHistory;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PlayerKillMob {

    //Amount of money to drop when entity gets killed
    public HashMap<EntityType, Double> moneyToDropPerEntityType = new HashMap<>();
    public final double defaultMobMoneyDrop = 1.3d;
    public final MobsKilledHistory mobsKilledHistory = new MobsKilledHistory();
    public final ConcurrentSet<HologramsService.Hologram> holograms = new ConcurrentSet<>();

    @Listener
    public void PlayerKillMobEvent(DamageEntityEvent event, @First Entity entity) {
       if(event.willCauseDeath() && entity instanceof Player && !(event.getTargetEntity() instanceof Player) ){
           final Player player = (Player)entity;

           moneyToDropPerEntityType.computeIfAbsent(event.getTargetEntity().getType(),(s)->moneyToDropPerEntityType.put(s,defaultMobMoneyDrop));

           //Calculating award
           final Optional<Double> multiplierOpt =  MobEconomy.getInstance().getUsersDataBank().getUserDataValue(player.getUniqueId(), DataKeys.PLAYER_MOB_DROP_MULTIPLIER);
           final double moneyToAward = Math.round(moneyToDropPerEntityType.get(event.getTargetEntity().getType()) * multiplierOpt.orElse(1.0d)*100.0)/100.0;

           //Add it to list to be awarded later
           mobsKilledHistory.addMobKilledMoney(player.getUniqueId(),
                   Cause.builder()
                   .append(event.getTargetEntity())
                   .build(event.getContext()), moneyToAward);

           //Spawn hologramw with money
           final HologramsService.Hologram hologram = MobEconomy.getInstance().getHologramsService()
                   .createHologram(event.getTargetEntity().getLocation().add(0,1,0), Text.of(TextColors.GREEN, TextStyles.BOLD,"+"+moneyToAward+" coins")).get();

           holograms.add(hologram);

           Task.builder()
                   .execute(()->{
                       if(holograms.contains(hologram)){
                           hologram.remove();
                           holograms.remove(hologram);
                       }
                   })
                   .delay(3, TimeUnit.SECONDS)
                   .submit(MobEconomy.getInstance());
       }
    }



}
