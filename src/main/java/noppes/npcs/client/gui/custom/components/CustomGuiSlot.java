package noppes.npcs.client.gui.custom.components;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.gui.IItemSlot;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.containers.ContainerCustomGui;

public class CustomGuiSlot
extends Slot {
	
	public EntityPlayer player;
	public IItemSlot slot;
	
	/*
	 * Changed public CustomGuiSlot(IInventory inventoryIn, int index, IItemSlot
	 * slot, EntityPlayer player) { super(inventoryIn, index, slot.getPosX(),
	 * slot.getPosY()); this.player = player; this.slot = slot; }
	 */

	// New
	public CustomGuiSlot(IInventory inventoryIn, int index, IItemSlot slot, EntityPlayer player, int cx, int cy) {
		super(inventoryIn, index, slot.getPosX() + cx, slot.getPosY() + cy);
		this.player = player;
		this.slot = slot;
	}

	public void onSlotChanged() {
		if (!this.player.world.isRemote /* && this.getStack() != this.slot.getStack().getMCItemStack() */) {
			this.slot.setStack(NpcAPI.Instance().getIItemStack(this.getStack()));
			if (this.player.openContainer instanceof ContainerCustomGui) {
				IItemStack heldItem = NpcAPI.Instance().getIItemStack(this.player.inventory.getItemStack());
				EventHooks.onCustomGuiSlot((PlayerWrapper<?>) NpcAPI.Instance().getIEntity(this.player),
						((ContainerCustomGui) this.player.openContainer).customGui, this.getSlotIndex(),
						this.slot.getStack(), heldItem);
				// EventHooks.onCustomGuiSlot((PlayerWrapper<?>)NpcAPI.Instance().getIEntity(this.player),
				// ((ContainerCustomGui)this.player.openContainer).customGui,
				// this.getSlotIndex()); Changed
			}
		}
		super.onSlotChanged();
	}
	
}
