package thebetweenlands.client.render.model.baked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import thebetweenlands.common.block.structure.BlockSlanted;
import thebetweenlands.util.QuadBuilder;

public class ModelSlant implements IModel {
	public final ResourceLocation textureSlant;
	public final ResourceLocation textureSide;
	public final ResourceLocation textureBase;

	public ModelSlant(ResourceLocation texture) {
		this(texture, texture, texture);
	}

	public ModelSlant(ResourceLocation textureSlant, ResourceLocation textureSide, ResourceLocation textureBase) {
		this.textureSlant = textureSlant == null ? TextureMap.LOCATION_MISSING_TEXTURE : textureSlant;
		this.textureSide = textureSide == null ? TextureMap.LOCATION_MISSING_TEXTURE : textureSide;
		this.textureBase = textureBase == null ? TextureMap.LOCATION_MISSING_TEXTURE : textureBase;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return ImmutableList.of(this.textureSlant, this.textureSide, this.textureBase);
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, java.util.function.Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		ImmutableMap<TransformType, TRSRTransformation> map = PerspectiveMapWrapper.getTransforms(state);
		return new ModelBakedSlant(state.apply(Optional.empty()), map, format, 
				bakedTextureGetter.apply(this.textureSlant), bakedTextureGetter.apply(this.textureSide), bakedTextureGetter.apply(this.textureBase));
	}

	@Override
	public IModelState getDefaultState() {
		return TRSRTransformation.identity();
	}

	@Override
	public IModel retexture(ImmutableMap<String, String> textures) {
		ResourceLocation slant = null;
		ResourceLocation side = null;
		ResourceLocation base = null;
		if(textures.containsKey("slant")) {
			slant = new ResourceLocation(textures.get("slant"));
		}
		if(textures.containsKey("side")) {
			side = new ResourceLocation(textures.get("side"));
		}
		if(textures.containsKey("base")) {
			base = new ResourceLocation(textures.get("base"));
		}
		if(slant != null || side != null || base != null) {
			return new ModelSlant(slant != null ? slant : this.textureSlant, side != null ? side : this.textureSide, base != null ? base : this.textureBase);
		}
		return this;
	}

	public static class ModelBakedSlant implements IBakedModel {
		private final VertexFormat format;
		private final TextureAtlasSprite textureSlant;
		private final TextureAtlasSprite textureSide;
		private final TextureAtlasSprite textureBase;
		private float slopeEdge = 1.0F / 16.0F * 3.0F;
		private final EnumMap<EnumFacing, List<BakedQuad>> faceQuads;
		private final List<BakedQuad> nonCulledQuads = new ArrayList<>();
		protected final TRSRTransformation transformation;
		protected final ImmutableMap<TransformType, TRSRTransformation> transforms;

		private ModelBakedSlant(Optional<TRSRTransformation> transformation, ImmutableMap<TransformType, TRSRTransformation> transforms, VertexFormat format, 
				TextureAtlasSprite textureSlant, TextureAtlasSprite textureSide, TextureAtlasSprite textureBase) {
			this.transformation = transformation.isPresent() ? transformation.get() : null;
			this.transforms = transforms;
			this.format = format;
			this.textureSlant = textureSlant;
			this.textureSide = textureSide;
			this.textureBase = textureBase;

			this.faceQuads = Maps.newEnumMap(EnumFacing.class);
			for(EnumFacing side : EnumFacing.values()) {
				this.faceQuads.put(side, ImmutableList.<BakedQuad>of());
			}
		}

