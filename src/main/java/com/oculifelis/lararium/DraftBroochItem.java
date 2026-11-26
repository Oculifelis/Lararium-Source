package com.oculifelis.lararium;

import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.function.Consumer;

public final class DraftBroochItem extends Item implements ICurioItem {
    public DraftBroochItem(Properties p){super(p);}
    @Override public void curioTick(SlotContext slot,ItemStack stack){
        if(slot.cosmetic() || !(slot.entity() instanceof ServerPlayer p) || stack.getDamageValue()>=stack.getMaxDamage() || p.isCreative() || p.isSpectator())return;
        if(p.fallDistance<LarariumData.decimal("brooch_fall_distance",5,3,15) || p.getDeltaMovement().y>=0 || p.hasEffect(MobEffects.SLOW_FALLING))return;
        long now=p.level().getGameTime();long last=p.getPersistentData().getLongOr("lararium_brooch_ready",0L);
        if(now<last)return;
        int cooldown=LarariumData.rule("brooch_cooldown",400,100,2400);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,LarariumData.rule("brooch_duration",100,40,400),0,false,true,true));p.fallDistance=0;
        stack.setDamageValue(Math.min(stack.getMaxDamage(),stack.getDamageValue()+1));
        p.getPersistentData().putLong("lararium_brooch_ready",now+cooldown);
        p.getCooldowns().addCooldown(stack,cooldown);
        p.sendOverlayMessage(Component.translatable("message.lararium.brooch_saved",stack.getMaxDamage()-stack.getDamageValue()));
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,Consumer<Component> out,TooltipFlag f){
        super.appendHoverText(s,c,d,out,f);out.accept(Component.translatable("tooltip.lararium.brooch",s.getMaxDamage()-s.getDamageValue()));
        out.accept(Component.translatable("tooltip.lararium.brooch_catch"));out.accept(Component.translatable("tooltip.lararium.brooch_recharge"));
    }
}
