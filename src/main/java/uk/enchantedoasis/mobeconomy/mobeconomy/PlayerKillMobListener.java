package uk.enchantedoasis.mobeconomy.mobeconomy;

import de.randombyte.holograms.api.HologramsService;
import io.netty.util.internal.ConcurrentSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
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
import uk.enchantedoasis.mobeconomy.mobeconomy.MobEconomy;
import uk.enchantedoasis.mobeconomy.mobeconomy.MobsKilledHistory;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PlayerKillMobListener {

    //Amount of money to drop when entity gets killed
    public HashMap<EntityType, Double> moneyToDropPerEntityType = new HashMap<>();
    public final MobsKilledHistory mobsKilledHistory = new MobsKilledHistory();
    public final ConcurrentSet<HologramsService.Hologram> holograms = new ConcurrentSet<>();

    @Listener
    public void PlayerKillMobHandler(DamageEntityEvent event, @First Entity entity) {
       if(event.willCauseDeath() && entity instanceof Player && !(event.getTargetEntity() instanceof Player) ){
           final Player player = (Player)entity;

           //Calculating award
           final double moneyToAward = Math.round(moneyToDropPerEntityType.get(event.getTargetEntity().getType()) * MobEconomy.getInstance().getPlayerMultiplier(player.getUniqueId()) *100.0)/100.0;

           if(moneyToAward>0d){
             //Add it to list to be awarded later
             mobsKilledHistory.addMobKilledMoney(player.getUniqueId(), Cause.builder().append(event.getTargetEntity())
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

}
