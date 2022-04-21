package me.jkmcameron.rs.mixin.client.network;

import me.jkmcameron.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin
{
    @Shadow private boolean breakingBlock;
    @Shadow private GameMode gameMode;
    @Shadow private BlockPos currentBreakingPos;

    @Shadow @Final private MinecraftClient client;
    @Shadow private float currentBreakingProgress;
    @Shadow private float blockBreakingSoundCooldown;

    @Shadow private boolean isCurrentlyBreaking(BlockPos pos) { return false; }
    @Shadow private void sendPlayerAction(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction) { }

    private boolean breakingSlabBlock, breakingSlabMate;

    @Inject(method = "updateBlockBreakingProgress", at = @At("RETURN"), cancellable = true)
    public void updateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir)
    {
        System.out.println(breakingSlabMate);

        if (!this.client.player.isBlockBreakingRestricted(this.client.world, pos, this.gameMode) && this.client.world.getWorldBorder().contains(pos) && !this.gameMode.isCreative())
        {
            BlockState blockState = this.client.world.getBlockState(pos);

            if(this.isCurrentlyBreaking(pos) && blockState.getBlock() instanceof SlabBlock)
            {
                if(breakingSlabBlock)
                {
                    SlabBlockEntity blockEntity = ((SlabBlockEntity) this.client.world.getBlockEntity(pos));

                    boolean wasBreakingSlabMate = breakingSlabMate;
                    if((breakingSlabMate = blockEntity.isLookingAtMate(client.player)) != wasBreakingSlabMate)
                    {
                        this.sendPlayerAction(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, this.currentBreakingPos, direction);
                        this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 0.0f);
                        this.sendPlayerAction(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction);
                        blockState.onBlockBreakStart(this.client.world, pos, this.client.player);
                        this.currentBreakingProgress = 0.0f;
                        this.blockBreakingSoundCooldown = 0.0f;
                        this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, (int)(this.currentBreakingProgress * 10.0f) - 1);
                    }
                }

                breakingSlabBlock = true;

                cir.setReturnValue(true);
                cir.cancel();
            }
            else breakingSlabBlock = breakingSlabMate = false;
        }
    }
}
