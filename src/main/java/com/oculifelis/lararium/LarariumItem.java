package com.oculifelis.lararium;

import com.fundamentum.guide.Guides;
import com.fundamentum.port.FId;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

public final class LarariumItem extends Item {
    public enum Kind{FORK,BOOK,BELL}
    private final Kind kind;
    public LarariumItem(Properties p,Kind kind){super(p);this.kind=kind;}
    @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
        if(kind==Kind.BOOK){if(l.isClientSide())Guides.open(FId.of("lararium","start"));return InteractionResult.SUCCESS;}
        if(kind==Kind.BELL&&p instanceof ServerPlayer sp&&!p.getCooldowns().isOnCooldown(p.getItemInHand(hand))){
            if(HollowHomes.homecoming(sp))p.getCooldowns().addCooldown(p.getItemInHand(hand),80);
            return InteractionResult.SUCCESS;
        }
        return super.use(l,p,hand);
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(kind==Kind.BOOK)return use(c.getLevel(),c.getPlayer(),c.getHand());
        if(kind==Kind.BELL)return use(c.getLevel(),c.getPlayer(),c.getHand());
        if (c.getLevel().getBlockEntity(c.getClickedPos()) instanceof HollowHeartEntity heart && c.getPlayer() != null) {
            if (!c.getLevel().isClientSide()) heart.interact(c.getPlayer(), c.getItemInHand(), c.getPlayer().isShiftKeyDown());
            return InteractionResult.SUCCESS;
        }
        if(!c.getLevel().isClientSide()&&c.getPlayer()!=null)c.getPlayer().sendOverlayMessage(Component.translatable("message.lararium.fork_heart"));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,Consumer<Component> out,TooltipFlag f){
        super.appendHoverText(s,c,d,out,f);out.accept(Component.translatable("tooltip.lararium."+kind.name().toLowerCase(java.util.Locale.ROOT)));
        if(kind==Kind.FORK){out.accept(Component.translatable("tooltip.lararium.fork_controls"));out.accept(Component.translatable("tooltip.lararium.fork_teach"));out.accept(Component.translatable("tooltip.lararium.fork_answer"));out.accept(Component.translatable("tooltip.lararium.fork_keeper"));}
    }
}
