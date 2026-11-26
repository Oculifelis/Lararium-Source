package com.oculifelis.lararium;

import com.oculifelis.lararium.geometry.HollowShape;
import com.fundamentum.hook.Hooks;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class HollowHeartEntity extends BlockEntity implements WorldlyContainer {
    public static final int INPUT=9, OUTPUT=10, FUEL=11;
    private final NonNullList<ItemStack> items=NonNullList.withSize(12,ItemStack.EMPTY);
    private UUID owner;
    private String name="";
    private int breaths,work,phrase,recordBuffer,recordBeat=-1,lastSignal;
    private String workKey="";
    private String favourite="";
    private long greetingUntil;
    private long phraseStart,paidUntil,homeUntil,nextScan;
    private boolean answersCalls;
    // Require an observed low input before accepting a call, including after a load.
    private boolean sawLowInput;
    private boolean playing,taught,dirty=true,checkingDirty,clientRecording;
    private HollowShape.Result shape=HollowShape.Result.failed("unmeasured");
    private HollowShape.Scan scanner;
    private UUID measuringFor;
    private List<HollowShape.Cell> preview=List.of();
    private long previewUntil;
    private int previewIndex;
    private AABB measuredBounds;
    public HollowHeartEntity(BlockPos p,BlockState s){super(Lararium.HEART_BE.get(),p,s);}
    public UUID owner(){return owner;}
    public BlockPos origin(){return worldPosition.above();}
    public int breaths(){return breaths;}
    public int work(){return work;}
    public int phrase(){return phrase;}
    public String favourite(){return favourite;}
    public Component favouriteName(){return favourite.isEmpty()?Component.translatable("message.lararium.no_favourite"):new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(favourite))).getHoverName();}
    private boolean favours(ItemStack stack){return !favourite.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(favourite);}
    private static String validFavourite(String value){
        if(value.length()>256)return "";
        var id=Identifier.tryParse(value);
        return id!=null&&BuiltInRegistries.ITEM.containsKey(id)&&BuiltInRegistries.ITEM.getValue(id)!=Items.AIR?id.toString():"";
    }
    public boolean recording(){return level!=null&&level.isClientSide()?clientRecording:recordBeat>=0;}
    public boolean playing(){return playing;}
    public HollowShape.Form form(){return shape.form();}
    public String problem(){return scanner!=null?"measuring":shape.problem();}
    public boolean awake(){return !dirty&&shape.valid()&&(scanner==null||!checkingDirty);}
    boolean reservesSpace(){return shape.valid()&&!isRemoved();}
    boolean canReceiveBreath(){return breaths<capacity();}
    public String personalName(){return name;}
    public Component displayName(){return name.isBlank()?Component.translatable("form.lararium."+form().name().toLowerCase(Locale.ROOT)):Component.literal(name);}
    public int stored(){int n=0;for(int i=0;i<9;i++)if(!items.get(i).isEmpty())n++;return n;}
    public boolean controls(Player p){return owner==null||owner.equals(p.getUUID());}
    public boolean friend(Player p){if(controls(p))return true;var own=level.getPlayerByUUID(owner);return own!=null&&own.isAlliedTo(p);}
    public boolean contains(BlockPos p){return shape.cells().contains(new HollowShape.Cell(p.getX()-origin().getX(),p.getY()-origin().getY(),p.getZ()-origin().getZ()));}
    public int signal(){
        if(level==null||!awake())return 0;
        if(form()==HollowShape.Form.NOOK)return level.getGameTime()<greetingUntil?15:0;
        return form()==HollowShape.Form.LOOP&&playing&&!recording()&&level.getGameTime()<paidUntil&&((phrase>>>beat())&1)!=0?15:0;
    }
    public boolean answersCalls() { return answersCalls; }

    public String answerStatus() {
        if (!playing) return "message.lararium.paused";
        if (level != null && level.getGameTime() < paidUntil) return "message.lararium.answer_busy";
        return "message.lararium.answer_ready";
    }

    private int beat(){return (int)Math.floorMod((level.getGameTime()-phraseStart)/10,8);}
    public boolean homecoming(){return level!=null&&homeUntil>level.getGameTime();}
    public void joinHomecoming(long until){homeUntil=until;note(new float[]{.8F,1F,1.2F,1.6F}[form().ordinal()]);changed();}
    public void addBreaths(int amount){breaths=amount<0?Math.max(0,breaths+amount):Math.clamp(breaths+amount,0,capacity());changed();}
    private int capacity(){return LarariumData.rule("capacity",256,32,4096);}
    public void invalidate(){dirty=true;}
    public void assign(Player p,ItemStack stack){owner=p.getUUID();if(stack.has(DataComponents.CUSTOM_NAME))name=boundedName(stack.getHoverName().getString());changed();}
    private static String boundedName(String n){return n.substring(0,Math.min(n.length(),64));}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide()){LarariumParticles.tick(this);return;}
        HollowHomes.track(this);
        long now=level.getGameTime();
        if(scanner!=null&&dirty)scanner=null;
        if(scanner==null&&(dirty||now>=nextScan))beginScan();
        if(scanner!=null){int budget=HollowHomes.scanBudget(level);if(budget>0)scanner.step(budget);if(scanner.done())finishScan();}
        if(now<previewUntil&&now%3==0)showStitches();
        if(now%10==Math.floorMod(worldPosition.asLong(),10)){
            if(awake()){
                refuel();weave();
                switch(form()){
                    case NOOK->keep();case SPIRE,VAULT->{}case LOOP->listen();
                }
                if(homecoming()&&now%20==Math.floorMod(worldPosition.asLong(),20))HollowHomes.share(this);
            }
        }
        if (answersCalls) tickAnswer(now);
        int out=signal();if(out!=lastSignal||getBlockState().getValue(HollowHeartBlock.PULSE)!=(out>0)){lastSignal=out;level.setBlock(worldPosition,getBlockState().setValue(HollowHeartBlock.PULSE,out>0),Block.UPDATE_CLIENTS);level.updateNeighborsAt(worldPosition,getBlockState().getBlock());if(out>0)note(1.3F);changed();}
        if(awake()){if(form()==HollowShape.Form.SPIRE)breathe();else if(form()==HollowShape.Form.VAULT)shelter();}
    }
    private void beginScan(){
        checkingDirty=dirty;dirty=false;if(checkingDirty){greetingUntil=0;setAwake(false);}
        int r=LarariumData.rule("range",8,3,12),height=LarariumData.rule("height",12,5,20),cap=LarariumData.rule("max_cells",512,16,2048);
        scanner=new HollowShape.Scan(p->{
            BlockPos at=origin().offset(p.x(),p.y(),p.z());
            if(!level.hasChunkAt(at))return HollowShape.Material.UNLOADED;
            BlockState s=level.getBlockState(at);
            if(s.is(Lararium.VEIL.get())||s.getBlock() instanceof DoorBlock||s.getBlock() instanceof TrapDoorBlock)return HollowShape.Material.WALL;
            return s.getFluidState().isEmpty()&&s.getCollisionShape(level,at).isEmpty()?HollowShape.Material.AIR:HollowShape.Material.WALL;
        },r,height,cap,LarariumData.criteria());
    }
    private void finishScan(){
        var found=scanner.result();var path=scanner.diagnosticPath();scanner=null;
        shape=found.valid()&&HollowHomes.overlaps(this,found.cells())?HollowShape.Result.failed("overlap"):found;
        if(shape.valid()){
            int x0=Integer.MAX_VALUE,y0=x0,z0=x0,x1=Integer.MIN_VALUE,y1=x1,z1=x1;
            for(var c:shape.cells()){x0=Math.min(x0,c.x());y0=Math.min(y0,c.y());z0=Math.min(z0,c.z());x1=Math.max(x1,c.x());y1=Math.max(y1,c.y());z1=Math.max(z1,c.z());}
            measuredBounds=new AABB(Vec3.atLowerCornerOf(origin().offset(x0,y0,z0)),Vec3.atLowerCornerOf(origin().offset(x1+1,y1+1,z1+1)));
        }
        nextScan=level.getGameTime()+100+Math.floorMod(worldPosition.asLong(),20);
        setAwake(shape.valid());
        if(shape.valid()&&owner!=null&&level.getPlayerByUUID(owner) instanceof ServerPlayer p)com.fundamentum.unlock.Unlocks.grant(p,com.fundamentum.port.FId.of("lararium","awaken_"+form().name().toLowerCase(Locale.ROOT)));
        if (!shape.valid() || shape.form() != HollowShape.Form.LOOP) {
            recordBeat = -1;
            if (answersCalls) { paidUntil = 0; sawLowInput = false; }
        }
        changed();
        if(measuringFor!=null){
            preview=found.valid()?found.cells().stream().sorted(Comparator.comparingInt(HollowShape.Cell::y).thenComparingInt(HollowShape.Cell::x).thenComparingInt(HollowShape.Cell::z)).toList():path;
            previewUntil=level.getGameTime()+60;previewIndex=0;
            var p=level.getPlayerByUUID(measuringFor);if(p!=null)status(p);measuringFor=null;if(awake())note(1.1F);
        }
    }
    private void showStitches(){
        if(!(level instanceof ServerLevel s)||preview.isEmpty()||!HollowHomes.effectBudget(level,8))return;
        var dust=new DustParticleOptions(shape.valid()?0x96D7C1:0xEDAF7B,.7F);
        for(int i=0;i<8;i++){var c=preview.get((previewIndex++)%preview.size());var p=origin().offset(c.x(),c.y(),c.z());s.sendParticles(dust,p.getX()+.5,p.getY()+.15,p.getZ()+.5,1,0,0,0,0);}
    }
    private void setAwake(boolean awake){var s=getBlockState();if(s.getValue(HollowHeartBlock.AWAKE)!=awake)level.setBlock(worldPosition,s.setValue(HollowHeartBlock.AWAKE,awake),Block.UPDATE_CLIENTS);}
    public void measure(Player p){measuringFor=p.getUUID();invalidate();p.sendOverlayMessage(Component.translatable("message.lararium.measuring"));}
    public void status(Player p){
        if(!awake())p.sendOverlayMessage(Component.translatable("problem.lararium."+problem()));
        else {
            var text=Component.translatable("message.lararium.status",displayName(),shape.width(),shape.height(),shape.depth(),breaths,stored());
            if(form()==HollowShape.Form.NOOK&&!favourite.isEmpty())text.append(" · ").append(Component.translatable("message.lararium.favourite",favouriteName()));
            if (form() == HollowShape.Form.LOOP && answersCalls && !recording())
                text.append(" · ").append(Component.translatable(answerStatus()));
            p.sendOverlayMessage(text);
        }
    }
    public void interact(Player p,ItemStack held,boolean sneaking){
        if(!controls(p)){p.sendOverlayMessage(Component.translatable("message.lararium.personal"));return;}
        if(owner==null)owner=p.getUUID();
        if(held.is(Lararium.FORK.get())){
            if(sneaking&&awake()&&form()==HollowShape.Form.NOOK){
                var sample=p.getOffhandItem();
                favourite=sample.isEmpty()?"":validFavourite(BuiltInRegistries.ITEM.getKey(sample.getItem()).toString());
                greetingUntil=0;
                p.sendOverlayMessage(favourite.isEmpty()?Component.translatable("message.lararium.favourite_cleared"):Component.translatable("message.lararium.favourite_taught",displayName(),favouriteName()));
                changed();note(1.1F);
            }else if(sneaking&&awake()&&form()==HollowShape.Form.LOOP){
                if (p.getOffhandItem().is(Lararium.THREAD.get())) {
                    changePlaybackMode(p);
                    return;
                }
                if(p.getOffhandItem().is(Items.REDSTONE)){startTeaching(p);}
                else if(recording()){recordBeat=-1;playing=taught;paidUntil=0;p.sendOverlayMessage(Component.translatable(taught ? playbackMessage() : "message.lararium.paused"));}
                else if(!taught){startTeaching(p);}
                else if(playing){playing=false;p.sendOverlayMessage(Component.translatable("message.lararium.paused"));}
                else{playing=true;paidUntil=0;sawLowInput=false;p.sendOverlayMessage(Component.translatable(playbackMessage()));}
                changed();
            }else measure(p);
            return;
        }
        if(held.is(Items.NAME_TAG)&&held.has(DataComponents.CUSTOM_NAME)){
            name=boundedName(held.getHoverName().getString());
            if(!p.isCreative())held.shrink(1);note(1.4F);changed();status(p);return;
        }
        if(held.is(Items.AMETHYST_SHARD)){
            int gain=LarariumData.rule("shard_breaths",32,1,256);
            if(breaths+gain<=capacity()){addBreaths(gain);if(!p.isCreative())held.shrink(1);note(.85F);}status(p);return;
        }
        if(!held.isEmpty()&&LarariumData.recipe(held).isPresent()){
            ItemStack into=items.get(INPUT);int n=into.isEmpty()?Math.min(64,held.getCount()):ItemStack.isSameItemSameComponents(into,held)?Math.min(held.getCount(),into.getMaxStackSize()-into.getCount()):0;
            if(n>0){if(into.isEmpty())items.set(INPUT,held.copyWithCount(n));else into.grow(n);if(!p.isCreative())held.shrink(n);changed();note(1.2F);}return;
        }
        if(held.isEmpty()){
            int first=sneaking&&!items.get(INPUT).isEmpty()?INPUT:sneaking&&!items.get(FUEL).isEmpty()?FUEL:!items.get(OUTPUT).isEmpty()?OUTPUT:-1;
            if(first<0)for(int i=0;i<9;i++)if(!items.get(i).isEmpty()){first=i;break;}
            if(first>=0){ItemStack out=items.get(first);items.set(first,ItemStack.EMPTY);if(!p.getInventory().add(out))p.drop(out,false);changed();note(1.25F);}else status(p);
        }else status(p);
    }
    private String playbackMessage() {
        return answersCalls ? "message.lararium.answer_ready" : "message.lararium.playback";
    }

    private void changePlaybackMode(Player player) {
        if (recording() || !taught) {
            player.sendOverlayMessage(Component.translatable(recording()
                ? "message.lararium.answer_recording" : "message.lararium.answer_learn"));
            return;
        }
        answersCalls = !answersCalls;
        playing = true;
        paidUntil = 0;
        sawLowInput = false;
        player.sendOverlayMessage(Component.translatable(playbackMessage()));
        changed();
    }

    private boolean markedFacePowered() {
        Direction face = getBlockState().getValue(HollowHeartBlock.FACING);
        return level.getSignal(worldPosition.relative(face), face) > 0;
    }

    private void tickAnswer(long now) {
        if (!awake() || form() != HollowShape.Form.LOOP || !playing || recording()) {
            sawLowInput = false;
            return;
        }
        boolean powered = markedFacePowered();
        boolean rising = powered && sawLowInput;
        sawLowInput = !powered;
        if (rising && now >= paidUntil && breaths > 0) beginPhrase(now);
    }

    private void beginPhrase(long now) {
        breaths--;
        phraseStart = now;
        paidUntil = now + 80;
        changed();
    }

    private void startTeaching(Player p){recordBuffer=0;recordBeat=0;phraseStart=level.getGameTime();playing=false;paidUntil=0;p.sendOverlayMessage(Component.translatable("message.lararium.recording"));}
    private void refuel(){int gain=LarariumData.rule("shard_breaths",32,1,256);if(items.get(FUEL).is(Items.AMETHYST_SHARD)&&breaths+gain<=capacity()){items.get(FUEL).shrink(1);addBreaths(gain);}}
    private void weave(){
        var recipe=LarariumData.recipe(items.get(INPUT));
        if(recipe.isEmpty()){if(work!=0){work=0;workKey="";changed();}return;}
        var r=recipe.get();ItemStack result=r.result(),out=items.get(OUTPUT);
        String key=r.toString();if(!key.equals(workKey)){work=0;workKey=key;}
        if(result.isEmpty()||breaths<r.breaths()||(!out.isEmpty()&&(!ItemStack.isSameItemSameComponents(out,result)||out.getCount()+result.getCount()>out.getMaxStackSize())))return;
        work+=10;
        if(work>=r.ticks()){
            items.get(INPUT).shrink(1);if(out.isEmpty())items.set(OUTPUT,result);else out.grow(result.getCount());
            breaths-=r.breaths();work=0;note(.9F);
        }
        changed();
    }
    private AABB bounds(){return measuredBounds==null?new AABB(worldPosition):measuredBounds;}
    private void keep(){
        if(breaths<=0)return;
        var offerings=level.getEntitiesOfClass(ItemEntity.class,bounds().minmax(new AABB(worldPosition)),e->e.isAlive()&&e.getAge()>=10&&(contains(e.blockPosition())||e.blockPosition().equals(worldPosition))&&e.getOwner()!=null&&Objects.equals(owner,e.getOwner().getUUID()));
        offerings.sort(Comparator.<ItemEntity>comparingInt(e->favours(e.getItem())?0:1).thenComparing(Comparator.comparingInt(ItemEntity::getAge).reversed()).thenComparingInt(ItemEntity::getId));
        for(ItemEntity item:offerings){
            ItemStack stack=item.getItem();int before=stack.getCount();
            boolean familiar=favours(stack);
            for(int i=0;i<9&&!stack.isEmpty();i++){
                ItemStack into=items.get(i);if(into.isEmpty()){items.set(i,stack.copy());stack.setCount(0);}
                else if(ItemStack.isSameItemSameComponents(into,stack)){int n=Math.min(into.getMaxStackSize()-into.getCount(),stack.getCount());into.grow(n);stack.shrink(n);}
            }
            if(before!=stack.getCount()){breaths--;if(familiar)greetingUntil=level.getGameTime()+20;if(stack.isEmpty())item.discard();else item.setItem(stack);changed();note(familiar?1.5F:1.3F);return;}
        }
    }
    private void breathe(){
        for(Player p:level.getEntitiesOfClass(Player.class,bounds(),q->friend(q)&&!q.isSpectator()&&contains(q.blockPosition())&&!q.isShiftKeyDown())){
            if(Hooks.latched(p))continue;
            boolean jumping=p instanceof ServerPlayer sp&&sp.getLastClientInput().jump();
            if(jumping || p.getDeltaMovement().y>.06 || p.getPersistentData().getLongOr("lararium_lifting",0L)>level.getGameTime()){
                long until=p.getPersistentData().getLongOr("lararium_lifting",0L);
                if(until<=level.getGameTime()){if(breaths<=0)continue;breaths--;p.getPersistentData().putLong("lararium_lifting",level.getGameTime()+LarariumData.rule("lift_ticks",30,10,100));note(.8F);changed();}
                p.setDeltaMovement(p.getDeltaMovement().x,Math.max(LarariumData.decimal("lift_speed",.32,.1,.6),p.getDeltaMovement().y),p.getDeltaMovement().z);p.hurtMarked=true;p.fallDistance=0;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,35,0,false,false,true));
            }
        }
    }
    private void shelter(){
        if(breaths<=0)return;
        int caught=0;
        for(Projectile p:level.getEntitiesOfClass(Projectile.class,bounds(),q->contains(q.blockPosition())&&!q.getPersistentData().getBooleanOr("lararium_sheltered",false))){
            if(p.getOwner() instanceof Player shooter&&friend(shooter))continue;
            if(p.getType().builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,Lararium.id("ward_immune"))))continue;
            p.setDeltaMovement(p.getDeltaMovement().scale(LarariumData.decimal("shelter_multiplier",.28,.05,.9)));p.hurtMarked=true;p.getPersistentData().putBoolean("lararium_sheltered",true);breaths--;caught++;
            if(breaths<=0||caught>=8)break;
        }
        if(caught>0){changed();note(.6F);}
    }
    private void listen(){
        long elapsed=level.getGameTime()-phraseStart;
        if(recording()&&elapsed>=80){recordBeat=-1;phrase=recordBuffer;taught=true;playing=true;paidUntil=0;changed();}
        else if(recording()&&recordBeat<8&&elapsed>=recordBeat*10L){
            boolean high = markedFacePowered();
            if(high)recordBuffer|=1<<recordBeat;
            recordBeat++;note(high?1.4F:.8F);
            changed();
        }
        if (!answersCalls && !recording() && playing && breaths > 0 && level.getGameTime() >= paidUntil)
            beginPhrase(level.getGameTime());
    }
    public void threadTo(HollowHeartEntity other){
        if(level instanceof ServerLevel s&&HollowHomes.effectBudget(level,12)){Vec3 a=Vec3.atCenterOf(worldPosition).add(0,.5,0),b=Vec3.atCenterOf(other.worldPosition).add(0,.5,0);for(int i=0;i<12;i++){Vec3 p=a.lerp(b,i/12.);s.sendParticles(ParticleTypes.ENCHANT,p.x,p.y,p.z,1,0,0,0,0);}}
    }
    private void note(float pitch){if(!HollowHomes.effectBudget(level,1))return;float voice=.994F+Math.floorMod(name.isEmpty()?worldPosition.hashCode():name.hashCode(),13)*.001F;var moon=com.fundamentum.moon.Moons.current(level);if(moon.isPresent()&&moon.get().id().startsWith("ostentum:"))voice*=moon.get().id().contains("enchanted")?1.12F:moon.get().id().contains("glimmer")?1.06F:.94F;level.playSound(null,worldPosition,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,.18F,pitch*voice);}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override public void setRemoved(){HollowHomes.forget(this);super.setRemoved();}
    @Override public void preRemoveSideEffects(BlockPos p,BlockState replacement){
        if(level!=null&&!level.isClientSide()&&!replacement.is(Lararium.HEART.get())){Containers.dropContents(level,worldPosition,this);clearContent();HollowHomes.forget(this);level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}
        super.preRemoveSideEffects(p,replacement);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putInt("schema",2);if(owner!=null)out.putString("owner",owner.toString());out.putString("name",name);out.putString("favourite",favourite);
        out.putBoolean("answers_calls", answersCalls);
        out.putInt("breaths",breaths);out.putInt("work",work);out.putString("work_key",workKey);out.putInt("phrase",phrase);out.putLong("phrase_start",phraseStart);out.putLong("paid_until",paidUntil);out.putBoolean("playing",playing);out.putBoolean("taught",taught);out.putLong("home_until",homeUntil);
        out.putInt("display_form",form().ordinal());ContainerHelper.saveAllItems(out,items);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);try{owner=UUID.fromString(in.getStringOr("owner",""));}catch(IllegalArgumentException e){owner=null;}
        name=in.getStringOr("name","");if(name.length()>64)name=name.substring(0,64);
        favourite=validFavourite(in.getStringOr("favourite",""));greetingUntil=0;
        breaths=Math.clamp(in.getIntOr("breaths",0),0,4096);work=Math.clamp(in.getIntOr("work",0),0,72000);workKey=in.getStringOr("work_key","");phrase=in.getIntOr("phrase",0)&255;phraseStart=in.getLongOr("phrase_start",0);
        playing=in.getBooleanOr("playing",false);taught=in.getBooleanOr("taught",phrase!=0);paidUntil=in.getLongOr("paid_until",0);homeUntil=in.getLongOr("home_until",0);recordBeat=-1;
        answersCalls = in.getBooleanOr("answers_calls", false);
        sawLowInput = false;
        if (answersCalls) paidUntil = 0;
        ContainerHelper.loadAllItems(in,items);
        if(level!=null&&level.isClientSide()){int f=Math.clamp(in.getIntOr("display_form",0),0,3);shape=new HollowShape.Result(Set.of(),HollowShape.Form.values()[f],"",0,0,0);clientRecording=in.getBooleanOr("recording",false);dirty=false;}
        else{dirty=true;shape=HollowShape.Result.failed("unmeasured");}
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){
        super.collectImplicitComponents(b);if(!name.isEmpty())b.set(DataComponents.CUSTOM_NAME,Component.literal(name));
        var memory=new CompoundTag();memory.putInt("schema",2);memory.putInt("breaths",breaths);memory.putInt("phrase",phrase);memory.putBoolean("taught",taught);memory.putBoolean("playing",playing);memory.putString("favourite",favourite);
        memory.putBoolean("answers_calls", answersCalls);
        var old=components().get(DataComponents.CUSTOM_DATA);var root=old==null?new CompoundTag():old.copyTag();root.put("lararium_memory",memory);
        b.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
    }
    @Override protected void applyImplicitComponents(DataComponentGetter c){
        super.applyImplicitComponents(c);var n=c.get(DataComponents.CUSTOM_NAME);if(n!=null)name=boundedName(n.getString());
        var data=c.get(DataComponents.CUSTOM_DATA);
        if(data!=null){var memory=data.copyTag().getCompoundOrEmpty("lararium_memory");int schema=memory.getIntOr("schema",0);if(schema==1||schema==2){answersCalls=memory.getBooleanOr("answers_calls",false);sawLowInput=false;breaths=Math.clamp(memory.getIntOr("breaths",0),0,4096);phrase=memory.getIntOr("phrase",0)&255;taught=memory.getBooleanOr("taught",false);playing=memory.getBooleanOr("playing",false);paidUntil=0;favourite=validFavourite(memory.getStringOr("favourite",""));greetingUntil=0;}}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider p){var tag=new CompoundTag();tag.putString("name",name);tag.putInt("breaths",breaths);tag.putInt("display_form",form().ordinal());tag.putBoolean("recording",recording());return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var r=ContainerHelper.removeItem(items,slot,count);if(!r.isEmpty())changed();return r;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack s){items.set(slot,s);if(s.getCount()>getMaxStackSize(s))s.setCount(getMaxStackSize(s));changed();}
    @Override public boolean stillValid(Player p){return controls(p)&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public int[] getSlotsForFace(Direction d){return d==Direction.DOWN?new int[]{OUTPUT}:new int[]{INPUT,FUEL};}
    @Override public boolean canPlaceItem(int slot,ItemStack s){return slot==FUEL?s.is(Items.AMETHYST_SHARD):slot==INPUT&&LarariumData.recipe(s).isPresent();}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack s,Direction d){return d!=Direction.DOWN&&canPlaceItem(slot,s);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack s,Direction d){return slot==OUTPUT&&d==Direction.DOWN;}
}
