package uk.enchantedoasis.mobeconomy.mobeconomy;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MobsKilledHistory {

    private final ConcurrentMap<UUID, ConcurrentMap<Cause, Double>> mobKilledHistory;

    public MobsKilledHistory(){
        mobKilledHistory= new ConcurrentHashMap<>();
        Task.builder()
                .execute(this:: depositPlayersMoney)
                .async()
                .interval(1, TimeUnit.MINUTES)
                .submit(MobEconomy.getInstance());
    }

    public void addMobKilledMoney(UUID uuid, Cause cause, Double money){
        if(mobKilledHistory.containsKey(uuid)){
            mobKilledHistory.get(uuid).put(cause,money);
        }else{
            mobKilledHistory.put(uuid, new ConcurrentHashMap<Cause, Double>(){
                {
                    put(cause, money);
                }
            });
        }
    }

    private void depositPlayersMoney(){
        EconomyService economyService =  MobEconomy.getInstance().getEconomyService();
        Currency defaultCurrency =  economyService.getDefaultCurrency();
        mobKilledHistory.forEach((player, mobHistory) ->{
            double totalMoneyAwarded =  Math.round(mobHistory.values().stream().mapToDouble(v -> v).sum() * 100.0) / 100.0;
            List<Text> entities =  mobHistory.keySet().stream().map(aDouble -> aDouble.first(Entity.class).get()).collect(Collectors.groupingBy(Entity::getType))
                    .entrySet().stream().map(s-> Text.builder().append(Text.of(TextColors.GREEN, s.getValue().size()+"x "))
                            .append(Text.of(TextColors.AQUA, s.getKey().getName().toUpperCase())).build()).collect(Collectors.toList());
            mobHistory.forEach((cause, money) ->
                    {
                        economyService.getOrCreateAccount(player).ifPresent(account -> {
                            account.deposit(defaultCurrency, BigDecimal.valueOf(money), cause);
                        });
                        mobHistory.remove(cause);
                    }
            );

            if(totalMoneyAwarded>0)
                Sponge.getServer().getPlayer(player).ifPresent(p->
                        p.sendMessage(
                                Text.builder().append(Text.builder()
                                        .append( Text.of(TextColors.GREEN, TextStyles.BOLD,"+"+totalMoneyAwarded+" "))
                                        .append(Text.of(TextColors.GREEN, "coins from killing these"))
                                        .append(Text.of(TextColors.YELLOW, " ENTITIES"))
                                        .append(Text.of(TextColors.GRAY," (hover to see entities)"))
                                .onHover(TextActions.showText(Text.joinWith(Text.NEW_LINE, entities)))
                                .build())
                                .build()));
        });

    }

}
