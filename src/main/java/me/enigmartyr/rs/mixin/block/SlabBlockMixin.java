package me.enigmartyr.rs.mixin.block;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import me.enigmartyr.rs.sound.SlabBlockSoundGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(SlabBlock.class)
public class SlabBlockMixin extends Block implements Waterloggable, BlockEntityProvider
{
    private static final EnumProperty<Direction.Axis> AXIS        = Properties.AXIS;
    @Shadow
    public  static final EnumProperty<SlabType>       TYPE        = Properties.SLAB_TYPE;
    @Shadow
    public  static final BooleanProperty              WATERLOGGED = Properties.WATERLOGGED;
    @Shadow
    protected static final VoxelShape    TOP_SHAPE = Block.createCuboidShape(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);
    @Shadow
    protected static final VoxelShape BOTTOM_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private   static final VoxelShape   LEFT_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
    private   static final VoxelShape  RIGHT_SHAPE = Block.createCuboidShape(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private   static final VoxelShape  FRONT_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    private   static final VoxelShape   BACK_SHAPE = Block.createCuboidShape(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
    private SlabBlockSoundGroup soundGroup;

    public SlabBlockMixin(Settings settings)
	{
        super(settings);
	}

    @Inject(method = "<init>", at = @At("TAIL"))
    public void rectifiedslabs_init(AbstractBlock.Settings settings, CallbackInfo info) {
        this.setDefaultState(this.getDefaultState().with(TYPE, SlabType.BOTTOM).with(AXIS, Direction.Axis.Y).with(WATERLOGGED, false));
        this.soundGroup = new SlabBlockSoundGroup(super.soundGroup);
    }
    @Override
    public boolean hasRandomTicks(BlockState state) { return true; }
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        SlabBlockEntity blockEntity = (SlabBlockEntity) world.getBlockEntity(pos);
        if(!blockEntity.flippedFromTick() && blockEntity.hasMate()) {
            world.setBlockState(pos, blockEntity.flip(state, SlabBlockEntity.FlipFlags.BLOCK_TICKED));
            world.getBlockState(pos).randomTick(world, pos, random);
        }
    }

    @Inject(method = "appendProperties", at = @At("TAIL"))
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo info) { builder.add(AXIS); }

    private boolean isFullBlock(BlockState state, SlabBlockEntity blockEntity) { return state.get(TYPE) == SlabType.DOUBLE || (blockEntity != null && blockEntity.hasMate()); }

    /**
     * @author enigmartyr
     * @reason Simple helper function: determines what the orientation of a slab should be
     *         based on where the player's crosshair falls on the block placed against.
     */
    private Optional<Pair<Direction.Axis, SlabType>> getResultingSlab(BlockPos blockPos, Vec3d crosshairIntersection, Direction blockFace, Direction.Axis faceAxis) {
        double deltaX = crosshairIntersection.x - blockPos.getX();
        double deltaY = crosshairIntersection.y - blockPos.getY();
        double deltaZ = crosshairIntersection.z - blockPos.getZ();

        double relX = ((faceAxis == Direction.Axis.X ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.POSITIVE ? 1 - deltaZ : deltaZ))
                    + ((faceAxis == Direction.Axis.Y ? 1 : 0) * deltaX)
                    + ((faceAxis == Direction.Axis.Z ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.NEGATIVE ? 1 - deltaX : deltaX));
        double relY = ((faceAxis == Direction.Axis.X ? 1 : 0) * deltaY)
                    + ((faceAxis == Direction.Axis.Y ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.POSITIVE ? 1 - deltaZ : deltaZ))
                    + ((faceAxis == Direction.Axis.Z ? 1 : 0) * deltaY);

        double normX = Math.abs(relX - 0.5);
        double normY = Math.abs(relY - 0.5);

        if(normX < 0.25 && normY < 0.25 )  return Optional.empty();

        Direction.Axis oddAxis = normX >= normY ? Direction.Axis.X : Direction.Axis.Y;
        return Optional.of(new Pair<>(faceAxis != oddAxis ? oddAxis : Direction.Axis.Z, (faceAxis == oddAxis ? deltaZ : (normX >= normY ? deltaX : deltaY)) > 0.5 ? SlabType.TOP : SlabType.BOTTOM));
    }

