package com.oculifelis.lararium;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.shapes.*;
public final class ThresholdBlock extends Block {
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    /** True when a veil on the same axis sits on that side, so a wall of veils draws as one cloth. Left and right are seen from the front. */
    public static final BooleanProperty UP=BlockStateProperties.UP, DOWN=BlockStateProperties.DOWN,
            LEFT=BooleanProperty.create("left"), RIGHT=BooleanProperty.create("right");
    public ThresholdBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)
            .setValue(UP,false).setValue(DOWN,false).setValue(LEFT,false).setValue(RIGHT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,UP,DOWN,LEFT,RIGHT);}
    private static Direction left(BlockState s){return s.getValue(FACING).getCounterClockWise();}
    private boolean joins(BlockState self,BlockState other){return other.is(this)&&other.getValue(FACING).getAxis()==self.getValue(FACING).getAxis();}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){
        BlockState s=defaultBlockState().setValue(FACING,c.getHorizontalDirection());
        Level l=c.getLevel();BlockPos p=c.getClickedPos();
        return s.setValue(UP,joins(s,l.getBlockState(p.above()))).setValue(DOWN,joins(s,l.getBlockState(p.below())))
                .setValue(LEFT,joins(s,l.getBlockState(p.relative(left(s))))).setValue(RIGHT,joins(s,l.getBlockState(p.relative(left(s).getOpposite()))));
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction dir,
                                               BlockPos neighbourPos,BlockState neighbourState,RandomSource random){
        if(dir==Direction.UP)return state.setValue(UP,joins(state,neighbourState));
        if(dir==Direction.DOWN)return state.setValue(DOWN,joins(state,neighbourState));
        if(dir==left(state))return state.setValue(LEFT,joins(state,neighbourState));
        if(dir==left(state).getOpposite())return state.setValue(RIGHT,joins(state,neighbourState));
        return state;
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.X?box(7,0,0,9,16,16):box(0,0,7,16,16,9);}
}
