package thebetweenlands.manual.widgets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import thebetweenlands.herblore.aspects.IAspectType;
import thebetweenlands.manual.GuiManualBase;
import thebetweenlands.manual.ManualManager;
import thebetweenlands.manual.Page;
import thebetweenlands.manual.widgets.text.TextContainer;
import thebetweenlands.manual.widgets.text.TextContainer.TextPage;
import thebetweenlands.manual.widgets.text.TextFormatComponents;
import thebetweenlands.utils.AspectIconRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bart on 6-11-2015.
 */
public class ButtonWidget extends ManualWidgetsBase {
    private ArrayList<ItemStack> items = new ArrayList<>();
    private IAspectType aspect;
    public int pageNumber;
    private TextContainer textContainer;

    public int color = 0x808080;

    private ResourceLocation resourceLocation;
    private Page page;

    public boolean isHidden;

    public int width = 100;
    public int height = 16;
    int currentItem;
    boolean renderSomething = true;
    boolean doMathWithIndexPages = true;

    public ButtonWidget(int xStart, int yStart, Page page) {
        super(xStart, yStart);
        this.pageNumber = page.pageNumber;
        this.page = page;
        if (page.pageItems.size() > 0)
            this.items.addAll(page.pageItems);
        else if (page.pageAspect != null) {
            aspect = page.pageAspect;
        } else if (page.resourceLocation != null) {
            this.resourceLocation = new ResourceLocation(page.resourceLocation);
        }
        this.textContainer = new TextContainer(84, 22, page.pageName, false);

        this.isHidden = page.isHidden;
        this.init();
    }

    public ButtonWidget(int xStart, int yStart, int width, int height, int pageNumber, boolean doMathWithIndexPages) {
        super(xStart, yStart);
        this.width = width;
        this.height = height;
        this.pageNumber = pageNumber;
        renderSomething = false;
        this.doMathWithIndexPages = doMathWithIndexPages;
    }

    @Override
    public void init(GuiManualBase manual) {
        super.init(manual);
        if (isHidden)
            this.isHidden = !ManualManager.hasFoundPage(manual.player, page.unlocalizedPageName, manual.manualType);
    }


    @Override
    public void setPageToRight() {
        super.setPageToRight();
        if (renderSomething) {
            this.textContainer = new TextContainer(84, 22, page.pageName, false);
            this.init();
        }
    }

    public void init() {
        this.textContainer.setCurrentScale(1f).setCurrentColor(color).setCurrentFormat("");
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatScale(1.0F));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatColor(0x808080));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatTooltip("N/A"));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatSimple("bold", EnumChatFormatting.BOLD));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatSimple("obfuscated", EnumChatFormatting.OBFUSCATED));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatSimple("italic", EnumChatFormatting.ITALIC));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatSimple("strikethrough", EnumChatFormatting.STRIKETHROUGH));
        this.textContainer.registerFormat(new TextFormatComponents.TextFormatSimple("underline", EnumChatFormatting.UNDERLINE));

        try {
            this.textContainer.parse();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawForeGround() {
        if (renderSomething) {
            if (items.size() > 0)
                renderItem(xStart, yStart, items.get(currentItem), false, false);
            else if (aspect != null) {
                AspectIconRenderer.renderIcon(xStart, yStart, 16, 16, aspect.getIconIndex());
                if (mouseX >= xStart && mouseX <= xStart + 16 && mouseY >= yStart && mouseY <= yStart + 16) {
                    List<String> tooltipData = new ArrayList<>();
                    tooltipData.add(aspect.getName());
                    tooltipData.add(EnumChatFormatting.GRAY + aspect.getType());
                    renderTooltip(mouseX, mouseY, tooltipData, 0xffffff, 0xf0100010);
                }
            } else if (resourceLocation != null) {
                Minecraft.getMinecraft().renderEngine.bindTexture(resourceLocation);
                manual.drawTexture(xStart, yStart, 16, 16, page.textureWidth, page.textureHeight, page.xStartTexture, page.xEndTexture, page.yStartTexture, page.yEndTexture);
            }
            if (isHidden) {
                color = 0x666666;
            }

            TextPage page = this.textContainer.getPages().get(0);
            page.render(this.xStart + 18, this.yStart + 2);
        }
    }

    @Override
    public void mouseClicked(int x, int y, int mouseButton) {
        if (mouseButton == 2 && x >= xStart && x <= xStart + 16 && y >= yStart && y <= yStart + height) {
            if (currentItem + 1 < items.size()) {
                currentItem++;
            } else
                currentItem = 0;
            drawForeGround();
            manual.untilUpdate = 0;
        } else if (mouseButton == 0 && x >= xStart && x <= xStart + width && y >= yStart && y <= yStart + height && !isHidden) {
            manual.changeTo(pageNumber, doMathWithIndexPages);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (manual.untilUpdate % 20 == 0) {
            if (currentItem + 1 < items.size()) {
                currentItem++;
            } else
                currentItem = 0;
            drawForeGround();
        }
    }

    @Override
    public void resize() {
        super.resize();
        if (renderSomething) {
            this.textContainer = new TextContainer(84, 22, page.pageName, false);
            this.init();
        }
    }
}
