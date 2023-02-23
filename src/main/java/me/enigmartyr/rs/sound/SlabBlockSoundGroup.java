package me.enigmartyr.rs.sound;

import me.enigmartyr.rs.block.entity.SlabBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class SlabBlockSoundGroup extends BlockSoundGroup
{
    private SoundEvent primedStepSound, primedFallSound;

    public SlabBlockSoundGroup(BlockSoundGroup baseSoundGroup)
    {
        super(baseSoundGroup.volume, baseSoundGroup.pitch, baseSoundGroup.getBreakSound(), baseSoundGroup.getStepSound(), baseSoundGroup.getPlaceSound(), baseSoundGroup.getHitSound(), baseSoundGroup.getFallSound());
        this.primedStepSound = baseSoundGroup.getStepSound();
        this.primedFallSound = baseSoundGroup.getFallSound();
    }

    @Override
    public SoundEvent getFallSound() {  return this.primedFallSound; }
    @Override
    public SoundEvent getStepSound() { return this.primedStepSound; }

    public void primeStepSound(BlockState mateState) {
        BlockSoundGroup mateSoundGroup = mateState.getSoundGroup();

        if(mateState.get(Properties.AXIS) == Direction.Axis.Y)
            this.primedStepSound = mateState.get(Properties.SLAB_TYPE) == SlabType.BOTTOM
                                 ? super.getStepSound()
                                 : mateSoundGroup.getStepSound();
        else
            this.primedStepSound = new Random().nextBoolean()
                                 ? super.getStepSound()
                                 : mateSoundGroup.getStepSound();
    }

    public void primeFallSound(BlockState mateState) {
        BlockSoundGroup mateSoundGroup = mateState.getSoundGroup();

        if(mateState.get(Properties.AXIS) == Direction.Axis.Y)
            this.primedStepSound = mateState.get(Properties.SLAB_TYPE) == SlabType.BOTTOM
                                 ? super.getFallSound()
                                 : mateSoundGroup.getFallSound();
        else
            this.primedStepSound = new Random().nextBoolean()
                                 ? super.getFallSound()
                                 : mateSoundGroup.getFallSound();
    }
}
