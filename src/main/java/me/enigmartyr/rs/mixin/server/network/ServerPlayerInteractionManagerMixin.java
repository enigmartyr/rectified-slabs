package me.enigmartyr.rs.mixin.server.network;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow
    protected ServerWorld world;

    @Shadow @Final
    protected ServerPlayerEntity player;

    /**
     * @author enigmartyr
     * @reason Makes the mate slab the new base slab when the player begins mining it so that the
     *         break animation is rendered on the correct half-slab and tool use and break speed
     *         may be appropriately calculated.
     */
    @Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
    public void rectifiedslabs_processBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
        BlockState blockState = world.getBlockState(pos);
        if(action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK && blockState.getBlock() instanceof SlabBlock) {
            SlabBlockEntity blockEntity = (SlabBlockEntity) world.getBlockEntity(pos);
            if (blockEntity.isLookingAtMate(player, blockState)) world.setBlockState(pos, blockEntity.flip(blockState));
        }
    }
}
