package me.enigmartyr.rs.mixin.block;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Degradable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(Degradable.class)
public interface DegradableMixin<T extends Enum<T>> {
    @Shadow
    public Optional<BlockState> getDegradationResult(BlockState var1);
    @Shadow
    public T getDegradationLevel();
    @Shadow
    public float getDegradationChanceMultiplier();

    /**
     * @author enigmartyr
     * @reason Ensures that mate slabs are also considered when polling surrounding blocks during
     *         the degradation calculation. Passes the mate BlockState if the mate is an instance
     *         of Degradable (such as copper slabs), and falls back on the base block if not.
     */
    @Redirect(method = "tryDegrade", at = @At(value = "INVOKE", target = "net/minecraft/server/world/ServerWorld.getBlockState (Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    default BlockState rectifiedslabs_tryDegrade_getBlockState(ServerWorld world, BlockPos blockPos) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);
        if(blockEntity instanceof SlabBlockEntity && ((SlabBlockEntity) blockEntity).hasMate()) {
            BlockState blockState = ((SlabBlockEntity) blockEntity).getMateBlockState(world.getBlockState(blockPos));
            if(blockState.getBlock() instanceof Degradable) return blockState;
        }

        return world.getBlockState(blockPos);
    }
}
