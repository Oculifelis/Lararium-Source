package com.oculifelis.lararium;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.*;
public final class PerchBlock extends Block {
    public PerchBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(3,4,3,13,12,13);}
}
