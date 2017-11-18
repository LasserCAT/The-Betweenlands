package thebetweenlands.client.handler;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.BufferBuilder;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import thebetweenlands.api.capability.IDecayCapability;
import thebetweenlands.api.capability.IEquipmentCapability;
import thebetweenlands.api.herblore.aspect.Aspect;
import thebetweenlands.api.herblore.aspect.ItemAspectContainer;
import thebetweenlands.common.TheBetweenlands;
import thebetweenlands.common.capability.equipment.EnumEquipmentInventory;
import thebetweenlands.common.herblore.aspect.AspectManager;
import thebetweenlands.common.herblore.book.widgets.text.FormatTags;
import thebetweenlands.common.herblore.book.widgets.text.TextContainer;
import thebetweenlands.common.herblore.book.widgets.text.TextContainer.TextPage;
import thebetweenlands.common.herblore.book.widgets.text.TextContainer.TextSegment;
import thebetweenlands.common.registries.CapabilityRegistry;
import thebetweenlands.common.world.WorldProviderBetweenlands;
import thebetweenlands.common.world.storage.BetweenlandsWorldStorage;
import thebetweenlands.common.world.storage.location.LocationStorage;
import thebetweenlands.util.AspectIconRenderer;
import thebetweenlands.util.ColorUtils;
import thebetweenlands.util.config.ConfigHandler;

public class ScreenRenderHandler extends Gui {
	private ScreenRenderHandler() { }

	public static ScreenRenderHandler INSTANCE = new ScreenRenderHandler();

	private static final ResourceLocation DECAY_BAR_TEXTURE = new ResourceLocation("thebetweenlands:textures/gui/decay_bar.png");

	private Random random = new Random();
	private int updateCounter;

	private TextContainer titleContainer = null;
	private String currentLocation = "";
	private int titleTicks = 0;
	private int maxTitleTicks = 120;

	public static final ResourceLocation TITLE_TEXTURE = new ResourceLocation("thebetweenlands:textures/gui/location_title.png");

