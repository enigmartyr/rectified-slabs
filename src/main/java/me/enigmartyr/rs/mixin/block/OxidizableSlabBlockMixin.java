package me.enigmartyr.rs.mixin.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Degradable;
import net.minecraft.block.OxidizableSlabBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OxidizableSlabBlock.class)
public abstract class OxidizableSlabBlockMixin extends SlabBlock implements Degradable {
    public OxidizableSlabBlockMixin(Settings settings) { super(settings); }

    /**
     * @author enigmartyr
     * @reason Required because the vanilla code never calls super and this would prevent any slab mated together
     *         with an OxidizableSlabBlock to not be ticked.
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        super.randomTick(state, world, pos, random);
    }
}
