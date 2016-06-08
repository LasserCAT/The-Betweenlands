package thebetweenlands.common.block.terrain;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thebetweenlands.client.particle.BLParticle;
import thebetweenlands.common.registries.BlockRegistry;
import thebetweenlands.common.registries.FluidRegistry;
import thebetweenlands.common.world.WorldProviderBetweenlands;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class BlockSwampWater extends BlockFluidClassic/* implements IBlockColor*/{
    private static final int DEEP_COLOR_R = 19;
    private static final int DEEP_COLOR_G = 24;
    private static final int DEEP_COLOR_B = 68;


    protected boolean canSpread = true;
    protected boolean hasBoundingBox = false;
    protected boolean canCollide = false;
    protected boolean canReplenish = true;

   // private static final HashMap<Block, IWaterRenderer> SPECIAL_RENDERERS = new HashMap<Block, IWaterRenderer>();

    public BlockSwampWater(Fluid fluid, Material material) {
        super(fluid, material);
        //setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        //setBlockName("thebetweenlands.swampWater");
        this.setMaxScaledLight(0);
    }

   /* public BlockSwampWater() {
        this(FluidRegistry.swampWater, Material.WATER);
    }


    @Override
    public boolean canDisplace(IBlockAccess world, BlockPos pos) {
        if (world.isAirBlock(pos) || !this.canSpread) return true;

        Block block = world.getBlockState(pos).getBlock();

        if (this.canConnectTo(block))
        {
            return false;
        }

        if (displacements.containsKey(block))
        {
            return displacements.get(block);
        }

        Material material = block.getMaterial(world.getBlockState(pos));
        if (material.blocksMovement() || material == Material.PORTAL)
        {
            return false;
        }

        int density = getDensity(world,pos);
        if (density == Integer.MAX_VALUE)
        {
            return true;
        }

        if (this.density > density)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int colorMultiplier(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int colorMultiplier = world.getBiomeGenForCoords(pos.add(dx, 0, dz)).getWaterColorMultiplier();
                r += (colorMultiplier & 0xFF0000) >> 16;
                g += (colorMultiplier & 0x00FF00) >> 8;
                b += colorMultiplier & 0x0000FF;
            }
        }
        r /= 9;
        g /= 9;
        b /= 9;
        float depth = 0;
        if (pos.getY() > WorldProviderBetweenlands.CAVE_START) {
            depth = 1;
        } else {
            if (pos.getY() < WorldProviderBetweenlands.CAVE_WATER_HEIGHT) {
                depth = 0;
            } else {
                depth = (pos.getY() - WorldProviderBetweenlands.CAVE_WATER_HEIGHT) / (float) (WorldProviderBetweenlands.CAVE_START - WorldProviderBetweenlands.CAVE_WATER_HEIGHT);
            }
        }
        r = (int) (r * depth + DEEP_COLOR_R * (1 - depth) + 0.5F);
        g = (int) (g * depth + DEEP_COLOR_G * (1 - depth) + 0.5F);
        b = (int) (b * depth + DEEP_COLOR_B * (1 - depth) + 0.5F);
        return r << 16 | g << 8 | b;
    }

    @Override
    public boolean isFlowingVertically(IBlockAccess world, int x, int y, int z) {
        return this.canConnectTo(world.getBlock(x, y + densityDir, z)) ||
                (this.canConnectTo(world.getBlock(x, y, z)) && canFlowInto(world, x, y + densityDir, z));
    }

    @Override
    public boolean isSourceBlock(IBlockAccess world, BlockPos pos) {
        return this.canConnectTo(world.getBlock(x, y, z)) && world.getBlockMetadata(x, y, z) == 0;
    }

    @Override
    protected boolean canFlowInto(IBlockAccess world, int x, int y, int z) {
        if (world.getBlock(x, y, z).isAir(world, x, y, z)) return true;

        Block block = world.getBlock(x, y, z);
        if (this.canConnectTo(block)) {
            return true;
        }

        if (displacements.containsKey(block))
        {
            return displacements.get(block);
        }

        Material material = block.getMaterial();
        if (material.blocksMovement()  ||
                material == Material.water ||
                material == Material.lava  ||
                material == Material.portal)
        {
            return false;
        }

        int density = getDensity(world, x, y, z);
        if (density == Integer.MAX_VALUE)
        {
            return true;
        }

        if (this.density > density)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected void flowIntoBlock(World world, int x, int y, int z, int meta) {
        if(!this.canSpread) {
            return;
        }
        if (meta < 0) return;
        if (displaceIfPossible(world, x, y, z) && this.canSpread) {
            world.setBlock(x, y, z, this, meta, 3);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if(!this.canSpread) {
            return;
        }

        if(this.canReplenish && !world.isAirBlock(x, y-1, z)) {
            int adjacentSources = 0;
            if(this.isSourceBlock(world, x+1, y, z)) adjacentSources++;
            if(this.isSourceBlock(world, x-1, y, z)) adjacentSources++;
            if(this.isSourceBlock(world, x, y, z+1)) adjacentSources++;
            if(this.isSourceBlock(world, x, y, z-1)) adjacentSources++;
            if(adjacentSources >= 2) {
                world.setBlockMetadataWithNotify(x, y, z, 0, 2);
            }
        }

        int quantaRemaining = quantaPerBlock - world.getBlockMetadata(x, y, z);
        int expQuanta = -101;

        // check adjacent block levels if non-source
        if (quantaRemaining < quantaPerBlock)
        {
            int y2 = y - densityDir;

            if (this.canConnectTo(world.getBlock(x,     y2, z    )) ||
                    this.canConnectTo(world.getBlock(x - 1, y2, z    )) ||
                    this.canConnectTo(world.getBlock(x + 1, y2, z    )) ||
                    this.canConnectTo(world.getBlock(x,     y2, z - 1)) ||
                    this.canConnectTo(world.getBlock(x,     y2, z + 1)))
            {
                expQuanta = quantaPerBlock - 1;
            }
            else
            {
                int maxQuanta = -100;
                maxQuanta = getLargerQuanta(world, x - 1, y, z,     maxQuanta);
                maxQuanta = getLargerQuanta(world, x + 1, y, z,     maxQuanta);
                maxQuanta = getLargerQuanta(world, x,     y, z - 1, maxQuanta);
                maxQuanta = getLargerQuanta(world, x,     y, z + 1, maxQuanta);

                expQuanta = maxQuanta - 1;
            }

            // decay calculation
            if (expQuanta != quantaRemaining)
            {
                quantaRemaining = expQuanta;

                if (expQuanta <= 0)
                {
                    world.setBlock(x, y, z, Blocks.air);
                }
                else
                {
                    world.setBlockMetadataWithNotify(x, y, z, quantaPerBlock - expQuanta, 3);
                    world.scheduleBlockUpdate(x, y, z, this, tickRate);
                    world.notifyBlocksOfNeighborChange(x, y, z, this);
                }
            }
        }
        // This is a "source" block, set meta to zero, and send a server only update
        else if (quantaRemaining >= quantaPerBlock)
        {
            world.setBlockMetadataWithNotify(x, y, z, 0, 2);
        }

        // Flow vertically if possible
        if (canDisplace(world, x, y + densityDir, z))
        {
            flowIntoBlock(world, x, y + densityDir, z, 1);
            return;
        }

        // Flow outward if possible
        int flowMeta = quantaPerBlock - quantaRemaining + 1;
        if (flowMeta >= quantaPerBlock)
        {
            return;
        }

        if (isSourceBlock(world, x, y, z) || !isFlowingVertically(world, x, y, z))
        {
            if (this.canConnectTo(world.getBlock(x, y - densityDir, z)))
            {
                flowMeta = 1;
            }
            boolean flowTo[] = getOptimalFlowDirections(world, x, y, z);

            if (flowTo[0]) flowIntoBlock(world, x - 1, y, z,     flowMeta);
            if (flowTo[1]) flowIntoBlock(world, x + 1, y, z,     flowMeta);
            if (flowTo[2]) flowIntoBlock(world, x,     y, z - 1, flowMeta);
            if (flowTo[3]) flowIntoBlock(world, x,     y, z + 1, flowMeta);
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        Block block = world.getBlock(x, y, z);
        if(side == 0 && block != BLBlockRegistry.swampWater) {
            return true;
        }
        if (!this.canConnectTo(block))
        {
            if(side == 1) {
                return true;
            } else {
                return !block.isOpaqueCube() && !block.isSideSolid(world, x, y, z, ForgeDirection.getOrientation(side));
            }
        }
        return (block.getMaterial() == this.getMaterial() || this.canConnectTo(block)) ? false : super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public boolean displaceIfPossible(World world, int x, int y, int z)
    {
        if(!this.canSpread) {
            return false;
        }
        if (world.getBlock(x, y, z).isAir(world, x, y, z))
        {
            return true;
        }

        Block block = world.getBlock(x, y, z);
        if (this.canConnectTo(block))
        {
            return false;
        }

        if (displacements.containsKey(block))
        {
            if (displacements.get(block))
            {
                block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
                return true;
            }
            return false;
        }

        Material material = block.getMaterial();
        if (material.blocksMovement() || material == Material.portal)
        {
            return false;
        }

        int density = getDensity(world, x, y, z);
        if (density == Integer.MAX_VALUE)
        {
            block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            return true;
        }

        if (this.density > density)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public Vec3d getFlowVector(IBlockAccess world, BlockPos pos)
    {
        if(!this.canSpread) {
            return new Vec3d(0.0D, 0.0D, 0.0D);
        }
        Vec3d vec = new Vec3d(0.0D, 0.0D, 0.0D);
        int decay = quantaPerBlock - getQuantaValue(world, x, y, z);

        for (int side = 0; side < 4; ++side)
        {
            int x2 = x;
            int z2 = z;

            switch (side)
            {
                case 0: --x2; break;
                case 1: --z2; break;
                case 2: ++x2; break;
                case 3: ++z2; break;
            }

            Block otherBlock = world.getBlock(x2, y, z2);
            if(otherBlock instanceof BlockSwampWater && ((BlockSwampWater)otherBlock).canSpread) {
                int otherDecay = quantaPerBlock - getQuantaValue(world, x2, y, z2);
                if (otherDecay >= quantaPerBlock)
                {
                    if (!world.getBlock(x2, y, z2).getMaterial().blocksMovement() && !this.canConnectTo(world.getBlock(x2, y, z2)))
                    {
                        otherDecay = quantaPerBlock - getQuantaValue(world, x2, y - 1, z2);
                        if (otherDecay >= 0)
                        {
                            int power = otherDecay - (decay - quantaPerBlock);
                            vec = vec.addVector((x2 - x) * power, (y - y) * power, (z2 - z) * power);
                        }
                    }
                }
                else if (otherDecay >= 0)
                {
                    int power = otherDecay - decay;
                    vec = vec.addVector((x2 - x) * power, (y - y) * power, (z2 - z) * power);
                }
            }
        }

        if (this.canConnectTo(world.getBlock(x, y + 1, z)))
        {
            boolean flag =
                    isBlockSolid(world, x,     y,     z - 1, 2) ||
                            isBlockSolid(world, x,     y,     z + 1, 3) ||
                            isBlockSolid(world, x - 1, y,     z,     4) ||
                            isBlockSolid(world, x + 1, y,     z,     5) ||
                            isBlockSolid(world, x,     y + 1, z - 1, 2) ||
                            isBlockSolid(world, x,     y + 1, z + 1, 3) ||
                            isBlockSolid(world, x - 1, y + 1, z,     4) ||
                            isBlockSolid(world, x + 1, y + 1, z,     5);

            if (flag)
            {
                vec = vec.normalize().addVector(0.0D, -6.0D, 0.0D);
            }
        }
        vec = vec.normalize();
        return vec;
    }

    @Override
    public int getQuantaValue(IBlockAccess world, int x, int y, int z) {
        if(this.canSpread) {
            return super.getQuantaValue(world, x, y, z);
        } else {
            return this.quantaPerBlock;
        }
    }

    @Override
    public boolean canCollideCheck(int meta, boolean fullHit) {
        return this.hasBoundingBox || fullHit && (!this.canSpread || meta == 0);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB bb, List bbList, Entity entity) {
        if(this.canCollide) {
            AxisAlignedBB blockBB = this.getCollisionBoundingBoxFromPool(world, x, y, z);
            if (blockBB != null && bb.intersectsWith(blockBB)) {
                bbList.add(blockBB);
            }
        } else {
            if(entity instanceof EntityPlayer && ItemImprovedRubberBoots.checkPlayerEffect((EntityPlayer)entity)) {
                AxisAlignedBB blockBB = AxisAlignedBB.getBoundingBox(x, y, z, x+1, y+((float)this.getQuantaValue(world, x, y, z)/(float)this.quantaPerBlock)*0.8F, z+1);
                if (blockBB != null && bb.intersectsWith(blockBB)) {
                    bbList.add(blockBB);
                }
            }
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        if(this.canCollide) {
            return AxisAlignedBB.getBoundingBox((double) x + this.minX, (double) y + this.minY, (double) z + this.minZ, (double) x + this.maxX, (double) y + this.maxY, (double) z + this.maxZ);
        }
        return null;
    }

    @Override
    public boolean isCollidable() {
        return this.canCollide;
    }

    public boolean canConnectTo(Block block) {
        return block == this || block == BlockRegistry.SWAMP_WATER || block instanceof BlockSwampWater || SPECIAL_RENDERERS.containsKey(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if(rand.nextInt(2500) == 0) {
            if(world.getBlockState(pos.up(2)).getBlock() == BlockRegistry.SWAMP_WATER) {
                BLParticle.FISH.spawn(world, x, y, z);
            } else if(world.getBlock(x, y - 1, z) == BlockRegistry.MUD) {
                if(rand.nextInt(2) == 0) {
                    BLParticle.MOSQUITO.spawn(world, x, y + 1.5, z);
                } else {
                    BLParticle.FLY.spawn(world, x, y + 1.5, z);
                }
            }
        }
    }*/
}