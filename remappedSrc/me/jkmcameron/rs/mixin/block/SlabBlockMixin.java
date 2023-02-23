package me.jkmcameron.rs.mixin.block;

import me.jkmcameron.rs.RectifiedSlabs;
import me.jkmcameron.rs.block.ISlabBlock;
import me.jkmcameron.rs.block.entity.SlabBlockEntity;
import me.jkmcameron.rs.sound.SlabBlockSoundGroup;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlabBlock.class)
public class SlabBlockMixin extends Block implements Waterloggable, BlockEntityProvider, ISlabBlock
{
    private static final EnumProperty<Direction.Axis> AXIS        = Properties.AXIS;
    @Shadow @Final
    public  static final EnumProperty<SlabType>       TYPE        = Properties.SLAB_TYPE;
    @Shadow @Final
    public  static final BooleanProperty              WATERLOGGED = Properties.WATERLOGGED;

    @Shadow @Final
    protected static final VoxelShape TOP_SHAPE = Block.createCuboidShape(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);
    @Shadow @Final
    protected static final VoxelShape BOTTOM_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape LEFT_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
    private static final VoxelShape RIGHT_SHAPE = Block.createCuboidShape(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape FRONT_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    private static final VoxelShape BACK_SHAPE = Block.createCuboidShape(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);

    private SlabBlockSoundGroup soundGroup;

	public SlabBlockMixin(Settings settings)
	{
        super(settings);
	}

    @Inject(method = "<init>(Lnet/minecraft/block/AbstractBlock$Settings;)V", at = @At("TAIL"))
    public void init(AbstractBlock.Settings settings, CallbackInfo info)
    {
        this.setDefaultState(this.getDefaultState().with(TYPE, SlabType.BOTTOM).with(AXIS, Direction.Axis.Y).with(WATERLOGGED, false));
        this.soundGroup = new SlabBlockSoundGroup(super.soundGroup);
    }

    @Inject(method = "appendProperties(Lnet/minecraft/state/StateManager$Builder;)V", at = @At("TAIL"))
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo info)
    {
        builder.add(AXIS);
    }

    private boolean isFullBlock(SlabType slabType, SlabBlockEntity blockEntity) { return slabType == SlabType.DOUBLE || (blockEntity != null && blockEntity.hasMate()); }

    private VoxelShape getSlabShape(BlockState state, boolean getOpposite)
    {
        return switch (state.get(AXIS))
        {
            case X -> state.get(TYPE) == (getOpposite ? SlabType.TOP : SlabType.BOTTOM) ? LEFT_SHAPE: RIGHT_SHAPE;
            case Y -> state.get(TYPE) == (getOpposite ? SlabType.TOP : SlabType.BOTTOM) ? BOTTOM_SHAPE: TOP_SHAPE;
            case Z -> state.get(TYPE) == (getOpposite ? SlabType.TOP : SlabType.BOTTOM) ? FRONT_SHAPE: BACK_SHAPE;
        };
    }

	/**
     * @author jkmcameron
     */
    @Overwrite
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        return (state.get(TYPE) == SlabType.DOUBLE)
                ? VoxelShapes.fullCube()
                : getSlabShape(state, blockEntity != null && ((SlabBlockEntity) blockEntity).isLookingAtMate(MinecraftClient.getInstance().player));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if(blockEntity != null && ((SlabBlockEntity) blockEntity).hasMate()) return VoxelShapes.fullCube();