    /**
     * @author enigmartyr
     * @reason Simple helper function: determines what the shape of a slab should be from its axis and
     *         slab type (top,bottom). Additionally, it may be told to return the opposite shape (as is
     *         required for the mate slab).
     */
    private VoxelShape getSlabShape(BlockState state, boolean getOpposite) {
        return switch (state.get(AXIS)) {
            case X -> state.get(TYPE) == (getOpposite ? SlabType.TOP : SlabType.BOTTOM) ? LEFT_SHAPE: RIGHT_SHAPE;
            case Y -> state.get(TYPE) == (getOpposite ? SlabType.TOP : SlabType.BOTTOM) ? BOTTOM_SHAPE: TOP_SHAPE;
            case Z -> state.get(TYPE) == (getOpposite ? SlabType.TOP : SlabType.BOTTOM) ? FRONT_SHAPE: BACK_SHAPE;
        };
    }

    /**
     * @author enigmartyr
     * @reason Determines what the block outline should be when a player's crosshair is on the block; in
     *         the case of this mod, the base slab and mate slab are treated like 2 separate blocks by
     *         calculating which half of the full double slab is being looked at.
     */
    @Overwrite
    @Environment(EnvType.CLIENT)
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return (state.get(TYPE) == SlabType.DOUBLE) ? VoxelShapes.fullCube() : getSlabShape(state, blockEntity != null && ((SlabBlockEntity) blockEntity).isLookingAtMate(MinecraftClient.getInstance().player, state));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return isFullBlock(state, (SlabBlockEntity) world.getBlockEntity(pos)) ? VoxelShapes.fullCube() : getSlabShape(state, false);
    }

    /**
     * @author enigmartyr
     * @reason Determines the slab orientation upon placement.
     */
    @Overwrite
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos blockPos = context.getBlockPos();
        SlabBlockEntity blockEntity = ((SlabBlockEntity) context.getWorld().getBlockEntity(blockPos));

        BlockState currentState = context.getWorld().getBlockState(blockPos);
        FluidState   fluidState = context.getWorld().getFluidState(blockPos);
        Direction     blockFace = context.getSide();
        Vec3d      intersection = context.getHitPos();
        Direction.Axis faceAxis = blockFace.getAxis();

        Direction.Axis slabAxis;
        SlabType       slabType;


        Optional<Pair<Direction.Axis, SlabType>> checkResultingSlab = getResultingSlab(blockPos, intersection, blockFace, faceAxis);
        if(checkResultingSlab.isEmpty()) {
            slabAxis = faceAxis;
            slabType = blockFace.getDirection().getOpposite() == Direction.AxisDirection.POSITIVE ? SlabType.TOP : SlabType.BOTTOM;
            if (currentState.getBlock() instanceof SlabBlock && slabAxis == currentState.get(AXIS) && slabType == currentState.get(TYPE)) {
                blockEntity.setMate((SlabBlock) (Object) this);
                context.getWorld().playSound(context.getPlayer(), blockPos, this.soundGroup.getPlaceSound(), SoundCategory.BLOCKS, (this.soundGroup.getVolume() + 1.0f) / 2.0f, this.soundGroup.getPitch() * 0.8f);
                context.getPlayer().swingHand(context.getHand());
                context.getPlayer().getEquippedStack(context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND).increment(-1);
                return currentState.with(WATERLOGGED, false);
            }
        } else {
            Pair<Direction.Axis, SlabType> resultingSlab = checkResultingSlab.get();
            slabAxis = resultingSlab.getLeft();
            slabType = resultingSlab.getRight();
        }

        return this.getDefaultState().with(AXIS, slabAxis).with(TYPE, slabType).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    /**
     * @author enigmartyr
     * @reason Determines whether the slab block should be made into a mixed double-slab block
     *         when interacted with while a slab block item is held in hand.
     */
    @Overwrite
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        Direction.Axis    axis = state.get(AXIS);
        Direction    blockFace = context.getSide();
        Vec3d     intersection = context.getHitPos();
        BlockPos blockPosition = context.getBlockPos();

        return getResultingSlab(blockPosition, intersection, blockFace, axis).isEmpty() && !isFullBlock(context.getWorld().getBlockState(blockPosition), (SlabBlockEntity) context.getWorld().getBlockEntity(blockPosition));
    }

    /**
     * @author enigmartyr
     * @reason Required so that the sound produced subsequently is always drawn from the top slab in the
     *         case of a y-axis mixed slab, and drawn form either the base or the mate slab at random in
     *         the case of x- or z-axis mixed slabs.
     */
    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        SlabBlockEntity blockEntity = ((SlabBlockEntity) world.getBlockEntity(pos));
        if(blockEntity.hasMate()) this.soundGroup.primeFallSound(blockEntity.getMateBlockState(state));
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }

    /**
     * @author enigmartyr
     * @reason Required so that the sound produced subsequently is always drawn from the top slab in the
     *         case of a y-axis mixed slab, and drawn form either the base or the mate slab at random in
     *         the case of x- or z-axis mixed slabs.
     */
    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        SlabBlockEntity blockEntity = ((SlabBlockEntity) world.getBlockEntity(pos));
        if(blockEntity.hasMate()) this.soundGroup.primeStepSound(blockEntity.getMateBlockState(state));
        super.onSteppedOn(world, pos, state, entity);
    }

    /**
     * @author enigmartyr
     * @reason Ensures that when the base slab of a mixed double slab is destroyed, the mate slab then replaces
     *         it and becomes the new base slab.
     */
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        world.playSound(player, pos, soundGroup.getBreakSound(), SoundCategory.BLOCKS, (soundGroup.getVolume() + 1.0f) / 2.0f, soundGroup.getPitch() * 0.8f);

        SlabBlockEntity slabBlockEntity = ((SlabBlockEntity) world.getBlockEntity(pos));
        if(slabBlockEntity.hasMate()) {
            world.setBlockState(pos, slabBlockEntity.flip(state, SlabBlockEntity.FlipFlags.CONSUME_BASE), Block.NO_REDRAW, Block.NOTIFY_LISTENERS);
            //if(!world.isClient) ((ServerWorld) world).getChunkManager().markForUpdate(pos);
        }
    }

    /**
     * @author enigmartyr
     * @reason Makes it such that the BlockEntity at this world position is removed in every case that the
     *         BlockState at this position is replaced, with the sole exception being when the BlockState
     *         being replaced is a consequence of the mixed slab being flipped upon a player initiating a
     *         block break on the mate slab (this is determined by whether the new BlockState is still a
     *         slab block).
     */
    @Override
    public void onStateReplaced(BlockState oldState, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!(newState.getBlock() instanceof SlabBlock)) {
            world.removeBlockEntity(pos);
        } else {
            //((ServerWorld)world).getChunkManager().getChunkHolder(new ChunkPos(pos).toLong()).sendPacketToPlayersWatching(new BlockUpdateS2CPacket(pos, newState), false);
            //world.getServer().getPlayerManager().sendToAll(new BlockUpdateS2CPacket(pos, newState));
            //world.getServer().getPlayerManager().sendToAll(world.getBlockEntity(pos).toUpdatePacket());
        }
    }

    /**
     * @author enigmartyr
     * @reason A custom SlabBlockSoundGroup class was created due to the need for the ability to determine the
     *         appropriate sound, which at times may be a sound belonging to the mate block (such as when
     *         walking or landing on a mixed vertical slab), and this functionality is not possible with the
     *         vanilla BlockSoundGroup class.
     */
    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) { return this.soundGroup; }
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new SlabBlockEntity(pos, state); }
    @Override
    public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!isFullBlock(state, (SlabBlockEntity) world.getBlockEntity(pos)) && !state.get(Properties.WATERLOGGED).booleanValue() && fluidState.getFluid() == Fluids.WATER) {
            if (!world.isClient()) {
                world.setBlockState(pos, state.with(Properties.WATERLOGGED, true), Block.NOTIFY_ALL);
                world.scheduleFluidTick(pos, fluidState.getFluid(), fluidState.getFluid().getTickRate(world));
            }
            return true;
        }
        else return false;
    }

    @Override
    public boolean canFillWithFluid(BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
        if (!isFullBlock(state, (SlabBlockEntity) world.getBlockEntity(pos)))
            return fluid == Fluids.WATER;
        else
            return false;
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) { return false; }
}
