package com.oculifelis.lararium;

import com.oculifelis.lararium.geometry.HollowShape;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class HollowHomes {
    private static final Map<Level,Map<BlockPos,HollowHeartEntity>> HOMES=new WeakHashMap<>();
    private record Budget(long tick,int spent) {}
    private static final Map<Level,Budget> BUDGET=new WeakHashMap<>();
    private static final Map<Level,Budget> EFFECTS=new WeakHashMap<>();
    static boolean effectBudget(Level l,int cost){long t=l.getGameTime();var b=EFFECTS.get(l);int used=b!=null&&b.tick==t?b.spent:0;if(used+cost>128)return false;EFFECTS.put(l,new Budget(t,used+cost));return true;}
    static void track(HollowHeartEntity h) { if(h.getLevel() instanceof ServerLevel)HOMES.computeIfAbsent(h.getLevel(),l->new HashMap<>()).put(h.getBlockPos(),h); }
    static void forget(HollowHeartEntity h) { if(!(h.getLevel() instanceof ServerLevel))return;var homes=HOMES.get(h.getLevel());if(homes!=null)homes.remove(h.getBlockPos(),h); }
    public static Collection<HollowHeartEntity> in(Level l) {
        if(!(l instanceof ServerLevel))return List.of();
        var map=HOMES.get(l);if(map==null)return List.of();
        map.values().removeIf(HollowHeartEntity::isRemoved);return List.copyOf(map.values());
    }
    static int scanBudget(Level l) {
        long now=l.getGameTime();var b=BUDGET.get(l);int used=b!=null&&b.tick==now?b.spent:0;
        int take=Math.min(32,Math.max(0,1024-used));BUDGET.put(l,new Budget(now,used+take));return take;
    }
    static boolean overlaps(HollowHeartEntity h,Set<HollowShape.Cell> cells) {
        for(var other:in(h.getLevel()))if(other!=h && other.reservesSpace()) {
            for(var p:cells)if(other.contains(h.origin().offset(p.x(),p.y(),p.z())))return true;
        }
        return false;
    }
    public static void invalidate(Level l,BlockPos p) {
        int range=LarariumData.rule("range",8,3,12)+1,height=LarariumData.rule("height",12,5,20)+1;
        for(var h:in(l)){BlockPos seed=h.origin();if(Math.abs(seed.getX()-p.getX())<=range&&Math.abs(seed.getZ()-p.getZ())<=range&&Math.abs(seed.getY()-p.getY())<=height)h.invalidate();}
    }
    public static void placed(BlockEvent.EntityPlaceEvent e) { if(e.getLevel() instanceof ServerLevel l)invalidate(l,e.getPos()); }
    public static void broken(net.neoforged.neoforge.event.level.block.BreakBlockEvent e) { if(e.getLevel() instanceof ServerLevel l)invalidate(l,e.getPos()); }
    public static void unloaded(net.neoforged.neoforge.event.level.LevelEvent.Unload e){if(e.getLevel() instanceof ServerLevel l){HOMES.remove(l);BUDGET.remove(l);EFFECTS.remove(l);}}
    public static boolean homecoming(ServerPlayer player) {
        EnumMap<HollowShape.Form,HollowHeartEntity> chosen=new EnumMap<>(HollowShape.Form.class);
        in(player.level()).stream().filter(h->h.awake() && h.controls(player) && h.getBlockPos().closerToCenterThan(player.position(),24))
            .sorted(Comparator.comparingDouble(h->h.getBlockPos().distToCenterSqr(player.position())))
            .forEach(h->chosen.putIfAbsent(h.form(),h));
        if(chosen.size()!=4) { player.sendOverlayMessage(Component.translatable("message.lararium.homecoming_missing",chosen.size()));return false; }
        long until=player.level().getGameTime()+1200;
        for(var h:chosen.values())h.joinHomecoming(until);
        com.fundamentum.unlock.Unlocks.grant(player,com.fundamentum.port.FId.of("lararium","homecoming"));
        player.sendOverlayMessage(Component.translatable("message.lararium.homecoming"));
        return true;
    }
    static void share(HollowHeartEntity source) {
        if(source.breaths()<2)return;
        in(source.getLevel()).stream().filter(h->h!=source&&h.awake()&&h.homecoming()&&Objects.equals(source.owner(),h.owner())
            && h.canReceiveBreath()&&h.getBlockPos().closerThan(source.getBlockPos(),24)&&h.breaths()+2<=source.breaths())
            .min(Comparator.comparingInt(HollowHeartEntity::breaths)).ifPresent(h->{source.addBreaths(-1);h.addBreaths(1);source.threadTo(h);});
    }
    private HollowHomes() {}
}
