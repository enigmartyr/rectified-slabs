package me.jkmcameron.rs.block.entity;

import me.jkmcameron.rs.RectifiedSlabs;
import me.jkmcameron.rs.block.ISlabBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import javax.annotation.Nullable;


public class SlabBlockEntity extends BlockEntity
{
    SlabBlock mate;
    BlockState baseBlockState;
    boolean isLookingAtMate;

    public SlabBlockEntity(BlockPos pos, BlockState state)
    {
        super(RectifiedSlabs.SLAB_BLOCK_ENTITY, pos, state);

        baseBlockState = state;
    }

    @Override
    public void writeNbt(NbtCompound tag)
    {
        super.writeNbt(tag);

        tag.putString("mate", Registry.BLOCK.getId(mate).toString());
    }

    @Override
    public void readNbt(NbtCompound tag)
    {
        super.readNbt(tag);

        Block block = Registry.BLOCK.get(new Identifier(tag.getString("mate")));
        this.setMate(block instanceof SlabBlock ? (SlabBlock) block : null);
    }

    public boolean hasMate() { return mate != null;}

    public void setMate(@Nullable SlabBlock block)
    {
        mate = block;

        if(world != null) this.world.updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt()
    {
        NbtCompound tag = new NbtCompound();
        tag.putString("mate", Registry.BLOCK.getId(mate).toString());

        return tag;
    }

    public boolean isLookingAtMate(PlayerEntity player)
    {
        if(!hasMate()) return false;

        double traceDistance = player.isCreative() ? 5.0f : 4.5f;
        Vec3d vec1 = player.getClientCameraPosVec(1.0f);
        Vec3d vec2 = player.getRotationVecClient();
        Vec3d vec3 = vec1.add(vec2.x * traceDistance, vec2.y * traceDistance, vec2.z * traceDistance);

        HitResult hitResult = player.world.raycast(new RaycastContext(vec1, vec3, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));

        Vec3d lookingPos = hitResult.getPos();

        double deltaX = lookingPos.x - (this.pos.getX() + 0.5);
        double deltaY = lookingPos.y - (this.pos.getY() + 0.5);
        double deltaZ = lookingPos.z - (this.pos.getZ() + 0.5);

        Direction.AxisDirection xDirection = deltaX < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
        Direction.AxisDirection yDirection = deltaY < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
        Direction.AxisDirection zDirection = deltaZ < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;

        return (isLookingAtMate = switch (baseBlockState.get(Properties.AXIS))
        {
            case X -> ISlabBlock.SLAB_DIRECTIONS.get(baseBlockState.get(Properties.SLAB_TYPE)) != xDirection;
            case Y -> ISlabBlock.SLAB_DIRECTIONS.get(baseBlockState.get(Properties.SLAB_TYPE)) != yDirection;
            case Z -> ISlabBlock.SLAB_DIRECTIONS.get(baseBlockState.get(Properties.SLAB_TYPE)) != zDirection;
        });
    }

    public BlockState getBaseBlockState()
    {
        return this.baseBlockState;
    }

    public BlockState getMateBlockState()
    {
        BlockState mateBlockState = mate.getDefaultState();
        mateBlockState = baseBlockState.get(SlabBlock.TYPE) == SlabType.BOTTOM ? mateBlockState.with(SlabBlock.TYPE, SlabType.TOP) : mateBlockState.with(SlabBlock.TYPE, SlabType.BOTTOM);
        mateBlockState = mateBlockState.with(Properties.AXIS, baseBlockState.get(Properties.AXIS)).with(Properties.WATERLOGGED, false);

        return mateBlockState;
    }

    public boolean isLookingAtMate()
    {
        return isLookingAtMate;
    }
}