        return this.getOutlineShape(state, world, pos, context);
    }

    /**
     * @author jkmcameron
     */
    @Overwrite
    public @Nullable BlockState getPlacementState(ItemPlacementContext context)
    {
        BlockPos blockPos = context.getBlockPos();
        SlabBlockEntity blockEntity = ((SlabBlockEntity) context.getWorld().getBlockEntity(blockPos));

        BlockState currentState = context.getWorld().getBlockState(blockPos);
        FluidState fluidState = context.getWorld().getFluidState(blockPos);
        Direction blockFace = context.getSide();
        Vec3d position = context.getHitPos();
        Direction.Axis faceAxis = blockFace.getAxis();

        double deltaX = position.x - context.getBlockPos().getX();
        double deltaY = position.y - context.getBlockPos().getY();
        double deltaZ = position.z - context.getBlockPos().getZ();

        double relX = ((faceAxis == Direction.Axis.X ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.POSITIVE ? 1 - deltaZ : deltaZ))
                    + ((faceAxis == Direction.Axis.Y ? 1 : 0) * deltaX)
                    + ((faceAxis == Direction.Axis.Z ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.NEGATIVE ? 1 - deltaX : deltaX));
        double relY = ((faceAxis == Direction.Axis.X ? 1 : 0) * deltaY)
                    + ((faceAxis == Direction.Axis.Y ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.POSITIVE ? 1 - deltaZ : deltaZ))
                    + ((faceAxis == Direction.Axis.Z ? 1 : 0) * deltaY);

        double normX = Math.abs(relX - 0.5);
        double normY = Math.abs(relY - 0.5);

        Direction.Axis slabAxis;
        Direction.AxisDirection slabDirection;

        if(normX < 0.25 && normY < 0.25 )
        {
            slabAxis = faceAxis;
            slabDirection = blockFace.getDirection().getOpposite();

            if(currentState.getBlock() instanceof SlabBlock && slabAxis == currentState.get(AXIS) && slabDirection == ISlabBlock.SLAB_DIRECTIONS.get(currentState.get(TYPE)))
            {
                if(currentState.isOf(this)) return currentState.with(TYPE, SlabType.DOUBLE).with(WATERLOGGED, false);

                blockEntity.setMate((SlabBlock) (Object) this);

                context.getWorld().playSound(context.getPlayer(), blockPos, this.soundGroup.getPlaceSound(), SoundCategory.BLOCKS, (this.soundGroup.getVolume() + 1.0f) / 2.0f, this.soundGroup.getPitch() * 0.8f);
                context.getPlayer().swingHand(context.getHand());
                context.getPlayer().getEquippedStack(context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND).increment(-1);

                return currentState.with(WATERLOGGED, false);
            }
        }
        else if(normX >= normY)
        {
            if(faceAxis == Direction.Axis.X)
            {
                slabAxis = Direction.Axis.Z;
                slabDirection = deltaZ < 0.5 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
            }
            else
            {
                slabAxis = Direction.Axis.X;
                slabDirection = deltaX < 0.5 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
            }
        }
        else
        {
            if(faceAxis == Direction.Axis.Y)
            {
                slabAxis = Direction.Axis.Z;
                slabDirection = deltaZ < 0.5 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
            }
            else
            {
                slabAxis = Direction.Axis.Y;
                slabDirection = deltaY < 0.5 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
            }
        }

        return this.getDefaultState().with(AXIS, slabAxis).with(TYPE, ISlabBlock.SLAB_DIRECTIONS.inverse().get(slabDirection)).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    /**
     * @author jkmcameron
     */
    @Overwrite
    public boolean canReplace(BlockState state, ItemPlacementContext context)
    {
        ItemStack itemStack = context.getStack();
        SlabType slabType = state.get(TYPE);
        Direction.Axis axis = state.get(AXIS);
        Direction blockFace = context.getSide();
        Vec3d hitPosition = context.getHitPos();
        BlockPos blockPosition = context.getBlockPos();

        if (slabType == SlabType.DOUBLE || !(itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof SlabBlock) || blockFace.getDirection() == ISlabBlock.SLAB_DIRECTIONS.get(slabType))  return false;

        double deltaX = hitPosition.x - blockPosition.getX();
        double deltaY = hitPosition.y - blockPosition.getY();
        double deltaZ = hitPosition.z - blockPosition.getZ();

        double relX = ((axis == Direction.Axis.X ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.POSITIVE ? 1 - deltaZ : deltaZ))
                    + ((axis == Direction.Axis.Y ? 1 : 0) * deltaX)
                    + ((axis == Direction.Axis.Z ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.NEGATIVE ? 1 - deltaX : deltaX));
        double relY = ((axis == Direction.Axis.X ? 1 : 0) * deltaY)
                    + ((axis == Direction.Axis.Y ? 1 : 0) * (blockFace.getDirection() == Direction.AxisDirection.POSITIVE ? 1 - deltaZ : deltaZ))
                    + ((axis == Direction.Axis.Z ? 1 : 0) * deltaY);

        double normX = Math.abs(relX - 0.5);
        double normY = Math.abs(relY - 0.5);

        return (normX < 0.25 && normY < 0.25) && !isFullBlock(context.getWorld().getBlockState(blockPosition).get(TYPE), (SlabBlockEntity) context.getWorld().getBlockEntity(blockPosition));
    }

    @Override
    protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state)
    {
        SlabBlockEntity blockEntity = (SlabBlockEntity) world.getBlockEntity(pos);

        super.spawnBreakParticles(world, player, pos, blockEntity.hasMate() && blockEntity.isLookingAtMate(player) ? blockEntity.getMateBlockState() : state);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity)
    {
        super.onSteppedOn(world, pos, state, entity);

        this.soundGroup.setStepSound((SlabBlockEntity) world.getBlockEntity(pos));
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player)
    {
        super.onBlockBreakStart(state, world, pos, player);

        this.soundGroup.setHitSound((SlabBlockEntity) world.getBlockEntity(pos), player);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance)
    {
        super.onLandedUpon(world, state, pos, entity, fallDistance);

        this.soundGroup.setFallSound((SlabBlockEntity) world.getBlockEntity(pos));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player)
    {
        SlabBlockEntity blockEntity = (SlabBlockEntity) world.getBlockEntity(pos);
        BlockSoundGroup soundGroup = (blockEntity.isLookingAtMate() ? blockEntity.getMateBlockState() : state).getSoundGroup();

        world.playSound(player, pos, soundGroup.getBreakSound(), SoundCategory.BLOCKS, (soundGroup.getVolume() + 1.0f) / 2.0f, soundGroup.getPitch() * 0.8f);
        super.onBreak(world, pos, blockEntity.isLookingAtMate(player) ? blockEntity.getMateBlockState() : state, player);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack)
    {
        if(!((SlabBlockEntity) blockEntity).hasMate())
        {
            super.afterBreak(world, player, pos, state, blockEntity, stack);
            return;
        }

        boolean lookingAtMate = ((SlabBlockEntity) blockEntity).isLookingAtMate();
        BlockState minedSlab = lookingAtMate ? ((SlabBlockEntity) blockEntity).getMateBlockState() : state;
        BlockState keptSlab  = lookingAtMate ? state : ((SlabBlockEntity) blockEntity).getMateBlockState();

        player.incrementStat(Stats.MINED.getOrCreateStat(minedSlab.getBlock()));
        Block.dropStacks(minedSlab, world, pos, blockEntity, player, new ItemStack(minedSlab.getBlock().asItem()));
        player.addExhaustion(0.005f);
        world.setBlockState(pos, keptSlab, Block.FORCE_STATE);
        ((SlabBlockEntity) blockEntity).setMate(null);
    }

    @Override
    public BlockSoundGroup getSoundGroup(BlockState state)
    {
        return this.soundGroup;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        SlabBlockEntity blockEntity = new SlabBlockEntity(pos, state);

        return blockEntity;
    }
}