	public static List<LocationStorage> getVisibleLocations(Entity entity) {
		BetweenlandsWorldStorage worldStorage = BetweenlandsWorldStorage.forWorld(entity.world);
		return worldStorage.getLocalStorageHandler().getLocalStorages(LocationStorage.class, entity.posX, entity.posZ, location -> location.isInside(entity.getPositionEyes(1)) && location.isVisible(entity));
	}

	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if(event.phase == Phase.START && !Minecraft.getMinecraft().isGamePaused()) {
			this.updateCounter++;

			if(this.titleTicks > 0) {
				this.titleTicks--;
			}

			EntityPlayer player = Minecraft.getMinecraft().player;
			if(player != null && player.dimension == ConfigHandler.dimensionId) {
				String prevLocation = this.currentLocation;

				List<LocationStorage> locations = getVisibleLocations(player);
				if(locations.isEmpty()) {
					String location;
					if(player.posY < WorldProviderBetweenlands.CAVE_START - 10) {
						String strippedName = I18n.format("location.wilderness.name");
						if(strippedName.startsWith("translate:")) {
							int startIndex = strippedName.indexOf("translate:");
							strippedName = strippedName.substring(startIndex+1, strippedName.length());
						}
						if(this.currentLocation.equals(strippedName)) {
							prevLocation = "";
						}
						location = I18n.format("location.caverns.name");
					} else {
						String strippedName = I18n.format("location.caverns.name");
						if(strippedName.startsWith("translate:")) {
							int startIndex = strippedName.indexOf("translate:");
							strippedName = strippedName.substring(startIndex+1, strippedName.length());
						}
						if(this.currentLocation.equals(strippedName)) {
							prevLocation = "";
						}
						location = I18n.format("location.wilderness.name");
					}
					this.currentLocation = location;
				} else {
					LocationStorage highestLocation = null;
					for(LocationStorage storage : locations) {
						if(highestLocation == null || storage.getLayer() > highestLocation.getLayer())
							highestLocation = storage;
					}
					this.currentLocation = highestLocation.getLocalizedName();
				}

				if(this.currentLocation.length() > 0) {
					if(this.currentLocation.contains(":")) {
						int startIndex = this.currentLocation.indexOf(":");
						try {
							String ticks = this.currentLocation.substring(0, startIndex);
							this.maxTitleTicks = Integer.parseInt(ticks);
							this.currentLocation = this.currentLocation.substring(startIndex+1, this.currentLocation.length());
						} catch(Exception ex) {
							this.maxTitleTicks = 80;
						}
					}
					if(prevLocation != null && !prevLocation.equals(this.currentLocation)) {
						this.titleTicks = this.maxTitleTicks;
						this.titleContainer = new TextContainer(2048, 2048, this.currentLocation, TheBetweenlands.proxy.getCustomFontRenderer());
						this.titleContainer.setCurrentScale(2.0f).setCurrentColor(0xFFFFFFFF);
						this.titleContainer.registerTag(new FormatTags.TagNewLine());
						this.titleContainer.registerTag(new FormatTags.TagScale(2.0F));
						this.titleContainer.registerTag(new FormatTags.TagSimple("bold", TextFormatting.BOLD));
						this.titleContainer.registerTag(new FormatTags.TagSimple("obfuscated", TextFormatting.OBFUSCATED));
						this.titleContainer.registerTag(new FormatTags.TagSimple("italic", TextFormatting.ITALIC));
						this.titleContainer.registerTag(new FormatTags.TagSimple("strikethrough", TextFormatting.STRIKETHROUGH));
						this.titleContainer.registerTag(new FormatTags.TagSimple("underline", TextFormatting.UNDERLINE));
						try {
							this.titleContainer.parse();
						} catch (Exception e) {
							this.titleContainer = null;
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
			int width = event.getResolution().getScaledWidth();
			int height = event.getResolution().getScaledHeight();

			Minecraft mc = Minecraft.getMinecraft();
			EntityPlayer player = mc.player;

			if(player != null) {
				if (player.hasCapability(CapabilityRegistry.CAPABILITY_EQUIPMENT, null)) {
					IEquipmentCapability capability = player.getCapability(CapabilityRegistry.CAPABILITY_EQUIPMENT, null);

					int yOffset = 0;

					for(EnumEquipmentInventory type : EnumEquipmentInventory.values()) {
						IInventory inv = capability.getInventory(type);

						int posX = width / 2 + 93;
						int posY = height + yOffset - 19;

						boolean hadItem = false;

						for(int i = 0; i < inv.getSizeInventory(); i++) {
							ItemStack stack = inv.getStackInSlot(i);

							if(!stack.isEmpty()) {
								float scale = 1.0F;

								GlStateManager.pushMatrix();
								GlStateManager.translate(posX, posY, 0);
								GlStateManager.color(1, 1, 1, 1);
								GlStateManager.enableBlend();
								GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
								GlStateManager.scale(scale, scale, scale);

								mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
								mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, 0, 0, null);

								GlStateManager.disableAlpha();
								GlStateManager.disableRescaleNormal();
								GlStateManager.disableLighting();
								GlStateManager.color(1, 1, 1, 1);
								GlStateManager.enableBlend();
								GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
								GlStateManager.enableTexture2D();
								GlStateManager.color(1, 1, 1, 1);
								GlStateManager.popMatrix();

								posX += 8;

								hadItem = true;
							}
						}

						if(hadItem) {
							yOffset -= 13;
						}
					}
				}

				if (!player.isRiding() && player.hasCapability(CapabilityRegistry.CAPABILITY_DECAY, null)) {
					IDecayCapability capability = player.getCapability(CapabilityRegistry.CAPABILITY_DECAY, null);

					if(capability.isDecayEnabled()) {
						int startX = (width / 2) - (27 / 2) + 23;
						int startY = height - 49;

						//Erebus compatibility
						if (player.getEntityData().hasKey("antivenomDuration")) {
							int duration = player.getEntityData().getInteger("antivenomDuration");
							if (duration > 0) {
								startY -= 12;
							}
						}
						
						//TaN compatibility
						if(TheBetweenlands.isToughAsNailsModInstalled) {
							startY -= 10;
						}

						//Ridden entity hearts offset
						Entity ridingEntity = player.getRidingEntity();
						if(ridingEntity != null && ridingEntity instanceof EntityLivingBase) {
							EntityLivingBase riddenEntity = (EntityLivingBase)ridingEntity;
							float maxEntityHealth = riddenEntity.getMaxHealth();
							int maxHealthHearts = (int)(maxEntityHealth + 0.5F) / 2;
							if (maxHealthHearts > 30) {
								maxHealthHearts = 30;
							}
							int guiOffsetY = 0;
							while(maxHealthHearts > 0) {
								int renderedHearts = Math.min(maxHealthHearts, 10);
								maxHealthHearts -= renderedHearts;
								guiOffsetY -= 10;
							}
							startY += guiOffsetY + 10;
						}

						int decay = 20 - capability.getDecayStats().getDecayLevel();

						Minecraft.getMinecraft().getTextureManager().bindTexture(DECAY_BAR_TEXTURE);

						for (int i = 0; i < 10; i++) {
							int offsetY = player.isInsideOfMaterial(Material.WATER) ? -10 : 0;

							if (this.updateCounter % (decay * 3 + 1) == 0) 
								offsetY += this.random.nextInt(3) - 1;

							GlStateManager.enableBlend();
							GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
							GlStateManager.color(1, 1, 1, 1);

							drawTexturedModalRect(startX + 71 - i * 8, startY + offsetY, 18, 0, 9, 9);
							if (i * 2 + 1 < decay) 
								drawTexturedModalRect(startX + 71 - i * 8, startY + offsetY, 0, 0, 9, 9);

							if (i * 2 + 1 == decay) 
								drawTexturedModalRect(startX + 72 - i * 8, startY + offsetY, 9, 0, 9, 9);
						}
					}
				}
			}
		} else if(event.getType() == ElementType.TEXT) {
			if(this.titleTicks > 0 && this.titleContainer != null && !this.titleContainer.getPages().isEmpty()) {
				TextPage page = this.titleContainer.getPages().get(0);
				int width = event.getResolution().getScaledWidth();
				int height = event.getResolution().getScaledHeight();
				double strWidth = page.getTextWidth();
				double strHeight = page.getTextHeight();
				double strX = width / 2.0D - strWidth / 2.0F;
				double strY = height / 5.0D;
				GlStateManager.pushMatrix();
				GlStateManager.translate(strX, strY, 0);
				float fade = Math.min(1.0F, ((float)this.maxTitleTicks - (float)this.titleTicks) / Math.min(40.0F, this.maxTitleTicks - 5.0F) + 0.02F) - Math.max(0, (-this.titleTicks + 5) / 5.0F);
				GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
				GlStateManager.enableBlend();
				float averageScale = 0F;
				for(TextSegment segment : page.getSegments()) {
					GlStateManager.pushMatrix();
					GlStateManager.translate(segment.x, segment.y, 0.0D);
					GlStateManager.scale(segment.scale, segment.scale, 1.0F);
					float[] rgba = ColorUtils.getRGBA(segment.color);
					segment.font.drawString(segment.text, 0, 0, ColorUtils.toHex(rgba[0], rgba[1], rgba[2], rgba[3] * fade));
					GlStateManager.color(1, 1, 1, 1);
					GlStateManager.popMatrix();
					averageScale += segment.scale;
				}
				averageScale /= page.getSegments().size();
				GlStateManager.popMatrix();
				Minecraft.getMinecraft().renderEngine.bindTexture(TITLE_TEXTURE);
				GlStateManager.color(1, 1, 1, fade);
				GlStateManager.disableCull();
				double sidePadding = 6;
				double yOffset = 5;
				double sy = Math.ceil(strY + strHeight - yOffset * averageScale);
				double ey = Math.ceil(strY + strHeight + (-yOffset + 16) * averageScale);
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder buffer = tessellator.getBuffer();
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
				this.renderTexturedRect(buffer, strX - sidePadding*averageScale, sy, strX - sidePadding*averageScale + 9*averageScale, ey, 0, 9 / 128.0D, 0, 1);
				this.renderTexturedRect(buffer, strX - sidePadding*averageScale + 9*averageScale, sy, strX + strWidth / 2.0D - 6*averageScale, ey, 9 / 128.0D, 58 / 128.0D, 0, 1);
				this.renderTexturedRect(buffer, strX + strWidth / 2.0D - 6*averageScale, sy, strX + strWidth / 2.0D + 6*averageScale, ey, 58 / 128.0D, 70 / 128.0D, 0, 1);
				this.renderTexturedRect(buffer, strX + strWidth / 2.0D + 6*averageScale, sy, strX + strWidth + sidePadding*averageScale - 9*averageScale, ey, 70 / 128.0D, 119 / 128.0D, 0, 1);
				this.renderTexturedRect(buffer, strX + strWidth + sidePadding*averageScale - 9*averageScale, sy, strX + strWidth + sidePadding*averageScale, ey, 119 / 128.0D, 1, 0, 1);
				tessellator.draw();
				GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			}
		}
	}

	private void renderTexturedRect(BufferBuilder buffer, double x, double y, double x2, double y2, double umin, double umax, double vmin, double vmax) {
		buffer.pos(x, y2, 0.0D).tex(umin, vmax).endVertex();
		buffer.pos(x2, y2, 0.0D).tex(umax, vmax).endVertex();
		buffer.pos(x2, y, 0.0D).tex(umax, vmin).endVertex();
		buffer.pos(x, y, 0.0D).tex(umin, vmin).endVertex();
	}

	public static final DecimalFormat ASPECT_AMOUNT_FORMAT = new DecimalFormat("#.##");

	@SubscribeEvent
	public void onRenderScreen(DrawScreenEvent.Post event) {
		Minecraft mc = Minecraft.getMinecraft();
		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && mc.currentScreen instanceof GuiContainer && mc.player != null) {
			GuiContainer container = (GuiContainer) mc.currentScreen;

			//Render aspects tooltip
			Slot selectedSlot = container.getSlotUnderMouse();
			if(selectedSlot != null && selectedSlot.getHasStack()) {
				ScaledResolution resolution = new ScaledResolution(mc);
				FontRenderer fontRenderer = mc.fontRenderer;
				double mouseX = (Mouse.getX() * resolution.getScaledWidth_double()) / mc.displayWidth;
				double mouseY = resolution.getScaledHeight_double() - (Mouse.getY() * resolution.getScaledHeight_double()) / mc.displayHeight - 1;
				GlStateManager.pushMatrix();
				GlStateManager.translate(mouseX + 8, mouseY - 38, 500);
				int yOffset = 0;
				int width = 0;
				List<Aspect> aspects = ItemAspectContainer.fromItem(selectedSlot.getStack(), AspectManager.get(mc.world)).getAspects(mc.player);
				GlStateManager.enableTexture2D();
				GlStateManager.enableBlend();
				RenderHelper.disableStandardItemLighting();
				if(aspects != null && aspects.size() > 0) {
					for(Aspect aspect : aspects) {
						String aspectText = aspect.type.getName() + " (" + ASPECT_AMOUNT_FORMAT.format(aspect.getDisplayAmount()) + ")";
						String aspectTypeText = aspect.type.getType();
						GlStateManager.color(1, 1, 1, 1);
						fontRenderer.drawString(aspectText, 2 + 17, 2 + yOffset, 0xFFFFFFFF);
						fontRenderer.drawString(aspectTypeText, 2 + 17, 2 + 9 + yOffset, 0xFFFFFFFF);
						AspectIconRenderer.renderIcon(2, 2 + yOffset, 16, 16, aspect.type.getIcon());
						int entryWidth = Math.max(fontRenderer.getStringWidth(aspectText) + 19, fontRenderer.getStringWidth(aspectTypeText) + 19);
						if(entryWidth > width) {
							width = entryWidth;
						}
						yOffset -= 21;
					}
					GlStateManager.translate(0, 0, -10);
					Gui.drawRect(0, yOffset + 20, width + 1, 21, 0x90000000);
					Gui.drawRect(1, yOffset + 21, width, 20, 0xAA000000);
				}
				RenderHelper.enableGUIStandardItemLighting();
				GlStateManager.popMatrix();
				GlStateManager.enableTexture2D();
				GlStateManager.enableBlend();
				GlStateManager.color(1, 1, 1, 1);
			}
		}
	}
}
