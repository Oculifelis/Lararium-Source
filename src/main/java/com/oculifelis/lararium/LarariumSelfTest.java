package com.oculifelis.lararium;

import com.oculifelis.lararium.geometry.HollowShape;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/** A finite, explicit development run in a separate test world. Never active in ordinary play. */
public final class LarariumSelfTest {
    private static final List<String> results=new ArrayList<>();
    private static final List<HollowHeartEntity> homes=new ArrayList<>();
    private static int ticks;
    private static MinecraftServer server;
    private static ServerPlayer player;
    private static net.minecraft.world.entity.projectile.arrow.Arrow testArrow;
    private static int wardBefore,fullBefore;
    private static HollowHeartEntity overlap;
    private static HollowHeartEntity keeper;
    private static net.minecraft.world.entity.item.ItemEntity favouriteDrop,ordinaryDrop,visitorDrop;
    private static int greetedAt=-1;
    private static HollowHeartEntity answering;
    private static int firstAnswer = -1;
    private static final int ANSWER_PATTERN = 0b10110001;
    private static final java.util.function.Consumer<ServerTickEvent.Post> TICKER=LarariumSelfTest::tick;
    public static void started(ServerStartedEvent e){
        if(!Boolean.getBoolean("lararium.selftest"))return;
        server=e.getServer();ServerLevel l=server.overworld();player=FakePlayerFactory.getMinecraft(l);
        try{
            for(int x=-1;x<=2;x++)for(int z=-1;z<=2;z++)l.setChunkForced(x,z,true);
            homes.add(room(l,new BlockPos(0,100,0),3,3,3,false));
            homes.add(room(l,new BlockPos(18,100,0),3,7,3,false));
            homes.add(room(l,new BlockPos(0,100,18),5,3,5,true));
            homes.add(room(l,new BlockPos(18,100,18),6,3,4,false));
            for(var h:homes){h.assign(player,ItemStack.EMPTY);h.addBreaths(64);}
            keeper=room(l,new BlockPos(36,100,0),3,3,3,false);keeper.assign(player,ItemStack.EMPTY);
            setUpAnswerCircuit(l);
            NeoForge.EVENT_BUS.addListener(TICKER);
        }catch(Throwable x){finish(x);}
    }
    public static HollowHeartEntity room(ServerLevel l,BlockPos heart,int w,int h,int d,boolean pillar){
        BlockPos seed=heart.above();
        for(int x=-1;x<=w;x++)for(int y=-1;y<=h;y++)for(int z=-1;z<=d;z++){
            boolean shell=x==-1||x==w||z==-1||z==d||y==-1||y==h;
            boolean post=pillar&&x==w/2&&z==d/2;
            l.setBlock(seed.offset(x,y,z),(shell||post?Blocks.STONE_BRICKS:Blocks.AIR).defaultBlockState(),3);
        }
        l.setBlock(heart,Lararium.HEART.get().defaultBlockState(),3);
        return (HollowHeartEntity)l.getBlockEntity(heart);
    }
    private static void tick(ServerTickEvent.Post e){
        if(e.getServer()!=server)return;
        ticks++;
        try{
            keeperHabits();
            callAndAnswer();
            if(ticks==60){
                var types=List.of(HollowShape.Form.NOOK,HollowShape.Form.SPIRE,HollowShape.Form.LOOP,HollowShape.Form.VAULT);
                for(int i=0;i<4;i++){check(homes.get(i).awake(),"form "+types.get(i)+" awake ("+homes.get(i).problem()+")");check(homes.get(i).form()==types.get(i),"classified "+types.get(i));}
                var h=homes.getFirst();h.setItem(HollowHeartEntity.INPUT,new ItemStack(Items.STRING,3));
                check(LarariumData.WEAVES.entries().size()>0,"weaving data loaded");
                check(com.fundamentum.hook.Hooks.def(new ItemStack(Lararium.HOOK.get()))!=null,"Bobbin Hook registered in Fundamentum");
                var cap=server.overworld().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,h.getBlockPos(),Direction.UP);
                check(cap!=null&&cap.size()==2,"transactional input capability");
                check(server.overworld().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,h.getBlockPos(),null)==null,"unsided access protects nest");
                var nest=h.getItem(0);check(nest.isEmpty(),"nest starts empty");
                h.setItem(0,new ItemStack(Items.DIAMOND,7));
                BlockEntity saved=BlockEntity.loadStatic(h.getBlockPos(),h.getBlockState(),h.saveWithFullMetadata(server.registryAccess()),server.registryAccess());
                check(saved instanceof HollowHeartEntity,"persistence type");
                var restored=(HollowHeartEntity)saved;
                check(restored.breaths()==h.breaths()&&restored.getItem(0).getCount()==7,"charge and inventory survive save");
                check(Objects.equals(restored.owner(),h.owner()),"owner survives save");
                var visitor=FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.fromString("d8ef4f0e-f879-46c6-90f8-826c4a38fb1b"),"Visitor"));
                var offered=new ItemStack(Items.AMETHYST_SHARD,2);int charge=h.breaths();h.interact(visitor,offered,false);
                check(h.breaths()==charge&&offered.getCount()==2&&Objects.equals(h.owner(),player.getUUID()),"another player cannot control a personal heart");
                BlockPos at=h.origin().offset(2,0,2);server.overworld().setBlock(at,Lararium.HEART.get().defaultBlockState(),3);
                overlap=(HollowHeartEntity)server.overworld().getBlockEntity(at);overlap.assign(player,ItemStack.EMPTY);h.invalidate();
            }
            if(ticks==200){
                var h=homes.getFirst();check(h.getItem(HollowHeartEntity.OUTPUT).getCount()>=1,"real ticking weaves thread");
                check(h.breaths()<64,"weaving spends charge");
                check(!overlap.awake()&&overlap.problem().equals("overlap"),"a second heart cannot inhabit reserved space");
                server.overworld().destroyBlock(overlap.getBlockPos(),false);
                server.overworld().setBlock(h.origin().offset(-1,1,0),Blocks.AIR.defaultBlockState(),3);h.invalidate();
            }
            if(ticks==80){
                var h=homes.getFirst();var cap=server.overworld().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,h.getBlockPos(),Direction.UP);
                int before=h.getItem(HollowHeartEntity.INPUT).getCount(),beforeWork=h.work();
                try(var tx=net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()){check(cap.insert(0,net.neoforged.neoforge.transfer.item.ItemResource.of(Items.STRING),2,tx)==2,"simulated transaction accepts input");}
                check(h.getItem(HollowHeartEntity.INPUT).getCount()==before,"aborted transaction leaves input unchanged");
                check(h.work()==beforeWork,"aborted transaction preserves weaving progress");
                var loop=homes.get(2);server.overworld().setBlock(loop.getBlockPos().north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);loop.interact(player,new ItemStack(Lararium.FORK.get()),true);
                check(loop.recording(),"Listener enters teaching through its tool");
            }
            if(ticks==90){
                var h=homes.get(3);wardBefore=h.breaths();
                testArrow=new net.minecraft.world.entity.projectile.arrow.Arrow(net.minecraft.world.entity.EntityType.ARROW,server.overworld());
                testArrow.setPos(h.origin().getX()+.5,h.origin().getY()+1,h.origin().getZ()+.5);testArrow.setNoGravity(true);testArrow.setDeltaMovement(1.2,0,0);server.overworld().addFreshEntity(testArrow);
            }
            if(ticks==115){
                check(testArrow.getPersistentData().getBooleanOr("lararium_sheltered",false),"Shelter slows a real projectile");
                check(homes.get(3).breaths()==wardBefore-1,"Shelter spends exactly one breath per projectile");testArrow.discard();
            }
            if(ticks==175){
                var loop=homes.get(2);check(loop.phrase()==255&&loop.playing(),"Listener records all eight powered beats");
                loop.interact(player,new ItemStack(Lararium.FORK.get()),true);check(!loop.playing()&&loop.phrase()==255,"pausing preserves the taught phrase");
                loop.interact(player,new ItemStack(Lararium.FORK.get()),true);check(loop.playing()&&!loop.recording()&&loop.phrase()==255,"resuming does not reteach the phrase");
            }
            if(ticks==190){
                var loop=homes.get(2);check(server.overworld().getSignal(loop.getBlockPos(),Direction.SOUTH)==0,"Listener's teaching face emits no feedback");
                check(server.overworld().getSignal(loop.getBlockPos(),Direction.NORTH)==15,"Listener drives ordinary redstone output");
                var copy=(HollowHeartEntity)BlockEntity.loadStatic(loop.getBlockPos(),loop.getBlockState(),loop.saveWithFullMetadata(server.registryAccess()),server.registryAccess());
                check(copy.phrase()==255&&copy.playing(),"taught rhythm survives a save");
            }
            if(ticks==250){
                var h=homes.getFirst();check(!h.awake(),"leaked room sleeps");check(h.getItem(0).getCount()==7,"sleep preserves nest");
                server.overworld().setBlock(h.origin().offset(-1,1,0),Lararium.VEIL.get().defaultBlockState(),3);h.invalidate();
            }
            if(ticks==300){
                check(homes.getFirst().awake(),"passable veil seals room");check(homes.getFirst().getItem(0).getCount()==7,"repair preserves possessions");
                var stack=new ItemStack(Lararium.BROOCH.get());player.fallDistance=6;player.setDeltaMovement(0,-.5,0);
                Lararium.BROOCH.get().curioTick(new top.theillusivec4.curios.api.SlotContext("charm",player,0,false,true),stack);
                check(stack.getDamageValue()==1&&player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"Draft Brooch pays for an emergency fall catch");
                player.removeEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);player.fallDistance=6;
                var second=new ItemStack(Lararium.BROOCH.get());Lararium.BROOCH.get().curioTick(new top.theillusivec4.curios.api.SlotContext("charm",player,0,false,true),second);
                check(second.getDamageValue()==0,"swapping brooches cannot reset the wearer's cooldown");
                var h=homes.getFirst();var door=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN,true);BlockPos at=h.origin().offset(-1,0,1);
                server.overworld().setBlock(at,door,3);server.overworld().setBlock(at.above(),door.setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);h.invalidate();
            }
            if(ticks==310){
                check(homes.getFirst().awake(),"an open ordinary door remains a magical boundary");
                player.snapTo(9,101,9,0,0);check(HollowHomes.homecoming(player),"House Bell recognises four owned forms");
                homes.getFirst().addBreaths(32);int before=homes.stream().mapToInt(HollowHeartEntity::breaths).sum();HollowHomes.share(homes.getFirst());
                check(homes.stream().mapToInt(HollowHeartEntity::breaths).sum()==before,"Homecoming transfers conserve charge");
                for(var h:homes)h.joinHomecoming(server.overworld().getGameTime());
                var h=homes.getFirst();h.setItem(HollowHeartEntity.OUTPUT,new ItemStack(Lararium.THREAD.get(),64));h.setItem(HollowHeartEntity.INPUT,new ItemStack(Items.STRING,2));fullBefore=h.breaths();
            }
            if(ticks==340){
                var h=homes.getFirst();check(h.getItem(HollowHeartEntity.INPUT).getCount()==2&&h.breaths()==fullBefore,"full output pauses work without spending inputs or charge");h.setItem(HollowHeartEntity.OUTPUT,ItemStack.EMPTY);
            }
            if(ticks==440){
                var h=homes.getFirst();check(h.getItem(HollowHeartEntity.OUTPUT).getCount()>=1,"clearing output resumes weaving");
                var tag=new ItemStack(Items.NAME_TAG);tag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Purl"));h.interact(player,tag,false);int remembered=h.breaths();
                server.overworld().destroyBlock(h.getBlockPos(),true);
                int diamonds=server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.getBlockPos()).inflate(3),i->i.getItem().is(Items.DIAMOND)).stream().mapToInt(i->i.getItem().getCount()).sum();
                check(diamonds==7,"dismantling returns the nest exactly once");
                var drops=server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.getBlockPos()).inflate(3),i->i.getItem().is(Lararium.HEART_ITEM.get()));check(drops.size()==1,"dismantling drops one portable heart");
                var restored=new HollowHeartEntity(h.getBlockPos(),Lararium.HEART.get().defaultBlockState());restored.applyComponentsFromItemStack(drops.getFirst().getItem());
                check(restored.personalName().equals("Purl")&&restored.breaths()==remembered,"portable heart carries its name and paid charge");
                check(restored.getItem(0).isEmpty(),"portable memory never duplicates the returned nest");
                var loop=homes.get(2);server.overworld().destroyBlock(loop.getBlockPos(),true);
                var loopDrops=server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(loop.getBlockPos()).inflate(3),i->i.getItem().is(Lararium.HEART_ITEM.get()));
                check(loopDrops.size()==1,"Listener drops one portable heart");var listener=new HollowHeartEntity(loop.getBlockPos(),Lararium.HEART.get().defaultBlockState());listener.applyComponentsFromItemStack(loopDrops.getFirst().getItem());
                check(listener.phrase()==255&&listener.playing(),"moving a Listener preserves its complete taught phrase");finish(null);
            }
        }catch(Throwable x){finish(x);}
    }
    private static void setUpAnswerCircuit(ServerLevel level) {
        answering = room(level, new BlockPos(36, 100, 6), 5, 3, 5, true);
        answering.assign(player, ItemStack.EMPTY);
        for (int z = 1; z < 6; z++) {
            BlockPos at = new BlockPos(36, 100, z);
            level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(at, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH), 3);
        }
    }

    private static void callAndAnswer() {
        ServerLevel level = server.overworld();
        BlockPos input = answering.getBlockPos().north();
        if (ticks == 60) {
            // A taught heart from 0.2.0, brought here by its owner.
            var memory = new net.minecraft.nbt.CompoundTag();
            memory.putInt("schema", 2);
            memory.putInt("phrase", ANSWER_PATTERN);
            memory.putInt("breaths", 2);
            memory.putBoolean("taught", true);
            var data = new net.minecraft.nbt.CompoundTag();
            data.put("lararium_memory", memory);
            var item = new ItemStack(Lararium.HEART_ITEM.get());
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
            answering.applyComponentsFromItemStack(item);
            check(answering.awake() && !answering.answersCalls(), "older Listeners keep their original playback mode");
            var fork = new ItemStack(Lararium.FORK.get());
            var sample = new ItemStack(Lararium.THREAD.get(), 3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fork);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, sample);
            player.setShiftKeyDown(true);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(answering.getBlockPos()), Direction.UP, answering.getBlockPos(), false);
            var use = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            Lararium.FORK.get().useOn(use);
            player.setShiftKeyDown(false);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            check(answering.answersCalls() && sample.getCount() == 3 && answering.breaths() == 2, "fork and off-hand thread arm a taught Listener without spending resources");
        }
        if (ticks > 60 && ticks < 200 && firstAnswer < 0 && answering.signal() == 15) firstAnswer = ticks;
        if (ticks == 130) check(firstAnswer > greetedAt && greetedAt > 0, "a real Keeper greeting reaches the Listener through five repeaters");
        if (firstAnswer > 0) {
            int age = ticks - firstAnswer;
            if (age >= 0 && age < 80 && age % 10 == 0) {
                int expected = ((ANSWER_PATTERN >> (age / 10)) & 1) * 15;
                int output = level.getSignal(answering.getBlockPos(), Direction.NORTH);
                check(output == expected, "wired answer reproduces taught beat " + (age / 10 + 1));
            }
        }
        if (ticks == 205) {
            check(answering.signal() == 0 && answering.breaths() == 1, "one greeting buys exactly one complete phrase");
            level.setBlock(input, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        }
        if (ticks == 207) check(answering.breaths() == 0 && answering.signal() == 15, "a new call uses the last available breath");
        if (ticks == 286) {
            check(answering.signal() == 0 && answering.breaths() == 0, "the held input does not restart the paid answer");
            level.setBlock(input, Blocks.STONE.defaultBlockState(), 3);
        }
        if (ticks == 290) level.setBlock(input, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        if (ticks == 295) answering.addBreaths(2);
        if (ticks == 305) {
            check(answering.signal() == 0 && answering.breaths() == 2, "a missed call is not replayed after refuelling");
            level.setBlock(input, Blocks.STONE.defaultBlockState(), 3);
        }
        if (ticks == 310) level.setBlock(input, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        if (ticks == 312) level.setBlock(input, Blocks.STONE.defaultBlockState(), 3);
        if (ticks == 315) level.setBlock(input, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        if (ticks == 397) {
            check(answering.breaths() == 1 && answering.signal() == 0, "an intervening call neither restarts nor queues an answer");
            BlockPos at = answering.getBlockPos();
            level.destroyBlock(at, true);
            var hearts = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(2), e -> e.getItem().is(Lararium.HEART_ITEM.get()));
            check(hearts.size() == 1, "moving an answering heart yields one item");
            level.setBlock(at, Lararium.HEART.get().defaultBlockState(), 3);
            answering = (HollowHeartEntity) level.getBlockEntity(at);
            answering.applyComponentsFromItemStack(hearts.getFirst().getItem());
            answering.assign(player, hearts.getFirst().getItem());
            hearts.getFirst().discard();
        }
        if (ticks == 430) check(answering.awake() && answering.answersCalls() && answering.phrase() == ANSWER_PATTERN && answering.breaths() == 1 && answering.signal() == 0, "moved heart keeps the answer mode and score without triggering on a powered wire");
    }

    private static void teachKeeper(ItemStack sample){
        var previous=player.getOffhandItem();player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,sample);
        keeper.interact(player,new ItemStack(Lararium.FORK.get()),true);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,previous);
    }
    private static net.minecraft.world.entity.item.ItemEntity offer(ItemStack stack,ServerPlayer giver){
        var at=keeper.origin();
        var drop=new net.minecraft.world.entity.item.ItemEntity(server.overworld(),at.getX()+1.5,at.getY()+.5,at.getZ()+1.5,stack);
        drop.setThrower(giver);drop.setPickUpDelay(32767);drop.setNoGravity(true);drop.setDeltaMovement(0,0,0);server.overworld().addFreshEntity(drop);return drop;
    }
    private static HollowHeartEntity copyKeeper(net.minecraft.nbt.CompoundTag data){return (HollowHeartEntity)BlockEntity.loadStatic(keeper.getBlockPos(),keeper.getBlockState(),data,server.registryAccess());}
    private static void keeperHabits(){
        if(ticks==60){
            check(keeper.awake()&&keeper.form()==HollowShape.Form.NOOK,"familiar things: dedicated Keeper awake");
            var sample=new ItemStack(Items.COPPER_INGOT,5);sample.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Private sample name"));teachKeeper(sample);
            check(keeper.favourite().equals("minecraft:copper_ingot")&&sample.getCount()==5,"teaching remembers only item type and keeps the sample");
            var visitor=FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.fromString("d8ef4f0e-f879-46c6-90f8-826c4a38fb1b"),"Visitor"));
            visitor.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND));keeper.interact(visitor,new ItemStack(Lararium.FORK.get()),true);
            check(keeper.favourite().equals("minecraft:copper_ingot"),"another player cannot reteach the preference");
            visitor.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
            check(keeper.signal()==0,"teaching alone never emits a greeting");
            var data=keeper.saveWithFullMetadata(server.registryAccess());
            check(copyKeeper(data).favourite().equals(keeper.favourite()),"favourite survives a block-entity save");
            check(!keeper.getUpdateTag(server.registryAccess()).contains("favourite"),"ordinary block synchronization does not publish private preference");
            data.putInt("schema",1);data.remove("favourite");data.putInt("breaths",37);data.putInt("phrase",173);data.putBoolean("taught",true);
            var old=copyKeeper(data);check(old.favourite().isEmpty()&&old.breaths()==37&&old.phrase()==173&&Objects.equals(old.owner(),keeper.owner()),"schema-one world data migrates without losing charge, phrase or owner");
            for(String invalid:List.of("bad identifier", "missing:unknown_item", "minecraft:air", "a".repeat(300))){data.putString("favourite",invalid);check(copyKeeper(data).favourite().isEmpty(),"invalid favourite recovers safely: "+invalid.substring(0,Math.min(invalid.length(),24)));}
            var memory=new net.minecraft.nbt.CompoundTag();memory.putInt("schema",1);memory.putInt("breaths",23);memory.putInt("phrase",87);memory.putBoolean("taught",true);memory.putBoolean("playing",true);
            var root=new net.minecraft.nbt.CompoundTag();root.put("lararium_memory",memory);var oldHeart=new ItemStack(Lararium.HEART_ITEM.get());oldHeart.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
            var moved=new HollowHeartEntity(keeper.getBlockPos(),keeper.getBlockState());moved.applyComponentsFromItemStack(oldHeart);
            check(moved.breaths()==23&&moved.phrase()==87&&moved.playing()&&moved.favourite().isEmpty(),"schema-one portable heart preserves its old memory");
        }
        if(ticks==70){keeper.addBreaths(1);ordinaryDrop=offer(new ItemStack(Items.DIAMOND,2),player);favouriteDrop=offer(new ItemStack(Items.COPPER_INGOT,2),player);}
        if(ticks>70&&ticks<125&&greetedAt<0&&!favouriteDrop.isAlive()){
            greetedAt=ticks;
            check(ordinaryDrop.isAlive()&&keeper.getItem(0).is(Items.COPPER_INGOT),"favourite wins over an earlier ordinary offering");
            check(keeper.getItem(0).getCount()==2&&keeper.breaths()==0,"favourite collection conserves items and spends one breath");
            check(keeper.signal()==15&&keeper.getBlockState().getValue(HollowHeartBlock.PULSE),"successful favourite collection starts the visible greeting");
            check(server.overworld().getSignal(keeper.getBlockPos(),Direction.NORTH)==15&&server.overworld().getSignal(keeper.getBlockPos(),Direction.SOUTH)==0,"greeting uses ordinary redstone and leaves the marked face quiet");
        }
        if(ticks==125){
            check(greetedAt>0,"favourite was collected within the bounded run");
            check(keeper.signal()==0&&!keeper.getBlockState().getValue(HollowHeartBlock.PULSE),"greeting expires with no charge or subsequent collection");
            check(ordinaryDrop.isAlive(),"an empty heart cannot collect another offering");keeper.addBreaths(1);
        }
        if(greetedAt>0&&ticks==greetedAt+19)check(keeper.signal()==15,"greeting remains active for its paid twentieth tick");
        if(greetedAt>0&&ticks==greetedAt+20)check(keeper.signal()==0,"greeting ends after exactly twenty ticks");
        if(ticks==150){
            check(!ordinaryDrop.isAlive()&&keeper.getItem(1).is(Items.DIAMOND),"ordinary offerings remain welcome");
            check(keeper.signal()==0&&keeper.breaths()==0,"ordinary collection spends one breath without a greeting");
            keeper.addBreaths(1);
            var visitor=FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.fromString("d8ef4f0e-f879-46c6-90f8-826c4a38fb1b"),"Visitor"));visitorDrop=offer(new ItemStack(Items.COPPER_INGOT,4),visitor);
        }
        if(ticks==200){
            check(visitorDrop.isAlive()&&keeper.breaths()==1&&keeper.signal()==0,"favourite belonging to another player is left alone");visitorDrop.discard();
            server.overworld().setBlock(keeper.origin().offset(-1,1,0),Blocks.AIR.defaultBlockState(),3);keeper.invalidate();
        }
        if(ticks==235){
            check(!keeper.awake()&&keeper.favourite().equals("minecraft:copper_ingot")&&keeper.signal()==0,"remodelling sleeps without forgetting the favourite");
            server.overworld().setBlock(keeper.origin().offset(-1,1,0),Blocks.STONE_BRICKS.defaultBlockState(),3);keeper.invalidate();
        }
        if(ticks==280){
            check(keeper.awake()&&keeper.favourite().equals("minecraft:copper_ingot"),"repaired Keeper remembers its favourite");
            for(int i=0;i<9;i++)keeper.setItem(i,new ItemStack(Items.COBBLESTONE,64));favouriteDrop=offer(new ItemStack(Items.COPPER_INGOT,2),player);
        }
        if(ticks==315){
            check(favouriteDrop.isAlive()&&keeper.signal()==0&&keeper.breaths()==1,"a full nest neither greets nor charges for a rejected favourite");
            keeper.setItem(0,new ItemStack(Items.COPPER_INGOT,63));
        }
        if(ticks==335){
            check(keeper.getItem(0).getCount()==64&&favouriteDrop.isAlive()&&favouriteDrop.getItem().getCount()==1&&keeper.breaths()==0,"partial favourite insertion keeps the remainder and pays exactly once");
            teachKeeper(ItemStack.EMPTY);check(keeper.favourite().isEmpty()&&keeper.signal()==0,"empty off-hand teaching forgets and cancels a greeting");
            teachKeeper(new ItemStack(Items.EMERALD));check(keeper.favourite().equals("minecraft:emerald"),"a Keeper can learn a different favourite");
            var data=keeper.saveWithFullMetadata(server.registryAccess());var loaded=copyKeeper(data);loaded.setLevel(server.overworld());
            check(loaded.signal()==0,"loading memory never replays a delivery greeting");
        }
        if(ticks==350){
            teachKeeper(new ItemStack(Items.COPPER_INGOT));for(int i=0;i<9;i++)keeper.setItem(i,new ItemStack(Items.COBBLESTONE,64));
            keeper.setItem(0,new ItemStack(Items.DIAMOND,63));keeper.addBreaths(1);ordinaryDrop=offer(new ItemStack(Items.DIAMOND,2),player);
        }
        if(ticks==380){
            check(favouriteDrop.isAlive()&&favouriteDrop.getItem().getCount()==1&&keeper.getItem(0).is(Items.DIAMOND)&&keeper.getItem(0).getCount()==64,"a blocked favourite does not starve an ordinary offering that fits");
            check(ordinaryDrop.isAlive()&&ordinaryDrop.getItem().getCount()==1&&keeper.breaths()==0&&keeper.signal()==0,"fallback collection conserves its remainder and emits no false greeting");
            teachKeeper(new ItemStack(Items.EMERALD));
        }
        if(ticks==390){
            var at=keeper.getBlockPos();server.overworld().destroyBlock(at,true);
            var hearts=server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(3),e->e.getItem().is(Lararium.HEART_ITEM.get()));
            check(hearts.size()==1,"a familiar Keeper still drops exactly one portable heart");
            var moved=new HollowHeartEntity(at,Lararium.HEART.get().defaultBlockState());moved.applyComponentsFromItemStack(hearts.getFirst().getItem());
            check(moved.favourite().equals("minecraft:emerald")&&moved.getItem(0).isEmpty(),"portable heart keeps its favourite without duplicating the nest");
        }
    }
    private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);results.add(name);LoggerFactory.getLogger("lararium").info("LARARIUMTEST PASS {}",name);}
    private static void finish(Throwable failure){
        if(failure!=null)LoggerFactory.getLogger("lararium").error("LARARIUMTEST FAIL",failure);
        String report="{\"passed\":"+(failure==null)+",\"checks\":"+results.size()+",\"ticks\":"+ticks+"}";
        try{Files.writeString(Path.of("lararium-selftest-result.json"),report);}catch(Exception ignored){}
        LoggerFactory.getLogger("lararium").info("LARARIUMTEST RESULT {}",report);
        NeoForge.EVENT_BUS.unregister(TICKER);
        server.halt(false);
    }
    private LarariumSelfTest(){}
}
