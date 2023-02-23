package me.jkmcameron.rs.sound;

import me.jkmcameron.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class SlabBlockSoundGroup extends BlockSoundGroup
{
    private final BlockSoundGroup baseGroup;
    private SoundEvent _stepSound, _fallSound, _hitSound;

    public SlabBlockSoundGroup(BlockSoundGroup baseSoundGroup)
    {
        super(baseSoundGroup.volume, baseSoundGroup.pitch, baseSoundGroup.getBreakSound(), baseSoundGroup.getStepSound(), baseSoundGroup.getPlaceSound(), baseSoundGroup.getHitSound(), baseSoundGroup.getFallSound());

        this.baseGroup  = baseSoundGroup;
        this._stepSound = baseSoundGroup.getStepSound();
        this._hitSound  = baseSoundGroup.getHitSound();
        this._fallSound = baseSoundGroup.getFallSound();
    }

    @Override
    public SoundEvent getFallSound() {  return this._fallSound; }
    @Override
    public SoundEvent getHitSound() {  return this._hitSound; }
    @Override
    public SoundEvent getStepSound() { return this._stepSound; }

    public void setStepSound(SlabBlockEntity blockEntity)
    {
        if(blockEntity == null || !blockEntity.hasMate())
        {
            this._stepSound = this.baseGroup.getStepSound();
            return;
        }

        BlockState baseState = blockEntity.getBaseBlockState(),
                   mateState = blockEntity.getMateBlockState();

        this._stepSound = (baseState.get(Properties.AXIS) == Direction.Axis.Y)
                ? baseState.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? ((SlabBlockSoundGroup) mateState.getSoundGroup()).baseGroup.getStepSound() : this.baseGroup.getStepSound()
                :               new  Random().nextBoolean()              ? ((SlabBlockSoundGroup) mateState.getSoundGroup()).baseGroup.getStepSound() : this.baseGroup.getStepSound();
    }

    public void setHitSound(SlabBlockEntity blockEntity, PlayerEntity player)
    {
        this._hitSound = (blockEntity == null || !blockEntity.hasMate())
                ? this.baseGroup.getHitSound()
                : (blockEntity.hasMate() && blockEntity.isLookingAtMate(player)) ? ((SlabBlockSoundGroup) blockEntity.getMateBlockState().getSoundGroup()).baseGroup.getHitSound() : this.baseGroup.getHitSound();
    }

    public void setFallSound(SlabBlockEntity blockEntity)
    {
        if(blockEntity == null || !blockEntity.hasMate())
        {
            this._fallSound = this.baseGroup.getFallSound();
            return;
        }

        BlockState baseState = blockEntity.getBaseBlockState(),
                   mateState = blockEntity.getMateBlockState();

        this._fallSound =  (baseState.get(Properties.AXIS) == Direction.Axis.Y)
                ? baseState.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? ((SlabBlockSoundGroup) mateState.getSoundGroup()).baseGroup.getFallSound() : this.baseGroup.getFallSound()
                :               new  Random().nextBoolean()              ? ((SlabBlockSoundGroup) mateState.getSoundGroup()).baseGroup.getFallSound() : this.baseGroup.getFallSound();
    }
}
