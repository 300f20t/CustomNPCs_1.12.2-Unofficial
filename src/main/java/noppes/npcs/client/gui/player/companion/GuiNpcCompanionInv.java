package noppes.npcs.client.gui.player.companion;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCCompanion;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;

import javax.annotation.Nonnull;

public class GuiNpcCompanionInv
extends GuiContainerNPCInterface {

	private final EntityNPCInterface npc;
	private final ResourceLocation resource;
	private final RoleCompanion role;

	public GuiNpcCompanionInv(EntityNPCInterface npc, ContainerNPCCompanion container) {
		super(npc, container);
		this.resource = new ResourceLocation(CustomNpcs.MODID, "textures/gui/companioninv.png");
		this.npc = npc;
		this.role = (RoleCompanion) npc.advanced.roleInterface;
		this.closeOnEsc = true;
		this.xSize = 171;
		this.ySize = 166;
	}

	public void actionPerformed(@Nonnull GuiButton guibutton) {
		super.actionPerformed(guibutton);
		int id = guibutton.id;
		if (id == 1) {
			CustomNpcs.proxy.openGui(this.npc, EnumGuiType.Companion);
		}
		if (id == 2) {
			CustomNpcs.proxy.openGui(this.npc, EnumGuiType.CompanionTalent);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int xMouse, int yMouse) {
		this.drawWorldBackground(0);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		this.mc.getTextureManager().bindTexture(this.resource);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
		this.mc.getTextureManager().bindTexture(GuiNPCInterface.RESOURCE_SLOT);
		if (this.role.getTalentLevel(EnumCompanionTalent.ARMOR) > 0) {
			for (int i = 0; i < 4; ++i) {
				this.drawTexturedModalRect(this.guiLeft + 5, this.guiTop + 7 + i * 18, 0, 0, 18, 18);
			}
		}
		if (this.role.getTalentLevel(EnumCompanionTalent.SWORD) > 0) {
			this.drawTexturedModalRect(this.guiLeft + 78, this.guiTop + 16, 0,
					(this.npc.inventory.weapons.get(0) == null) ? 18 : 0, 18, 18);
		}
        this.role.getTalentLevel(EnumCompanionTalent.RANGED);
        if (this.role.talents.containsKey(EnumCompanionTalent.INVENTORY)) {
			for (int size = (this.role.getTalentLevel(EnumCompanionTalent.INVENTORY) + 1) * 2, j = 0; j < size; ++j) {
				this.drawTexturedModalRect(this.guiLeft + 113 + j % 3 * 18, this.guiTop + 7 + j / 3 * 18, 0, 0, 18, 18);
			}
		}
	}

    @Override
	public void drawScreen(int i, int j, float f) {
		super.drawScreen(i, j, f);
		super.drawNpc(52, 70);
	}

	@Override
	public void initGui() {
		super.initGui();
		GuiNpcCompanionStats.addTopMenu(this.role, this, 3);
	}

}
