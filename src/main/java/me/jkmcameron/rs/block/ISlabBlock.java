package me.jkmcameron.rs.block;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public interface ISlabBlock
{
    BiMap<SlabType, Direction.AxisDirection> SLAB_DIRECTIONS = new ImmutableBiMap.Builder<SlabType, Direction.AxisDirection>().put(SlabType.BOTTOM, Direction.AxisDirection.NEGATIVE).put(SlabType.TOP, Direction.AxisDirection.POSITIVE).build();
}
