package me.enigmartyr.rs.mixin.client.network;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin
{
    @Shadow @Final private MinecraftClient client;
    @Shadow private float currentBreakingProgress;
    @Shadow private float blockBreakingSoundCooldown;

    @Shadow private boolean isCurrentlyBreaking(BlockPos pos) { return false; }
    @Shadow private void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator) { }
    /**
     * @author enigmartyr
     * @reason Stops destroying block from client side if player's crosshair moves between halves of a mixed slab.
     */
    @Inject(method = "updateBlockBreakingProgress", at = @At("RETURN"))
    public void rectifiedslabs_updateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ClientWorld world      = this.client.world;
        BlockState  blockState = world.getBlockState(pos);
        if(this.isCurrentlyBreaking(pos) && blockState.getBlock() instanceof SlabBlock) {
            SlabBlockEntity blockEntity = (SlabBlockEntity) world.getBlockEntity(pos);
            if(blockEntity.isLookingAtMate(this.client.player, blockState)) {
                this.currentBreakingProgress = this.blockBreakingSoundCooldown = 0.0f;
                this.sendSequencedPacket(world, sequence -> { return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, direction, sequence); });
                this.sendSequencedPacket(world, sequence -> { return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence); });
            }
        }
    }

    /**
     * @author enigmartyr
     * @reason Makes it so that one can form a mixed slab block from a single slab block by attempting to place
     *         a slab block on the face of the adjacent block (vanilla Minecraft will not allow ths as it knows
     *         the result block placed would need to occupy a position that already has a block).
     */
    @ModifyVariable(method = "interactBlock", at = @At("HEAD"), argsOnly = true)
    public BlockHitResult rectifiedslabs_interactBlock(BlockHitResult hitResult) {
        BlockState touchedBlockState = client.player.world.getBlockState(hitResult.getBlockPos());
        BlockState  futureBlockState = client.player.world.getBlockState(hitResult.getBlockPos().offset(hitResult.getSide()));
        if(futureBlockState.getBlock() instanceof SlabBlock) {
            ItemPlacementContext context = new ItemPlacementContext(client.player, client.player.getActiveHand(), client.player.getActiveItem(), hitResult);
            Block touchedBlock = touchedBlockState.getBlock();
            if(!(touchedBlock instanceof SlabBlock && ((SlabBlock) touchedBlockState.getBlock()).canReplace(touchedBlockState, context))) {
                return hitResult.withBlockPos(hitResult.getBlockPos().offset(hitResult.getSide())).withSide(hitResult.getSide().getOpposite());
            }
        }

        return hitResult;
    }
}
