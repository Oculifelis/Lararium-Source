package com.oculifelis.lararium;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Lararium.ID)
public final class Lararium {
    public static final String ID="lararium";
    public static Identifier id(String s) { return Identifier.fromNamespaceAndPath(ID,s); }
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    public static final DeferredRegister<BlockEntityType<?>> BES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
    public static final DeferredBlock<HollowHeartBlock> HEART=BLOCKS.registerBlock("hollow_heart",HollowHeartBlock::new,
        p->p.strength(2.5F).sound(SoundType.COPPER).noOcclusion().lightLevel(s->s.getValue(HollowHeartBlock.AWAKE)?(s.getValue(HollowHeartBlock.PULSE)?11:7):1));
    public static final DeferredBlock<ThresholdBlock> VEIL=BLOCKS.registerBlock("threshold_veil",ThresholdBlock::new,
        p->p.strength(.3F).sound(SoundType.WOOL).noCollision().noOcclusion());
    public static final DeferredBlock<PerchBlock> PERCH=BLOCKS.registerBlock("perch",PerchBlock::new,
        p->p.strength(1.5F).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredItem<BlockItem> HEART_ITEM=ITEMS.registerSimpleBlockItem(HEART,p->p.stacksTo(1));
    public static final DeferredItem<BlockItem> VEIL_ITEM=ITEMS.registerSimpleBlockItem(VEIL);
    public static final DeferredItem<BlockItem> PERCH_ITEM=ITEMS.registerSimpleBlockItem(PERCH);
    public static final DeferredItem<Item> THREAD=ITEMS.registerSimpleItem("hollow_thread");
    public static final DeferredItem<LarariumItem> FORK=ITEMS.registerItem("tuning_fork",p->new LarariumItem(p,LarariumItem.Kind.FORK),p->p.stacksTo(1));
    public static final DeferredItem<LarariumItem> BOOK=ITEMS.registerItem("field_notes",p->new LarariumItem(p,LarariumItem.Kind.BOOK),p->p.stacksTo(1));
    public static final DeferredItem<LarariumItem> BELL=ITEMS.registerItem("house_bell",p->new LarariumItem(p,LarariumItem.Kind.BELL),p->p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> HOOK=ITEMS.registerSimpleItem("bobbin_hook",p->p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DraftBroochItem> BROOCH=ITEMS.registerItem("draft_brooch",DraftBroochItem::new,p->p.stacksTo(1).durability(16).rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<HollowHeartEntity>> HEART_BE=BES.register("hollow_heart",()->new BlockEntityType<>(HollowHeartEntity::new,HEART.get()));
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.lararium")).icon(()->new ItemStack(HEART_ITEM.get()))
        .displayItems((p,o)->ITEMS.getEntries().forEach(i->o.accept(i.get()))).build());
    public Lararium(IEventBus bus) {
        BLOCKS.register(bus);ITEMS.register(bus);BES.register(bus);TABS.register(bus);
        LarariumData.init();LarariumViews.init();
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e)->e.registerBlockEntity(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,HEART_BE.get(),
            (be,side)->side==null?null:new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be,side)));
        NeoForge.EVENT_BUS.addListener(HollowHomes::placed);
        NeoForge.EVENT_BUS.addListener(HollowHomes::broken);
        NeoForge.EVENT_BUS.addListener(HollowHomes::unloaded);
        NeoForge.EVENT_BUS.addListener(LarariumSelfTest::started);
    }
}
