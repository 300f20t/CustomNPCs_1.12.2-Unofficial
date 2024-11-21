package noppes.npcs.client.gui;

import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.handler.data.IBorder;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumAvailabilityRegion;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.Zone3D;

import java.util.*;

public class SubGuiNpcAvailabilityRegions
        extends SubGuiInterface
        implements ICustomScrollListener {

    private final Availability availability;
    private final Map<String, Integer> data = new TreeMap<>();
    private GuiCustomScroll scroll;
    private String select = "";

    public SubGuiNpcAvailabilityRegions(Availability availability) {
        setBackground("smallbg.png");
        xSize = 176;
        ySize = 222;
        closeOnEsc = true;

        this.availability = availability;
    }

    @Override
    public void buttonEvent(GuiNpcButton button) {
        switch (button.id) {
            case 0: {
                if (!data.containsKey(select)) { return; }
                IBorder region = BorderController.getInstance().getRegion(data.get(select));
                if (region == null) { return; }
                IPos iPos = region.getCenter();
                Client.sendData(EnumPacketServer.TeleportTo, region.getDimensionId(), (int) iPos.getX(), (int) iPos.getY(), (int) iPos.getZ());
                break;
            }
            case 1: {
                if (!data.containsKey(select)) { return; }
                int id = data.get(select);
                if (button.getValue() == 0) {
                    availability.regions.remove(id);
                } else {
                    if (!availability.regions.containsKey(id)) {
                        availability.regions.put(id, EnumAvailabilityRegion.Always);
                    } else {
                        availability.regions.put(id, EnumAvailabilityRegion.values()[button.getValue() - 1]);
                    }
                }
                initGui();
                break;
            }
            case 66: {
                this.close();
                break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!CustomNpcs.ShowDescriptions) {
            return;
        }
        if (this.getButton(66) != null && this.getButton(66).isMouseOver()) {
            this.setHoverText(new TextComponentTranslation("hover.back").getFormattedText());
        }
        if (this.hoverText != null) {
            this.drawHoveringText(Arrays.asList(this.hoverText), mouseX, mouseY, this.fontRenderer);
            this.hoverText = null;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = guiLeft + 5;
        int y = guiTop + 197;
        addButton(new GuiNpcButton(66, x, y, 70, 20, "gui.done"));

        int selID = -1;
        if (!select.isEmpty() && data.containsKey(select)) {
            selID = data.get(select);
        }
        data.clear();
        if (scroll == null) {
            (scroll = new GuiCustomScroll(this, 0)).setSize(168, 191);
        }
        List<String> list = new ArrayList<>();
        List<String> suffixes = new ArrayList<>();
        BorderController bData = BorderController.getInstance();
        char c = ((char) 167);
        scroll.hoversTexts = new String[bData.regions.size()][];
        Map<Integer, Map<Integer, Zone3D>> regionWorlds = new TreeMap<>();
        for (int id : bData.regions.keySet()) {
            Zone3D region = bData.regions.get(id);
            if (!regionWorlds.containsKey(region.getDimensionId())) { regionWorlds.put(region.getDimensionId(), new TreeMap<>()); }
            regionWorlds.get(region.getDimensionId()).put(id, region);
        }
        for (int worldID : regionWorlds.keySet()) {
            for (int id : regionWorlds.get(worldID).keySet()) {
                Zone3D region = regionWorlds.get(worldID).get(id);
                IPos iPos = region.getCenter();
                IWorld iWorld = Objects.requireNonNull(NpcAPI.Instance()).getIWorld(worldID);
                String name = new TextComponentTranslation(region.getName()).getFormattedText();
                scroll.hoversTexts[list.size()] = new String[] {
                        c + "7Name: " + c + "r" + name,
                        c + "7World ID: " + c + "6" + region.getDimensionId() + c + "7 - " + c + "r" + iWorld.getDimension().getName(),
                        c + "7Center in X:" + c + "e" + (int) iPos.getX() + c + "7, Y:" + c + "e" + (int) iPos.getY() + c + "7, Z:" + c + "e" + (int) iPos.getZ()
                };
                String key = id + " - " + name;
                if (availability.regions.containsKey(id)) {
                    System.out.println("CNPCs: "+id+"; "+availability.regions.get(id));
                    key = c + "r" + key;
                    if (availability.regions.get(id) == EnumAvailabilityRegion.Always) {
                        suffixes.add(c + "aA");
                    } else if (availability.regions.get(id) == EnumAvailabilityRegion.InSide) {
                        suffixes.add(c + "bIn");
                    } else {
                        suffixes.add(c + "dOut");
                    }
                } else {
                    key = c + "7" + key;
                    suffixes.add(c + "cN");
                }
                key = c + "7ID:" + key;
                data.put(key, id);
                if (select.isEmpty() || selID == id) {
                    select = key;
                }
                list.add(key);
            }
        }
        scroll.setListNotSorted(list);
        scroll.setSuffixes(suffixes);
        scroll.guiLeft = guiLeft + 4;
        scroll.guiTop = guiTop + 4;
        if (!select.isEmpty()) { scroll.setSelected(select); }
        addScroll(scroll);

        EnumAvailabilityRegion aData = null;
        if (!select.isEmpty() && data.containsKey(select) && availability.regions.containsKey(data.get(scroll.getSelected()))) {
            aData = availability.regions.get(data.get(scroll.getSelected()));
        }
        addButton(new GuiNpcButton(0, x += 73, y, 20, 20, "TP"));
        getButton(0).setEnabled(!select.isEmpty());
        addButton(new GuiNpcButton(1, x + 23, y, 70, 20, new String[] { "gui.disabled", "availability.always", "availability.inside", "availability.outside" }, aData == null ? 0 : aData.ordinal() + 1));
        getButton(1).setEnabled(!select.isEmpty());
    }

    @Override
    public void scrollClicked(int mouseX, int mouseY, int mouseButton, GuiCustomScroll scroll) {
        select = scroll.getSelected();
        initGui();
    }

    @Override
    public void scrollDoubleClicked(String select, GuiCustomScroll scroll) {

    }

}
