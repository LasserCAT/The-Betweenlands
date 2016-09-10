package thebetweenlands.client.render.entity;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import thebetweenlands.client.render.shader.LightSource;
import thebetweenlands.client.render.shader.ShaderHelper;
import thebetweenlands.common.entity.EntityShockwaveSwordItem;

public class RenderShockwaveSwordItem extends RenderEntityItem {
	public RenderShockwaveSwordItem(RenderManager renderManager, RenderItem renderItem) {
		super(renderManager, renderItem);
	}

	@Override
	public void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
		super.doRender(entity, x, y, z, entityYaw, partialTicks);

		EntityShockwaveSwordItem shockWaveSwordItem = (EntityShockwaveSwordItem) entity;

		if(ShaderHelper.INSTANCE.isWorldShaderActive()) {
			float waveProgress = shockWaveSwordItem.getWaveProgress(partialTicks);
			if(waveProgress < 40) {
				ShaderHelper.INSTANCE.addDynLight(new LightSource(entity.posX, entity.posY, entity.posZ, 
						waveProgress * waveProgress * waveProgress / 64000.0F * 30.0F,
						5.0f / 255.0f * 13.0F, 
						20.0f / 255.0f * 13.0F, 
						80.0f / 255.0f * 13.0F));
				ShaderHelper.INSTANCE.addDynLight(new LightSource(entity.posX, entity.posY, entity.posZ, 
						waveProgress * waveProgress * waveProgress / 64000.0F * 15.0F,
						5.0f / 255.0f * 13.0F, 
						20.0f / 255.0f * 13.0F, 
						80.0f / 255.0f * 13.0F));
				ShaderHelper.INSTANCE.addDynLight(new LightSource(entity.posX, entity.posY, entity.posZ, 
						waveProgress * waveProgress * waveProgress / 64000.0F * 8.0F,
						5.0f / 255.0f * 13.0F, 
						20.0f / 255.0f * 13.0F, 
						80.0f / 255.0f * 13.0F));
			} else {
				ShaderHelper.INSTANCE.addDynLight(new LightSource(entity.posX, entity.posY, entity.posZ, 
						(1.0F + (float)Math.sin((entity.ticksExisted + partialTicks) / 20.0F)) / 2.0F + 0.25F,
						5.0f / 255.0f * 13.0F, 
						20.0f / 255.0f * 13.0F, 
						80.0f / 255.0f * 13.0F));
				ShaderHelper.INSTANCE.addDynLight(new LightSource(entity.posX, entity.posY, entity.posZ, 
						(1.0F + (float)Math.sin((entity.ticksExisted + partialTicks) / 20.0F)) / 4.0F + 0.25F,
						-1.25F, 
						-1.25F, 
						-1.25F));
			}
		}
	}
}
