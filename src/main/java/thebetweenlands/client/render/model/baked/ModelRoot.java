package thebetweenlands.client.render.model.baked;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import thebetweenlands.common.block.terrain.BlockRoot;
import thebetweenlands.common.lib.ModInfo;
import thebetweenlands.util.QuadBuilder;
import thebetweenlands.util.StalactiteHelper;

public class ModelRoot implements IModel {
	public final ResourceLocation textureTop;
	public final ResourceLocation textureMiddle;
	public final ResourceLocation textureBottom;
	
	public ModelRoot(ResourceLocation textureTop, ResourceLocation textureMiddle, ResourceLocation textureBottom) {
		this.textureTop = textureTop;
		this.textureMiddle = textureMiddle;
		this.textureBottom = textureBottom;
	}
	
	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return Collections.unmodifiableCollection(Arrays.asList(new ResourceLocation[]{this.textureTop, this.textureMiddle, this.textureBottom}));
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, java.util.function.Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return new ModelBakedRoot(format, bakedTextureGetter.apply(this.textureTop), bakedTextureGetter.apply(this.textureMiddle), bakedTextureGetter.apply(this.textureBottom));
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	public static class ModelBakedRoot implements IBakedModel {
		private final VertexFormat format;
		private final TextureAtlasSprite textureTop;
		private final TextureAtlasSprite textureMiddle;
		private final TextureAtlasSprite textureBottom;

		private ModelBakedRoot(VertexFormat format, TextureAtlasSprite textureTop, TextureAtlasSprite textureMiddle, TextureAtlasSprite textureBottom) {
			this.format = format;
			this.textureTop = textureTop;
			this.textureMiddle = textureMiddle;
			this.textureBottom = textureBottom;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState stateOld, EnumFacing side, long rand) {
			IExtendedBlockState state = (IExtendedBlockState) stateOld;

			List<BakedQuad> quads = new ArrayList<>();

			if(side == null) {
				try {
					int distUp = state.getValue(BlockRoot.DIST_UP);
					int distDown = state.getValue(BlockRoot.DIST_DOWN);
					boolean noTop = state.getValue(BlockRoot.NO_TOP);
					boolean noBottom = state.getValue(BlockRoot.NO_BOTTOM);
					int posX = state.getValue(BlockRoot.POS_X);
					int posY = state.getValue(BlockRoot.POS_Y);
					int posZ = state.getValue(BlockRoot.POS_Z);
					float height = 1.0F;

					int totalHeight = 1 + distDown + distUp;
					float distToMidBottom, distToMidTop;

					double squareAmount = 1.2D;
					double halfTotalHeightSQ;

					if(noTop) {
						halfTotalHeightSQ = Math.pow(totalHeight, squareAmount);
						distToMidBottom = Math.abs(distUp + 1);
						distToMidTop = Math.abs(distUp);
					} else if(noBottom) {
						halfTotalHeightSQ = Math.pow(totalHeight, squareAmount);
						distToMidBottom = Math.abs(distDown);
						distToMidTop = Math.abs(distDown + 1);
					} else {
						float halfTotalHeight = totalHeight * 0.5F;
						halfTotalHeightSQ = Math.pow(halfTotalHeight, squareAmount);
						distToMidBottom = Math.abs(halfTotalHeight - distUp - 1);
						distToMidTop = Math.abs(halfTotalHeight - distUp);
					}

					int minValBottom = (noBottom && distDown == 0) ? 0 : 1;
					int minValTop = (noTop && distUp == 0) ? 0 : 1;
					int scaledValBottom = (int) (Math.pow(distToMidBottom, squareAmount) / halfTotalHeightSQ * (8 - minValBottom)) + minValBottom;
					int scaledValTop = (int) (Math.pow(distToMidTop, squareAmount) / halfTotalHeightSQ * (8 - minValTop)) + minValTop;

					float umin = 0;
					float umax = 16;
					float vmin = 0;
					float vmax = 16;

					float halfSize = (float) scaledValBottom / 16;
					float halfSizeTexW = halfSize * (umax - umin);
					float halfSize1 = (float) (scaledValTop) / 16;
					float halfSizeTex1 = halfSize1 * (umax - umin);

					StalactiteHelper core = StalactiteHelper.getValsFor(posX, posY, posZ);

					if(distDown == 0 && !noBottom) {
						core.bX = 0.5D;
						core.bZ = 0.5D;
					}
					if(distUp == 0 && !noTop) {
						core.tX = 0.5D;
						core.tZ = 0.5D;
					}
					
					QuadBuilder builder = new QuadBuilder(this.format);

					boolean hasTop = distUp == 0 && !noTop;
					boolean hasBottom = distDown == 0 && !noBottom;

					builder.setSprite(hasTop ? this.textureTop : hasBottom ? this.textureBottom : this.textureMiddle);

					// front
					builder.addVertex(core.bX - halfSize, 0, core.bZ - halfSize, umin + halfSizeTexW * 2, vmax);
					builder.addVertex(core.bX - halfSize, 0, core.bZ + halfSize, umin, vmax);
					builder.addVertex(core.tX - halfSize1, height, core.tZ + halfSize1, umin, vmin);
					builder.addVertex(core.tX - halfSize1, height, core.tZ - halfSize1, umin + halfSizeTex1 * 2, vmin);
					// back
					builder.addVertex(core.bX + halfSize, 0, core.bZ + halfSize, umin + halfSizeTexW * 2, vmax);
					builder.addVertex(core.bX + halfSize, 0, core.bZ - halfSize, umin, vmax);
					builder.addVertex(core.tX + halfSize1, height, core.tZ - halfSize1, umin, vmin);
					builder.addVertex(core.tX + halfSize1, height, core.tZ + halfSize1, umin + halfSizeTex1 * 2, vmin);
					// left
					builder.addVertex(core.bX + halfSize, 0, core.bZ - halfSize, umin + halfSizeTexW * 2, vmax);
					builder.addVertex(core.bX - halfSize, 0, core.bZ - halfSize, umin, vmax);
					builder.addVertex(core.tX - halfSize1, height, core.tZ - halfSize1, umin, vmin);
					builder.addVertex(core.tX + halfSize1, height, core.tZ - halfSize1, umin + halfSizeTex1 * 2, vmin);
					// right
					builder.addVertex(core.bX - halfSize, 0, core.bZ + halfSize, umin + halfSizeTexW * 2, vmax);
					builder.addVertex(core.bX + halfSize, 0, core.bZ + halfSize, umin, vmax);
					builder.addVertex(core.tX + halfSize1, height, core.tZ + halfSize1, umin, vmin);
					builder.addVertex(core.tX - halfSize1, height, core.tZ + halfSize1, umin + halfSizeTex1 * 2, vmin);

					// top
					if(distUp == 0) {
						builder.addVertex(core.tX - halfSize1, height, core.tZ - halfSize1, umin, vmin);
						builder.addVertex(core.tX - halfSize1, height, core.tZ + halfSize1, umin + halfSizeTex1 * 2, vmin);
						builder.addVertex(core.tX + halfSize1, height, core.tZ + halfSize1, umin + halfSizeTex1 * 2, vmin + halfSizeTex1 * 2);
						builder.addVertex(core.tX + halfSize1, height, core.tZ - halfSize1, umin, vmin + halfSizeTex1 * 2);
					}

					// bottom
					if(distDown == 0) {
						builder.addVertex(core.bX - halfSize, 0, core.bZ + halfSize, umin + halfSizeTexW * 2, vmin);
						builder.addVertex(core.bX - halfSize, 0, core.bZ - halfSize, umin, vmin);
						builder.addVertex(core.bX + halfSize, 0, core.bZ - halfSize, umin, vmin + halfSizeTexW * 2);
						builder.addVertex(core.bX + halfSize, 0, core.bZ + halfSize, umin + halfSizeTexW * 2, vmin + halfSizeTexW * 2);
					}

					quads = builder.build();
				} catch(Exception ex) {
					//throws inexplicable NPE when damaging block :(
				}
			}

			return quads;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return false;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return this.textureTop;
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return ItemCameraTransforms.DEFAULT;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}
	}
}