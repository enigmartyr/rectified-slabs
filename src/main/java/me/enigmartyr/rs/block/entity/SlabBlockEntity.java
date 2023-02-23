package me.enigmartyr.rs.block.entity;

import me.enigmartyr.rs.RectifiedSlabs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Arrays;
import java.util.Optional;


public class SlabBlockEntity extends BlockEntity {
    private Block   mate;
    private boolean flippedFromTick = false;

    public SlabBlockEntity(BlockPos pos, BlockState state) {
        super(RectifiedSlabs.SLAB_BLOCK_ENTITY, pos, state);
    }

    public boolean flippedFromTick() {
        boolean b = flippedFromTick;
        flippedFromTick = false;
        return b;
    }

    @Override
    public void writeNbt(NbtCompound tag) { tag.putString("mate", String.valueOf(Registries.BLOCK.getId(mate))); }

    @Override
    public void readNbt(NbtCompound tag) { Optional.of(Registries.BLOCK.get(new Identifier(tag.getString("mate")))).filter(block -> block instanceof SlabBlock).ifPresent(block -> setMate((SlabBlock) block)); }

    public boolean hasMate() { return mate != null;}

    public void setMate(SlabBlock block) { this.mate = block; }
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = new NbtCompound();
        if(mate != null) tag.putString("mate", Registries.BLOCK.getId(mate).toString());
        return tag;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }


    public boolean isLookingAtMate(PlayerEntity player, BlockState baseState) {
        if(!hasMate()) return false;

        double traceDistance = player.isCreative() ? 5.0f : 4.5f;
        Vec3d vec1 = player.getClientCameraPosVec(1.0f);
        Vec3d vec2 = player.getRotationVecClient();
        Vec3d vec3 = vec1.add(vec2.x * traceDistance, vec2.y * traceDistance, vec2.z * traceDistance);

        HitResult hitResult = player.world.raycast(new RaycastContext(vec1, vec3, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));

        Vec3d lookingPos = hitResult.getPos();

        Direction.AxisDirection xDirection = lookingPos.x > super.pos.getX() + 0.5 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
        Direction.AxisDirection yDirection = lookingPos.y > super.pos.getY() + 0.5 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
        Direction.AxisDirection zDirection = lookingPos.z > super.pos.getZ() + 0.5 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;

        Direction.Axis  axis = baseState.get(Properties.AXIS);
        SlabType    baseType = baseState.get(Properties.SLAB_TYPE);

        return (baseType == SlabType.TOP ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE) != switch (axis) {
            case X -> xDirection;
            case Y -> yDirection;
            case Z -> zDirection;
        };
    }

    public enum FlipFlags {
        CONSUME_BASE, BLOCK_TICKED
    }

    public BlockState flip(BlockState baseState, FlipFlags... flags) {
        BlockState mateState = getMateBlockState(baseState);
        Block        newMate = baseState.getBlock();
        for(FlipFlags flag : Arrays.stream(flags).toList()) switch (flag) {
            case BLOCK_TICKED -> flippedFromTick = true;
            case CONSUME_BASE ->         newMate = null;
        }

        mate = newMate;
        //this.markDirty();
        return mateState;
    }

    public BlockState getMateBlockState(BlockState baseState) {
        BlockState mateBlockState = mate.getDefaultState()
            .with(SlabBlock.TYPE,  baseState.get(SlabBlock.TYPE) == SlabType.BOTTOM ? SlabType.TOP : SlabType.BOTTOM)
            .with(Properties.AXIS, baseState.get(Properties.AXIS))
            .with(Properties.WATERLOGGED, false);
        return mateBlockState;
    }
}