		private ModelBakedSlant(Optional<TRSRTransformation> transformation, ImmutableMap<TransformType, TRSRTransformation> transforms, VertexFormat format, 
				TextureAtlasSprite textureSlant, TextureAtlasSprite textureSide, TextureAtlasSprite textureBase, Integer key) {
			this(transformation, transforms, format, textureSlant, textureSide, textureBase);

			boolean cornerNW = (key & 0x1) != 0;
			boolean cornerNE = (key & 0x2) != 0;
			boolean cornerSE = (key & 0x4) != 0;
			boolean cornerSW = (key & 0x8) != 0;
			boolean upsidedown = (key & 0x10) != 0;
			EnumFacing slantDir = EnumFacing.getHorizontal(key >> 5);

			float cornerHeightNW = cornerNW ? 1.0F : this.slopeEdge;
			float cornerHeightNE = cornerNE ? 1.0F : this.slopeEdge;
			float cornerHeightSE = cornerSE ? 1.0F : this.slopeEdge;
			float cornerHeightSW = cornerSW ? 1.0F : this.slopeEdge;

			QuadBuilder builder = new QuadBuilder(this.format).setTransformation(this.transformation);

			int[] slantTexU = new int[4];
			int[] slantTexV = new int[4];

			switch(slantDir) {
			default:
			case NORTH:
				slantTexU = new int[] {0, 0, 16, 16};
				slantTexV = new int[] {0, 16, 16, 0};
				break;
			case SOUTH:
				slantTexU = new int[] {16, 16, 0, 0};
				slantTexV = new int[] {16, 0, 0, 16};
				break;
			case EAST:
				slantTexU = new int[] {0, 16, 16, 0};
				slantTexV = new int[] {16, 16, 0, 0};
				break;
			case WEST:
				slantTexU = new int[] {16, 0, 0, 16};
				slantTexV = new int[] {0, 0, 16, 16};
				break;
			}

			if(!upsidedown) {
				builder.setSprite(this.textureSide);

				//z- face
				builder.addVertex(0, 0, 0.0001f, 16, 16);
				builder.addVertex(0, cornerHeightNW, 0.0001f, 16, 16-cornerHeightNW*16.0F);
				builder.addVertex(1, cornerHeightNE, 0.0001f, 0, 16-cornerHeightNE*16.0F);
				builder.addVertex(1, 0, 0.0001f, 0, 16);
				this.faceQuads.put(EnumFacing.NORTH, builder.build());

				//z+ face
				builder.addVertex(0, 0, 1, 0, 16);
				builder.addVertex(1, 0, 1, 16, 16);
				builder.addVertex(1, cornerHeightSE, 1, 16, 16-cornerHeightSE*16.0F);
				builder.addVertex(0, cornerHeightSW, 1, 0, 16-cornerHeightSW*16.0F);
				this.faceQuads.put(EnumFacing.SOUTH, builder.build());

				//x+ face
				builder.addVertex(1, 0, 0, 16, 16);
				builder.addVertex(1, cornerHeightNE, 0, 16, 16-cornerHeightNE*16.0F);
				builder.addVertex(1, cornerHeightSE, 1, 0, 16-cornerHeightSE*16.0F);
				builder.addVertex(1, 0, 1, 0, 16);
				this.faceQuads.put(EnumFacing.EAST, builder.build());

				//x- face
				builder.addVertex(0.0001f, 0, 0, 0, 16);
				builder.addVertex(0.0001f, 0, 1, 16, 16);
				builder.addVertex(0.0001f, cornerHeightSW, 1, 16, 16-cornerHeightSW*16.0F);
				builder.addVertex(0.0001f, cornerHeightNW, 0, 0, 16-cornerHeightNW*16.0F);
				this.faceQuads.put(EnumFacing.WEST, builder.build());

				//top face
				builder.setSprite(this.textureSlant);
				if(cornerNW && cornerNE && cornerSE) {
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);

					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
				} else if(cornerNE && cornerSE && cornerSW) {
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);

					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
				} else if(cornerSE && cornerSW && cornerNW) {
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);

					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
				} else if(cornerSW && cornerNW && cornerNE) {
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);

					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
				} else if(cornerNW && !cornerNE && !cornerSE && !cornerSW) {
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);

					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
				} else if(!cornerNW && cornerNE && !cornerSE && !cornerSW) {
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);

					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
				} else if(!cornerNW && !cornerNE && cornerSE && !cornerSW) {
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);

					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
				} else if(!cornerNW && !cornerNE && !cornerSE && cornerSW) {
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);

					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
				} else if(cornerNW && cornerSE && !cornerNE && !cornerSW) {
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);

					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
				} else if(!cornerNW && !cornerSE && cornerNE && cornerSW) {
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);

					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
				} else {
					//straight
					builder.addVertex(0, cornerHeightNW, 0, slantTexU[0], slantTexV[0]);
					builder.addVertex(0, cornerHeightSW, 1, slantTexU[1], slantTexV[1]);
					builder.addVertex(1, cornerHeightSE, 1, slantTexU[2], slantTexV[2]);
					builder.addVertex(1, cornerHeightNE, 0, slantTexU[3], slantTexV[3]);
				}
				this.nonCulledQuads.addAll(builder.build());

				//bottom face
				builder.setSprite(this.textureBase);
				builder.addVertex(0, 0, 0, 0, 0);
				builder.addVertex(1, 0, 0, 0, 16);
				builder.addVertex(1, 0, 1, 16, 16);
				builder.addVertex(0, 0, 1, 16, 0);
				this.faceQuads.put(EnumFacing.DOWN, builder.build());
			} else {
				builder.setSprite(this.textureSide);

				//z- face
				builder.addVertex(0, 1, 0.0001f, 16, 0);
				builder.addVertex(1, 1, 0.0001f, 0, 0);
				builder.addVertex(1, 1-cornerHeightNE, 0.0001f, 0, cornerHeightNE*16.0F);
				builder.addVertex(0, 1-cornerHeightNW, 0.0001f, 16, cornerHeightNW*16.0F);
				this.faceQuads.put(EnumFacing.NORTH, builder.build());

				//z+ face
				builder.addVertex(0, 1, 1, 0, 0);
				builder.addVertex(0, 1-cornerHeightSW, 1, 0, cornerHeightSW*16.0F);
				builder.addVertex(1, 1-cornerHeightSE, 1, 16, cornerHeightSE*16.0F);
				builder.addVertex(1, 1, 1, 16, 0);
				this.faceQuads.put(EnumFacing.SOUTH, builder.build());

				//x+ face
				builder.addVertex(1, 1, 0, 16, 0);
				builder.addVertex(1, 1, 1, 0, 0);
				builder.addVertex(1, 1-cornerHeightSE, 1, 0, cornerHeightSE*16.0F);
				builder.addVertex(1, 1-cornerHeightNE, 0, 16, cornerHeightNE*16.0F);
				this.faceQuads.put(EnumFacing.EAST, builder.build());

				//x- face
				builder.addVertex(0.0001f, 1, 0, 0, 0);
				builder.addVertex(0.0001f, 1-cornerHeightNW, 0, 0, cornerHeightNW*16.0F);
				builder.addVertex(0.0001f, 1-cornerHeightSW, 1, 16, cornerHeightSW*16.0F);
				builder.addVertex(0.0001f, 1, 1, 16, 0);
				this.faceQuads.put(EnumFacing.WEST, builder.build());

				//bottom face
				builder.setSprite(this.textureSlant);
				if(cornerNW && cornerNE && cornerSE) {
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);

					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
				} else if(cornerNE && cornerSE && cornerSW) {
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);

					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
				} else if(cornerSE && cornerSW && cornerNW) {
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);

					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
				} else if(cornerSW && cornerNW && cornerNE) {
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);

					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
				} else if(cornerNW && !cornerNE && !cornerSE && !cornerSW) {
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);

					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
				} else if(!cornerNW && cornerNE && !cornerSE && !cornerSW) {
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);

					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
				} else if(!cornerNW && !cornerNE && cornerSE && !cornerSW) {
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);

					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
				} else if(!cornerNW && !cornerNE && !cornerSE && cornerSW) {
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);

					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
				} else if(cornerNW && cornerSE && !cornerNE && !cornerSW) {
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);

					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
				} else if(!cornerNW && !cornerSE && cornerNE && cornerSW) {
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);

					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
				} else {
					//straight
					builder.addVertex(1, 1-cornerHeightNE, 0, slantTexU[3], 16-slantTexV[3]);
					builder.addVertex(1, 1-cornerHeightSE, 1, slantTexU[2], 16-slantTexV[2]);
					builder.addVertex(0, 1-cornerHeightSW, 1, slantTexU[1], 16-slantTexV[1]);
					builder.addVertex(0, 1-cornerHeightNW, 0, slantTexU[0], 16-slantTexV[0]);
				}
				this.nonCulledQuads.addAll(builder.build());

				//top face
				builder.setSprite(this.textureBase);
				builder.addVertex(0, 1, 0, 0, 0);
				builder.addVertex(0, 1, 1, 16, 0);
				builder.addVertex(1, 1, 1, 16, 16);
				builder.addVertex(1, 1, 0, 0, 16);
				this.faceQuads.put(EnumFacing.UP, builder.build());
			}
		}

		private final LoadingCache<Integer, ModelBakedSlant> modelCache = CacheBuilder.newBuilder().maximumSize(256).build(new CacheLoader<Integer, ModelBakedSlant>() {
			@Override
			public ModelBakedSlant load(Integer key) throws Exception {
				return new ModelBakedSlant(ModelBakedSlant.this.transformation != null ? Optional.of(ModelBakedSlant.this.transformation) : Optional.empty(), ModelBakedSlant.this.transforms, ModelBakedSlant.this.format, 
						ModelBakedSlant.this.textureSlant, ModelBakedSlant.this.textureSide, ModelBakedSlant.this.textureBase, key);
			}
		});

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			boolean upsidedown = state != null ? state.getValue(BlockSlanted.HALF) == EnumHalf.TOP : false;
			boolean cornerNW = false;
			boolean cornerNE = false;
			boolean cornerSE = false;
			boolean cornerSW = false;
			EnumFacing slantDir = EnumFacing.NORTH;

			if(state instanceof IExtendedBlockState) {
				IExtendedBlockState extendedState = (IExtendedBlockState) state;
				cornerNW = extendedState.getValue(BlockSlanted.CORNER_NORTH_WEST);
				cornerNE = extendedState.getValue(BlockSlanted.CORNER_NORTH_EAST);
				cornerSE = extendedState.getValue(BlockSlanted.CORNER_SOUTH_EAST);
				cornerSW = extendedState.getValue(BlockSlanted.CORNER_SOUTH_WEST);
				slantDir = state.getValue(BlockSlanted.FACING);
			} else {
				cornerNW = true;
				cornerNE = false;
				cornerSE = false;
				cornerSW = true;
				slantDir = EnumFacing.WEST;
			}

			int index = 0;
			if(cornerNW) index |= 0x1;
			if(cornerNE) index |= 0x2;
			if(cornerSE) index |= 0x4;
			if(cornerSW) index |= 0x8;
			if(upsidedown) index |= 0x10;
			index |= slantDir.getHorizontalIndex() << 5;

			ModelBakedSlant model = this.modelCache.getUnchecked(index);

			return side == null ? model.nonCulledQuads : model.faceQuads.get(side);
		}

		@Override
		public boolean isAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return true;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return this.textureSlant;
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return ItemCameraTransforms.DEFAULT;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
			return PerspectiveMapWrapper.handlePerspective(this, this.transforms, type);
		}
	}
}