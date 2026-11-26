package com.oculifelis.lararium.client;

import com.oculifelis.lararium.*;
import com.fundamentum.guide.Guides;
import com.fundamentum.port.FId;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid=Lararium.ID,value=Dist.CLIENT)
public final class LarariumClientCheck {
    private static final boolean ON=Boolean.getBoolean("lararium.devtest");
    private static boolean hidden,started;
    private static int ticks,idle;
    private static volatile int failures,checks;
    private static volatile int floor=-61;
    private static final List<HollowHeartEntity> HOMES=new ArrayList<>();
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(!ON)return;
        Minecraft mc=Minecraft.getInstance();
        if(!hidden){hidden=true;GLFW.glfwSetWindowPos(mc.getWindow().handle(),-32000,-32000);GLFW.glfwSetWindowSize(mc.getWindow().handle(),1600,900);mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);mc.options.pauseOnLostFocus=false;}
        if(mc.level==null||mc.player==null){
            if(!started&&mc.gui.screen() instanceof TitleScreen&&mc.gui.overlay()==null){started=true;create(mc);}
            else if(!started&&++idle>200&&mc.gui.overlay()==null)mc.gui.setScreen(new TitleScreen());
            return;
        }
        var server=mc.getSingleplayerServer();if(server==null)return;
        ticks++;
        if(ticks<400&&mc.gui.screen()!=null)mc.gui.setScreen(null);
        try{
            switch(ticks){
                case 30->server.execute(()->setup(server));
                case 80->server.execute(()->{
                    var p=player(server);teleport(p,1.5,101,1.5,0,0);
                    ItemEntity item=new ItemEntity(p.level(),.5,floor+2,.5,new ItemStack(Items.EMERALD,3));item.setThrower(p);item.setPickUpDelay(80);p.level().addFreshEntity(item);
                });
                case 150->server.execute(()->{
                    for(var h:HOMES)check(h.awake(),"inhabitant awake: "+h.personalName());
                    var k=HOMES.getFirst();var drops=server.overworld().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atCenterOf(pos(-4,96,-4)),net.minecraft.world.phys.Vec3.atCenterOf(pos(8,110,8))));
                    check(k.getItem(0).is(Items.EMERALD)&&k.getItem(0).getCount()==3,"Keeper gathers the real player's offered stack (form "+k.form()+", breaths "+k.breaths()+", slot0 "+k.getItem(0)+", heart "+k.getBlockPos().toShortString()+", drops "+drops.stream().map(d->d.getItem()+"@"+d.blockPosition().toShortString()+" age "+d.getAge()+" owner "+(d.getOwner()!=null)).toList()+")");
                    check(HollowHomes.homecoming(player(server)),"four-form Homecoming");
                });
                case 160->{hud(mc,true);view(mc,v(1.5,101,-3.5),v(.5,101,1));}
                case 200->shot(mc,"keeper");
                case 215->{hud(mc,false);view(mc,v(1.5,101,1.5),v(.5,101.4,.5));}
                case 235->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(v(.5,101.4,.5),Direction.UP,pos(0,101,0),false));
                case 255->shot(mc,"jade");
                case 270->{hud(mc,true);view(mc,v(10.5,103,-4.5),v(10.5,104,1.5));}
                case 310->shot(mc,"breather");
                case 325->view(mc,v(2.5,107,7),v(2.5,101,12.5));
                case 365->shot(mc,"listener");
                case 380->view(mc,v(12.5,103,7),v(12.5,101,12.5));
                case 420->shot(mc,"shelter");
                case 435->view(mc,v(-9,111,-10),v(7,102,7));
                case 480->shot(mc,"garden");
                case 495->{hud(mc,false);Guides.open(FId.of("lararium","start"));}
                case 535->shot(mc,"guide");
                case 550->{mc.gui.setScreen(null);mc.gui.setScreen(new InventoryScreen(mc.player));}
                case 590->shot(mc,"inventory");
                case 620->{
                    mc.gui.setScreen(null);hud(mc,false);
                    server.execute(()->{
                        var p=player(server);p.setGameMode(GameType.SURVIVAL);p.getAbilities().flying=false;p.onUpdateAbilities();
                        var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventoryOrNull(p);
                        check(inventory!=null&&inventory.getCurios().containsKey("charm"),"standard Curios charm slot exists");
                        if(inventory!=null&&inventory.getCurios().containsKey("charm"))inventory.getCurios().get("charm").getStacks().setStackInSlot(0,new ItemStack(Lararium.BROOCH.get()));
                        com.fundamentum.slots.Slots.set(p,com.fundamentum.slots.Slots.HOOK,0,new ItemStack(Lararium.HOOK.get()));
                        teleport(p,10.5,101,1.5,0,-20);
                    });
                }
                case 630->mc.options.keyJump.setDown(true);
                case 675->{mc.options.keyJump.setDown(false);server.execute(()->check(player(server).getY()>floor+3,"Breather lifts a player through normal jump input"));}
                case 680->server.execute(()->check(com.fundamentum.hook.Hooks.fire(player(server)),"Bobbin Hook fires from the equipped core hook slot"));
                case 688->server.execute(()->{check(com.fundamentum.hook.Hooks.latched(player(server)),"Bobbin Hook latches in the Breather shaft");com.fundamentum.hook.Hooks.release(player(server));});
                case 690->server.execute(()->{
                    var p=player(server);p.removeEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);p.setDeltaMovement(0,0,0);p.hurtMarked=true;teleport(p,19.5,111,.5,0,60);
                });
                case 745->shot(mc,"brooch");
                case 780->server.execute(()->{
                    var p=player(server);var inv=top.theillusivec4.curios.api.CuriosApi.getCuriosInventoryOrNull(p);
                    var brooch=inv.getCurios().get("charm").getStacks().getStackInSlot(0);
                    check(brooch.getDamageValue()==1,"equipped brooch catches a real fall and spends one use");
                    check(p.getHealth()>=19.9F,"brooch fall reaches the ground without damage");
                });
                case 825->{
                    mc.gui.setScreen(new InventoryScreen(mc.player));
                    if(net.neoforged.fml.ModList.get().isLoaded("jei"))check(com.oculifelis.lararium.compat.LarariumJeiAccess.filterLararium(),"JEI runtime is available");
                    else check(!net.neoforged.fml.ModList.get().isLoaded("jade"),"base client runs with both optional viewers absent");
                }
                case 865->shot(mc,"jei");
                case 875->{
                    if(net.neoforged.fml.ModList.get().isLoaded("jei")){
                        check(com.oculifelis.lararium.compat.LarariumJeiAccess.hasWeaving(),"JEI has the Hollow Weaving category");
                        check(com.oculifelis.lararium.compat.LarariumJeiAccess.hasWeavingRecipe(),"JEI has the synchronized weaving recipe");
                        com.oculifelis.lararium.compat.LarariumJeiAccess.showWeaving();
                    }
                }
                case 915->shot(mc,"weaving");
                case 950->finish(mc);
            }
        }catch(Throwable x){failures++;LoggerFactory.getLogger("lararium").error("LARARIUMCLIENT FAIL",x);finish(mc);}
    }
    private static void setup(MinecraftServer s){
        try{
            ServerLevel l=s.overworld();var p=player(s);run(s,"time set 8500");run(s,"gamerule minecraft:advance_time false");
            floor=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,0,0)-1;
            for(int x=-12;x<31;x++)for(int z=-12;z<31;z++){
                boolean path=(x>=5&&x<=7)||(z>=6&&z<=8)||z==-3;
                l.setBlock(pos(x,100,z),(path?((x+z)%5==0?Blocks.MOSSY_COBBLESTONE:Blocks.GRAVEL):Blocks.GRASS_BLOCK).defaultBlockState(),3);
            }
            HOMES.add(gardenRoom(l,pos(0,100,0),3,3,3,false));
            HOMES.add(gardenRoom(l,pos(9,100,0),3,7,3,false));
            HOMES.add(gardenRoom(l,pos(0,100,10),5,3,5,true));
            HOMES.add(gardenRoom(l,pos(10,100,10),6,3,4,false));
            landscape(l);
            String[] names={"Purl","Sough","Tippet","Still"};
            for(int i=0;i<4;i++){
                var h=HOMES.get(i);h.assign(p,ItemStack.EMPTY);ItemStack tag=new ItemStack(Items.NAME_TAG);tag.set(DataComponents.CUSTOM_NAME,Component.literal(names[i]));h.interact(p,tag,false);h.addBreaths(i==0?224:64);h.setItem(HollowHeartEntity.INPUT,new ItemStack(Items.STRING,3));
            }
            l.setBlock(pos(9,104,1),Lararium.PERCH.get().defaultBlockState(),3);
            p.getInventory().setItem(0,new ItemStack(Lararium.FORK.get()));p.getInventory().setItem(1,new ItemStack(Lararium.HEART_ITEM.get()));p.getInventory().setItem(2,new ItemStack(Lararium.VEIL_ITEM.get()));
            p.getInventory().setItem(3,new ItemStack(Lararium.HOOK.get()));p.getInventory().setItem(4,new ItemStack(Lararium.BROOCH.get()));p.getInventory().setItem(5,new ItemStack(Lararium.BELL.get()));p.getInventory().setItem(6,new ItemStack(Lararium.BOOK.get()));p.getInventory().setItem(7,new ItemStack(Items.AMETHYST_SHARD,16));p.getInventory().setItem(8,new ItemStack(Items.STRING,16));
            p.getAbilities().flying=true;p.onUpdateAbilities();
            teleport(p,1.5,101,-3.5,0,12);
        }catch(Throwable x){failures++;LoggerFactory.getLogger("lararium").error("LARARIUMCLIENT setup failed",x);}
    }
    private static HollowHeartEntity gardenRoom(ServerLevel l,BlockPos p,int w,int h,int d,boolean pillar){
        var be=LarariumSelfTest.room(l,p,w,h,d,pillar);BlockPos seed=p.above();
        for(int x=-1;x<=w;x++)for(int z=-1;z<=d;z++){
            l.setBlock(seed.offset(x,h,z),(x==-1||x==w||z==-1||z==d?Blocks.CUT_COPPER.weathering().unaffected():Blocks.GLASS).defaultBlockState(),3);
            if(!(x==0&&z==0))l.setBlock(seed.offset(x,-1,z),Blocks.OAK_PLANKS.defaultBlockState(),3);
        }
        for(int y=0;y<h;y++)for(int x=-1;x<=w;x++)for(int z=-1;z<=d;z++){
            if(x!=-1&&x!=w&&z!=-1&&z!=d)continue;
            boolean corner=(x==-1||x==w)&&(z==-1||z==d);
            Block material=corner?Blocks.STRIPPED_OAK_LOG:y==0?Blocks.CALCITE:y==h-1?Blocks.OAK_PLANKS:Blocks.GLASS;
            l.setBlock(seed.offset(x,y,z),material.defaultBlockState(),3);
        }
        for(int x=-2;x<=w+1;x++)for(int z=-2;z<=d+1;z++)if(x==-2||x==w+1||z==-2||z==d+1)l.setBlock(seed.offset(x,h,z),Blocks.CUT_COPPER_SLAB.waxed().weathered().defaultBlockState(),3);
        for(int y=0;y<2;y++)l.setBlock(seed.offset(w/2,y,-1),Lararium.VEIL.get().defaultBlockState(),3);
        if(pillar)for(int y=0;y<h;y++)l.setBlock(seed.offset(w/2,y,d/2),Blocks.STRIPPED_OAK_LOG.defaultBlockState(),3);
        l.setBlock(p,Blocks.OAK_PLANKS.defaultBlockState(),3);l.setBlock(p.above(),Lararium.HEART.get().defaultBlockState(),3);
        return (HollowHeartEntity)l.getBlockEntity(p.above());
    }
    private static void landscape(ServerLevel l){
        var random=new Random(20261115);
        Block[] flowers={Blocks.ALLIUM,Blocks.AZURE_BLUET,Blocks.OXEYE_DAISY,Blocks.SHORT_GRASS,Blocks.SHORT_GRASS};
        for(int x=-10;x<28;x++)for(int z=-9;z<28;z++){
            if(l.getBlockState(pos(x,100,z)).is(Blocks.GRASS_BLOCK)&&l.getBlockState(pos(x,101,z)).isAir()&&random.nextInt(9)==0)l.setBlock(pos(x,101,z),flowers[random.nextInt(flowers.length)].defaultBlockState(),3);
        }
        for(int[] tree:new int[][]{{-6,12},{-4,23},{9,24},{22,17},{23,5},{18,-7}}){
            int x=tree[0],z=tree[1];
            for(int y=101;y<=105;y++)l.setBlock(pos(x,y,z),Blocks.BIRCH_LOG.defaultBlockState(),3);
            for(int y=103;y<=107;y++)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                if(Math.abs(dx)+Math.abs(dz)+(y==107?2:0)>4||dx==0&&dz==0&&y<=105)continue;
                l.setBlock(pos(x+dx,y,z+dz),Blocks.BIRCH_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true),3);
            }
        }
        for(int[] lamp:new int[][]{{6,-1},{6,9},{6,19},{19,7}}){
            l.setBlock(pos(lamp[0],101,lamp[1]),Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(),3);
            l.setBlock(pos(lamp[0],102,lamp[1]),Blocks.LANTERN.defaultBlockState(),3);
        }
    }
    private static BlockPos pos(int x,int y,int z){return new BlockPos(x,floor+y-100,z);}
    private static Vec3 v(double x,double y,double z){return new Vec3(x,floor+y-100,z);}
    private static void teleport(ServerPlayer p,double x,double y,double z,float yaw,float pitch){p.connection.teleport(x,floor+y-100,z,yaw,pitch);}
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static void run(MinecraftServer s,String c){s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),c);}
    private static void view(Minecraft mc,Vec3 at,Vec3 target){
        Vec3 d=target.subtract(at.add(0,mc.player.getEyeHeight(),0));float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));
        mc.getSingleplayerServer().execute(()->player(mc.getSingleplayerServer()).connection.teleport(at.x,at.y,at.z,yaw,pitch));
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);
    }
    private static void hud(Minecraft mc,boolean hide){if(mc.gui.hud.isHidden()!=hide)mc.gui.hud.toggle();}
    private static void shot(Minecraft mc,String n){Screenshot.grab(mc.gameDirectory,"lararium-"+n+".png",mc.gameRenderer.mainRenderTarget(),1,c->LoggerFactory.getLogger("lararium").info("LARARIUMCLIENT saved {}",n));}
    private static void check(boolean yes,String what){checks++;if(!yes){failures++;LoggerFactory.getLogger("lararium").error("LARARIUMCLIENT FAIL {}",what);}else LoggerFactory.getLogger("lararium").info("LARARIUMCLIENT PASS {}",what);}
    private static void finish(Minecraft mc){try{Files.writeString(mc.gameDirectory.toPath().resolve("lararium-client-result.json"),"{\"passed\":"+(failures==0)+",\"checks\":"+checks+",\"failures\":"+failures+"}");}catch(Exception ignored){}mc.stop();}
    private static void create(Minecraft mc){
        mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);
        String n="lararium-garden-"+System.currentTimeMillis()/1000;
        var settings=new LevelSettings(n,GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(n,settings,new WorldOptions(20261115L,false,false),provider->provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
    }
    private LarariumClientCheck(){}
}
