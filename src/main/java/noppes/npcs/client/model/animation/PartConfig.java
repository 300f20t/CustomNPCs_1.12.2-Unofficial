package noppes.npcs.client.model.animation;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.api.entity.data.IAnimationPart;
import noppes.npcs.util.ValueUtil;

public class PartConfig implements IAnimationPart {
	
	public float[] rotation, offset, scale;
	public int id;
	public boolean disable, show;
	public String name;
	
	public PartConfig() {
		this.id = 0;
		this.disable = false;
		this.show = true;
		this.name = "part_"+id;
		this.clear();
	}
	
	public PartConfig(int id) {
		this();
		this.id = id;
		this.name = "part_"+id;
	}

	@Override
	public void clear() {
		this.rotation = new float[] { 0.5f, 0.5f, 0.5f }; // 0.0 = 0; 1.0 = 360
		this.offset = new float[] { 0.5f, 0.5f, 0.5f }; // 0.0 = -5; 1.0 = 5
		this.scale = new float[] { 0.2f, 0.2f, 0.2f }; // 0.0 = 0; 1.0 = 5
	}

	@Override
	public float[] getRotation() { return new float[] { this.rotation[0] * 360.0f, this.rotation[1] * 360.0f, this.rotation[2] * 360.0f }; }

	@Override
	public float[] getOffset() { return new float[] { 10.0f * this.offset[0] - 5.0f, 10.0f * this.offset[1] - 5.0f, 10.0f * this.offset[2] - 5.0f }; }

	@Override
	public float[] getScale() { return new float[] { this.scale[0] * 5.0f, this.scale[1] * 5.0f, this.scale[2] * 5.0f }; }
	
	@Override
	public void setRotation(float x, float y, float z) {
		x %= 360.0f;
		y %= 360.0f;
		z %= 360.0f;
		if (x<0.0f) { x += 360.0f; }
		if (y<0.0f) { y += 360.0f; }
		if (z<0.0f) { z += 360.0f; }
		this.rotation[0] = ValueUtil.correctFloat(x / 360.0f, 0.0f, 1.0f);
		this.rotation[1] = ValueUtil.correctFloat(y / 360.0f, 0.0f, 1.0f);
		this.rotation[2] = ValueUtil.correctFloat(z / 360.0f, 0.0f, 1.0f);
	}
	
	@Override
	public void setOffset(float x, float y, float z) {
		x %= 5.0f;
		y %= 5.0f;
		z %= 5.0f;
		this.offset[0] = ValueUtil.correctFloat(x / 5.0f, 0.0f, 1.0f);
		this.offset[1] = ValueUtil.correctFloat(y / 5.0f, 0.0f, 1.0f);
		this.offset[2] = ValueUtil.correctFloat(z / 5.0f, 0.0f, 1.0f);
	}
	
	@Override
	public void setScale(float x, float y, float z) {
		x %= 5.0f;
		y %= 5.0f;
		z %= 5.0f;
		this.scale[0] = ValueUtil.correctFloat(x / 5.0f, 0.0f, 1.0f);
		this.scale[1] = ValueUtil.correctFloat(y / 5.0f, 0.0f, 1.0f);
		this.scale[2] = ValueUtil.correctFloat(z / 5.0f, 0.0f, 1.0f);
	}

	@Override
	public boolean isDisable() { return this.disable; }
	@Override
	public void setDisable(boolean bo) { this.disable = bo; }

	@Override
	public boolean isShow() { return this.show; }

	@Override
	public void setShow(boolean bo) { this.show = bo; }

	public void readNBT(NBTTagCompound compound) {
		for (int i=0; i<3; i++) {
			try { this.rotation[i] = ValueUtil.correctFloat(compound.getTagList("Rotation", 5).getFloatAt(i), -1.0f, 1.0f); } catch (Exception e) { }
			try { this.offset[i] = ValueUtil.correctFloat(compound.getTagList("Offset", 5).getFloatAt(i), -5.0f, 5.0f); } catch (Exception e) { }
			try { this.scale[i] = ValueUtil.correctFloat(compound.getTagList("Scale", 5).getFloatAt(i), 0.0f, 5.0f); } catch (Exception e) { }
		}
		this.id = compound.getInteger("Part");
		this.disable = compound.getBoolean("Disabled");
		if (compound.hasKey("Show", 1)) { this.show = compound.getBoolean("Show"); }
		this.name = compound.getString("Name");
	}
	
	public NBTTagCompound writeNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList listRot = new NBTTagList();
		NBTTagList listOff = new NBTTagList();
		NBTTagList listSc = new NBTTagList();
		for (int i=0; i<3; i++) {
			listRot.appendTag(new NBTTagFloat(this.rotation[i]));
			listOff.appendTag(new NBTTagFloat(this.offset[i]));
			listSc.appendTag(new NBTTagFloat(this.scale[i]));
		}
		compound.setTag("Rotation", listRot);
		compound.setTag("Offset", listOff);
		compound.setTag("Scale", listSc);
		compound.setInteger("Part", this.id);
		compound.setBoolean("Disabled", this.disable);
		compound.setBoolean("Show", this.show);
		compound.setString("Name", this.name);
		return compound;
	}

	public PartConfig copy() {
		PartConfig pc = new PartConfig();
		pc.readNBT(this.writeNBT());
		return pc;
	}
	
}
