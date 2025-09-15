package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.entity.EntityTerrapin;
import com.github.alexthe666.alexsmobs.entity.util.TerrapinTypes;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class TerrapinEggBlockEntity extends BlockEntity {
    public ParentData parent1;
    public ParentData parent2;

    public TerrapinEggBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.TERRAPIN_EGG.get(), pos, state);
    }

    public void addAttributesToOffspring(EntityTerrapin baby, Random random){
        if(parent1 != null && parent2 != null){
            baby.setTurtleType(random.nextBoolean() ? parent1.type : parent2.type);
            baby.setShellType(random.nextBoolean() ? parent1.shellType : parent2.shellType);
            baby.setSkinType(random.nextBoolean() ? parent1.skinType : parent2.skinType);
            baby.setTurtleColor((parent1.turtleColor + parent2.turtleColor) / 2);
            baby.setShellColor((parent1.shellColor + parent2.shellColor) / 2);
            baby.setSkinColor((parent1.skinColor + parent2.skinColor) / 2);
            if(random.nextFloat() < 0.15F){
                baby.setTurtleType(TerrapinTypes.OVERLAY);
                switch (random.nextInt(2)){
                    case 0:
                        baby.setTurtleColor((int) (0xFFFFFF * random.nextFloat()));
                        break;
                    case 1:
                        baby.setShellColor((int) (0xFFFFFF * random.nextFloat()));
                        break;
                    case 2:
                        baby.setSkinColor((int) (0xFFFFFF * random.nextFloat()));
                        break;
                }
            }
        }
    }

    @Override
    public void readNbt(NbtCompound compound) {
        super.readNbt(compound);
        if(compound.contains("Parent1Data")){
            this.parent1 = new ParentData(compound.getCompound("Parent1Data"));
        }
        if(compound.contains("Parent2Data")){
            this.parent2 = new ParentData(compound.getCompound("Parent2Data"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound compound) {
        super.writeNbt(compound);
        if(this.parent1 != null){
            NbtCompound tag = new NbtCompound();
            parent1.writeToNBT(tag);
            compound.put("Parent1Data", tag);
        }
        if(this.parent2 != null){
            NbtCompound tag = new NbtCompound();
            parent2.writeToNBT(tag);
            compound.put("Parent2Data", tag);
        }
    }

    public static class ParentData {
        public TerrapinTypes type;
        public int shellType;
        public int skinType;
        public int turtleColor;
        public int shellColor;
        public int skinColor;

        public ParentData(TerrapinTypes type, int shellType, int skinType, int turtleColor, int shellColor, int skinColor) {
            this.type = type;
            this.shellType = shellType;
            this.skinType = skinType;
            this.turtleColor = turtleColor;
            this.shellColor = shellColor;
            this.skinColor = skinColor;
        }

        public ParentData(NbtCompound tag){
            this(TerrapinTypes.values()[MathHelper.clamp(tag.getInt("TerrapinType"), 0, TerrapinTypes.values().length - 1)],
                    tag.getInt("ShellType"),
                    tag.getInt("SkinType"),
                    tag.getInt("TurtleColor"),
                    tag.getInt("ShellColor"),
                    tag.getInt("SkinColor")
                    );
        }

        public boolean canMerge(ParentData other){
            if(type == TerrapinTypes.OVERLAY && other.type == TerrapinTypes.OVERLAY){
                return turtleColor == other.turtleColor && shellType == other.shellType && skinType == other.skinType && shellColor == other.shellColor && skinColor == other.skinColor;
            }
            return other.type == this.type;
        }

        public void writeToNBT(NbtCompound tag){
            tag.putInt("TerrapinType", type.ordinal());
            tag.putInt("ShellType", shellType);
            tag.putInt("SkinType", skinType);
            tag.putInt("TurtleColor", turtleColor);
            tag.putInt("ShellColor", shellColor);
            tag.putInt("SkinColor", skinColor);

        }
    }

}
