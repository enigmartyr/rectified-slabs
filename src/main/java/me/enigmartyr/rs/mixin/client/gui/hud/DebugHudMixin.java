package me.enigmartyr.rs.mixin.client.gui.hud;

import com.llamalad7.mixinextras.sugar.Local;
import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    /**
     * @author enigmartyr
     * @reason Alters the presentation of mate slabs on the debug screen.
     */
    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "java/util/List.add (Ljava/lang/Object;)Z"))
    private boolean rectifiedslabs_getRightText(List<String> list, Object string, @Local BlockPos blockPos, @Local BlockState blockState) {
        if (!(blockState.getBlock() instanceof SlabBlock)) return list.add((String) string);

        SlabBlockEntity blockEntity = ((SlabBlockEntity) MinecraftClient.getInstance().world.getBlockEntity(blockPos));
        if (blockEntity.isLookingAtMate(MinecraftClient.getInstance().player, blockState)) blockState = blockEntity.getMateBlockState(blockState);

        if (((String) string).startsWith(Formatting.UNDERLINE + "Targeted Block: ") && blockState.get(Properties.SLAB_TYPE) == SlabType.TOP) {
            Vec3d slabPos = switch (blockState.get(Properties.AXIS)) {
                case X -> Vec3d.of(blockPos).add(0.5, 0.0, 0.0);
                case Y -> Vec3d.of(blockPos).add(0.0, 0.5, 0.0);
                case Z -> Vec3d.of(blockPos).add(0.0, 0.0, 0.5);
            };

            return list.add((Formatting.UNDERLINE + "Targeted Block: " + slabPos.getX() + ", " + slabPos.getY() + ", " + slabPos.getZ()).replace(".0", ""));
        }

        try {
            if (Registries.BLOCK.get(new Identifier(((String) string))) instanceof SlabBlock)
                return list.add(String.valueOf(Registries.BLOCK.getId(blockState.getBlock())));
        } catch (Exception e) { }

        return list.add((String) string);
    }

    /**
     * @author enigmartyr
     * @reason Required so that the slab type property is correctly reported.
     */
    @ModifyVariable(method = "getRightText", at = @At("STORE"), index = 11)
    private BlockState rectifiedslabs_getRightText_blockState(BlockState blockState, @Local BlockPos blockPos) {
        if (!(blockState.getBlock() instanceof SlabBlock)) return blockState;
        SlabBlockEntity blockEntity = ((SlabBlockEntity) MinecraftClient.getInstance().world.getBlockEntity(blockPos));
        return blockEntity.isLookingAtMate(MinecraftClient.getInstance().player, blockState) ? blockEntity.getMateBlockState(blockState) : blockState;
    }
}
