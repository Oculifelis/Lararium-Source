package com.oculifelis.lararium;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class HollowHeartBlock extends BaseEntityBlock {
    public static final MapCodec<HollowHeartBlock> CODEC=simpleCodec(HollowHeartBlock::new);
    public static final BooleanProperty AWAKE=BooleanProperty.create("awake");
    public static final BooleanProperty PULSE=BooleanProperty.create("pulse");
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE=Shapes.or(box(2,0,2,14,3,14),box(3,3,3,5,12,5),box(11,3,3,13,12,5),box(3,3,11,5,12,13),box(11,3,11,13,12,13),box(3,11,3,13,13,5),box(3,11,11,13,13,13),box(3,11,5,5,13,11),box(11,11,5,13,13,11));
    public HollowHeartBlock(Properties p) { super(p);registerDefaultState(stateDefinition.any().setValue(AWAKE,false).setValue(PULSE,false).setValue(FACING,Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b) { b.add(AWAKE,PULSE,FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new HollowHeartEntity(p,s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t) {
        return createTickerHelper(t,Lararium.HEART_BE.get(),(world,pos,state,be)->be.tick());
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack item) {
        super.setPlacedBy(l,p,s,e,item);
        if(!l.isClientSide()&&e instanceof Player player&&l.getBlockEntity(p) instanceof HollowHeartEntity h)h.assign(player,item);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit) {
        if(l.getBlockEntity(p) instanceof HollowHeartEntity h) { if(!l.isClientSide())h.interact(player,stack,player.isShiftKeyDown());return InteractionResult.SUCCESS; }
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit) {
        if(l.getBlockEntity(p) instanceof HollowHeartEntity h){if(!l.isClientSide())h.interact(player,ItemStack.EMPTY,player.isShiftKeyDown());return InteractionResult.SUCCESS;}return InteractionResult.PASS;
    }
    @Override protected boolean isSignalSource(BlockState s){return true;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction side){return l.getBlockEntity(p) instanceof HollowHeartEntity h &&side.getOpposite()!=s.getValue(FACING)?h.signal():0;}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction side){return l.getBlockEntity(p) instanceof HollowHeartEntity h?Math.min(15,h.breaths()/16):0;}
}
