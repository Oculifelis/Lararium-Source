package com.oculifelis.lararium.compat;

import com.oculifelis.lararium.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.*;
import java.util.List;

@WailaPlugin
public final class LarariumJade implements IWailaPlugin {
    private static final Identifier UID=Lararium.id("inhabitant");
    private static final Data DATA=new Data();
    @Override public void register(IWailaCommonRegistration r){r.registerBlockDataProvider(DATA,HollowHeartEntity.class);r.registerItemStorage(new PrivateNest(),HollowHeartEntity.class);}
    @Override public void registerClient(IWailaClientRegistration r){r.registerBlockComponent(new Display(),HollowHeartBlock.class);r.registerItemStorageClient(new PrivateNestDisplay());}
    record Status(String name,String form,String problem,int breaths,int stored,int work,boolean recording,String favourite,String answer){}
    /** Stop the generic container fallback before any private stacks enter a response. */
    static final class PrivateNest implements IServerExtensionProvider<ItemStack>{
        @Override public List<ViewGroup<ItemStack>> getGroups(Accessor<?> a){return List.of();}
        @Override public int getDefaultPriority(){return -1000;}
        @Override public Identifier getUid(){return Lararium.id("private_nest");}
    }
    static final class PrivateNestDisplay implements IClientExtensionProvider<ItemStack,ItemView>{
        @Override public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> a,List<ViewGroup<ItemStack>> groups){return List.of();}
        @Override public Identifier getUid(){return Lararium.id("private_nest");}
    }
    static final class Data implements StreamServerDataProvider<BlockAccessor,Status>{
        @Override public Status streamData(BlockAccessor a){if(!(a.getBlockEntity() instanceof HollowHeartEntity h))return null;return new Status(h.personalName(),h.form().name().toLowerCase(java.util.Locale.ROOT),h.problem(),h.breaths(),h.stored(),h.work(),h.recording(),a.getPlayer()!=null&&h.controls(a.getPlayer())?h.favourite():"",h.answersCalls() && h.awake() && h.form()==com.oculifelis.lararium.geometry.HollowShape.Form.LOOP && !h.recording()?h.answerStatus():"");}
        @Override public StreamCodec<RegistryFriendlyByteBuf,Status> streamCodec(){return StreamCodec.of((b,s)->{b.writeUtf(s.name,128);b.writeUtf(s.form,16);b.writeUtf(s.problem,32);b.writeVarInt(s.breaths);b.writeVarInt(s.stored);b.writeVarInt(s.work);b.writeBoolean(s.recording);b.writeUtf(s.favourite,256);b.writeUtf(s.answer,48);},b->new Status(b.readUtf(128),b.readUtf(16),b.readUtf(32),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readUtf(256),b.readUtf(48)));}
        @Override public Identifier getUid(){return UID;}
    }
    static final class Display implements IBlockComponentProvider{
        @Override public void appendTooltip(ITooltip t,BlockAccessor a,IPluginConfig config){t.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);DATA.decodeFromData(a).ifPresent(s->{
            var form=Component.translatable("form.lararium."+s.form);t.add(s.name.isBlank()?form:Component.literal(s.name).append(" · ").append(form));
            t.add(s.problem.isEmpty()?Component.translatable("jade.lararium.status",s.breaths,s.stored):Component.translatable("problem.lararium."+s.problem));
            if(s.work>0)t.add(Component.translatable("jade.lararium.weaving",s.work/20.));
            if(!s.answer.isEmpty())t.add(Component.translatable(s.answer));
            if(s.recording)t.add(Component.translatable("message.lararium.recording"));
            if(s.form.equals("nook")&&!s.favourite.isEmpty()){
                var id=Identifier.tryParse(s.favourite);
                if(id!=null&&net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))t.add(Component.translatable("message.lararium.favourite",new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)).getHoverName()));
            }
        });}
        @Override public int getDefaultPriority(){return 1100;}
        @Override public Identifier getUid(){return UID;}
    }
}
