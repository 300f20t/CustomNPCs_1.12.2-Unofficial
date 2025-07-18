package noppes.npcs.schematics;

import java.util.*;

import net.minecraft.block.BlockBanner.BlockBannerStanding;
import net.minecraft.block.BlockLever.EnumOrientation;
import net.minecraft.block.BlockLog.EnumAxis;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockVine;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemBuilder;
import noppes.npcs.items.ItemPlacer;
import noppes.npcs.util.BuilderData;

public class SchematicWrapper {

	public static Entity rotatePos(Entity entity, int rotation, BlockPos pos, BlockPos offset) {
		if (entity == null) {
			return null;
		}
		double x, y, z;
		if (entity instanceof EntityHanging) {
			EntityHanging eh = (EntityHanging) entity;
			x = eh.posX;
			y = eh.posY - offset.getY();
			z = eh.posZ;
			eh.rotationYaw = (eh.rotationYaw + (float) rotation * 90.0f) % 360.0f;
			switch (rotation) {
				case 1:
                case 2:
					x += offset.getX() * -1.0d;
					z += -1.0d - offset.getZ();
					break;
                case 3:
					x += 1.0d + offset.getX() * -1.0d;
					z += -1.0d - offset.getZ();
					break;
				default:
					x -= offset.getX();
					z -= offset.getZ();
					break;
			}
			x += pos.getX();
			y += pos.getY();
			z += pos.getZ();
			for (int i = 0; i < rotation; i++) {
                assert eh.facingDirection != null;
                eh.facingDirection = eh.facingDirection.rotateY();
			}
			entity.setPosition(x, y, z);
			return entity;
		}
		x = entity.posX;
		y = entity.posY;
		z = entity.posZ;
		switch (rotation) {
		case 1:
			x = 1.0d + offset.getZ() - entity.posZ;
			z = 1.0d + entity.posX + offset.getX() * -1.0d;
			break;
		case 2:
			x = 1.0d + entity.posX * -1.0d + offset.getX();
			z = 1.0d + entity.posZ * -1.0d + offset.getZ();
			break;
		case 3:
			x = 1.0d + entity.posZ - offset.getZ();
			z = 1.0d + entity.posX * -1.0d + offset.getX();
			break;
		default:
			x += 1.0d - offset.getX();
			z += 1.0d - offset.getZ();
			break;
		}
		entity.rotationYaw = (entity.rotationYaw + (float) rotation * 90.0f) % 360.0f;
		entity.posX = x + pos.getX() + 0.5d;
		entity.posY = y + pos.getY();
		entity.posZ = z + pos.getZ() + 0.5d;
		if (entity instanceof EntityCreature) {
			((EntityCreature) entity).setHomePosAndDistance(entity.getPosition(), (int) ((EntityCreature) entity).getMaximumHomeDistance());
		}
		if (entity instanceof EntityNPCInterface) {
			((EntityNPCInterface) entity).ais.orientation = (((EntityNPCInterface) entity).ais.orientation + rotation * 90) % 360;
		}
		return entity;
	}
	@SuppressWarnings({ "unchecked" })
	public static IBlockState rotationState(IBlockState state, int rotation) {
		if (rotation == 0) {
			return state;
		}
		if (state.getBlock() instanceof BlockVine) {
			int d = state.getBlock().getMetaFromState(state);
			for (int i = 0; i < rotation; ++i) {
				switch (d) {
				case 1:
					d = 2;
					break;
				case 2:
					d = 4;
					break;
				case 4:
					d = 8;
					break;
				case 8:
					d = 1;
					break;
				default:
					break;
				}
			}
			return state.getBlock().getDefaultState().withProperty(BlockVine.SOUTH, (d & 1) > 0)
					.withProperty(BlockVine.WEST, (d & 2) > 0)
					.withProperty(BlockVine.NORTH, (d & 4) > 0)
					.withProperty(BlockVine.EAST, (d & 8) > 0);
		}
		Set<IProperty<?>> set = state.getProperties().keySet();
		for (@SuppressWarnings("rawtypes")
		IProperty prop : set) {
			if (prop.getValueClass() == EnumAxis.class) {
				EnumAxis d = (EnumAxis) state.getValue(prop);
				if (d == EnumAxis.Y || d == EnumAxis.NONE) {
					continue;
				}
				for (int i = 0; i < rotation; ++i) {
					d = (d == EnumAxis.X) ? EnumAxis.Z : EnumAxis.X;
				}
				return state.withProperty(prop, d);
			}
			if (prop.getValueClass() == EnumFacing.class) {
				EnumFacing d = (EnumFacing) state.getValue(prop);
				if (d == EnumFacing.UP || d == EnumFacing.DOWN) {
					continue;
				}
				for (int i = 0; i < rotation; ++i) {
					d = d.rotateY();
				}
				return state.withProperty(prop, d);
			}
			if (prop.getValueClass() == EnumRailDirection.class) {
				EnumRailDirection d = (EnumRailDirection) state.getValue(prop);
				for (int i = 0; i < rotation; ++i) {
					switch (d) {
					case NORTH_SOUTH: // 0 |
						d = EnumRailDirection.EAST_WEST;
						break;
					case EAST_WEST: // 1 |
						d = EnumRailDirection.NORTH_SOUTH;
						break;
					case ASCENDING_EAST: // 2 \
						d = EnumRailDirection.ASCENDING_SOUTH;
						break;
					case ASCENDING_WEST: // 3 \
						d = EnumRailDirection.ASCENDING_NORTH;
						break;
					case ASCENDING_NORTH: // 4 \
						d = EnumRailDirection.ASCENDING_EAST;
						break;
					case ASCENDING_SOUTH: // 5 \
						d = EnumRailDirection.ASCENDING_WEST;
						break;
					case SOUTH_EAST: // 6 L
						d = EnumRailDirection.SOUTH_WEST;
						break;
					case SOUTH_WEST: // 7 L
						d = EnumRailDirection.NORTH_WEST;
						break;
					case NORTH_WEST: // 8 L
						d = EnumRailDirection.NORTH_EAST;
						break;
					case NORTH_EAST: // 9 L
						d = EnumRailDirection.SOUTH_EAST;
						break;
					}
				}
				return state.withProperty(prop, d);
			}
			if (prop.getValueClass() == EnumOrientation.class) {
				EnumOrientation d = (EnumOrientation) state.getValue(prop);
				for (int i = 0; i < rotation; ++i) {
					switch (d) {
					case DOWN_X:
						d = EnumOrientation.DOWN_Z;
						break; // 0
					case DOWN_Z:
						d = EnumOrientation.DOWN_X;
						break; // 7
					case UP_X:
						d = EnumOrientation.UP_Z;
						break; // 6
					case UP_Z:
						d = EnumOrientation.UP_X;
						break; // 5
					case EAST:
						d = EnumOrientation.SOUTH;
						break; // 1
					case WEST:
						d = EnumOrientation.NORTH;
						break; // 2
					case SOUTH:
						d = EnumOrientation.WEST;
						break; // 3
					case NORTH:
						d = EnumOrientation.EAST;
						break; // 4
					}
				}
				return state.withProperty(prop, d);
			}
			if (prop.getValueClass() == Integer.class) {
				int d = (Integer) state.getValue(prop);
				if (state.getBlock() instanceof BlockBannerStanding || state.getBlock() instanceof BlockStandingSign) {
					d %= 16;
					for (int i = 0; i < rotation; ++i) {
						if (d <= 12) {
							d = 4 + d;
						} else {
							d = 4 + d - 16;
						}
					}
					return state.withProperty(prop, d % 16);
				}
			}
		}
		return state;
	}
	public boolean isBuilding, isBlock;
	public int layer, buildPos, size, rotation, buildingPercentage;
	private long time = 0L;
	public ISchematic schema;
	public BlockPos start;
	public Map<String, NBTTagCompound> tileEntities;

	public World world;
	public ICommandSender sender;
	private BuilderData builder;

	private List<SchematicBlockData> listB = new ArrayList<>();

	private List<Entity> listE = new ArrayList<>();

	public SchematicWrapper(ISchematic schematic) {
		this.start = BlockPos.ORIGIN;
		this.rotation = 0;
		this.isBuilding = false;
		this.layer = 0;
		this.isBlock = true;
		this.schema = schematic;
		this.size = schematic.getWidth() * schematic.getHeight() * schematic.getLength();
		this.tileEntities = new HashMap<>();
		this.sender = null;
		this.builder = null;
		for (int i = 0; i < schematic.getTileEntitySize(); ++i) {
			NBTTagCompound teTag = schematic.getTileEntity(i);
			this.tileEntities.put(teTag.getInteger("x") + "_" + teTag.getInteger("y") + "_" + teTag.getInteger("z"),
					teTag);
		}
	}

	public void build() {
		if (this.world == null || !this.isBuilding) {
			return;
		}
		long endPos = this.buildPos + CustomNpcs.MaxBuilderBlocks;
		if (endPos > this.size) {
			endPos = this.size;
		}
		// blocks first and next types
		if (this.layer < 2) {
			if (this.layer == 0 && this.builder != null) { // remove Entity
				this.listB = new ArrayList<>();
				this.listE = new ArrayList<>();
				BlockPos ps = this.start;
				BlockPos pe = this.start.add(this.rotation % 2 == 0 ? this.schema.getWidth() : this.schema.getLength(), this.schema.getHeight(), this.rotation % 2 == 0 ? this.schema.getLength() : this.schema.getWidth());
				List<Entity> list = new ArrayList<>();
				try {
					list = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(ps.getX() - 0.5d,
							ps.getY() - 0.5d, ps.getZ() - 0.5d, pe.getX() + 0.5d, pe.getY() + 0.5d, pe.getZ() + 0.5d));
				}
				catch (Exception ignored) { }
				for (Entity e : list) {
					if (e instanceof EntityThrowable || e instanceof EntityArrow || e instanceof EntityPlayer) {
						continue;
					}
					this.listE.add(e);
					e.isDead = true;
				}
			}
			long t = System.currentTimeMillis();
			while (this.buildPos < endPos) {
				int x = this.buildPos % this.schema.getWidth();
				int z = (this.buildPos - x) / this.schema.getWidth() % this.schema.getLength();
				int y = ((this.buildPos - x) / this.schema.getWidth() - z) / this.schema.getLength();
				SchematicBlockData sbd = this.place(x, y, z, this.layer == 0);
				if (sbd != null) {
					this.listB.add(sbd);
				}
				++this.buildPos;
			}
			this.time += System.currentTimeMillis() - t;
		}
		if (this.buildPos >= this.size) {
			switch (this.layer) {
			case 0: { // next blocks
				this.layer = 1;
				this.buildPos = 0;
				break;
			}
			case 1: { // entitys
				if (this.schema.hasEntitys()) {
					NBTTagList list = this.schema.getEntitys();
					for (int i = 0; i < list.tagCount(); i++) {
						this.spawn(list.getCompoundTagAt(i));
					}
				}
				this.layer = 2;
				SchematicController.time = this.time
						/ ((long) this.schema.getHeight() * this.schema.getLength() * this.schema.getWidth());
				this.time = 0L;
				break;
			}
			default: {
				this.layer = 3;
				this.isBuilding = false;
				if (this.builder != null) {
					this.builder.add(this.listB, this.listE);
				}
			}
			}
		}
	}

	public NBTTagCompound getNBTSmall() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setShort("Width", this.schema.getWidth());
		compound.setShort("Height", this.schema.getHeight());
		compound.setShort("Length", this.schema.getLength());
		compound.setString("SchematicName", this.schema.getName());
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < this.size && i < 25000; ++i) {
			IBlockState state = this.schema.getBlockState(i);
			if (state.getBlock() == Blocks.AIR || state.getBlock() == Blocks.STRUCTURE_VOID) {
				list.appendTag(new NBTTagCompound());
			} else {
				list.appendTag(NBTUtil.writeBlockState(new NBTTagCompound(), this.schema.getBlockState(i)));
			}
		}
		compound.setTag("Data", list);
		return compound;
	}

	public int getPercentage() {
		double l = this.buildPos + (this.layer == 0 ? 0 : this.size);
		return (int) (l / this.size * 50.0);
	}

	public NBTTagCompound getTileEntity(int x, int y, int z, BlockPos pos) {
		NBTTagCompound compound = this.tileEntities.get(x + "_" + y + "_" + z);
		if (compound == null) {
			return null;
		}
		compound = compound.copy();
		compound.setInteger("x", pos.getX());
		compound.setInteger("y", pos.getY());
		compound.setInteger("z", pos.getZ());
		return compound;
	}

	public void init(BlockPos pos, World world, int rotation) {
		this.start = pos;
		this.world = world;
		this.rotation = rotation;
		this.isBuilding = true;
		this.buildingPercentage = 0;
		this.layer = 0;
		this.isBlock = true;
		this.time = 0L;
	}

	/**
	 * place block in world
	 * 
	 * @param x,y,z
	 *            - BlockPos
	 * @param firstLayer
	 *            - not Air and FullBlock, next vice versa
	 */
	public SchematicBlockData place(int x, int y, int z, boolean firstLayer) {
		IBlockState state = this.schema.getBlockState(x, y, z);
		if (state == null || (firstLayer && !state.isFullBlock() && state.getBlock() != Blocks.AIR)
				|| (!firstLayer && (state.isFullBlock() || state.getBlock() == Blocks.AIR))) {
			return null;
		}
		int rotation = this.rotation / 90;
		BlockPos pos = this.start.add(this.rotatePos(x, y, z, rotation));
		SchematicBlockData sbd = new SchematicBlockData(this.world, this.world.getBlockState(pos), pos);
		state = SchematicWrapper.rotationState(state, rotation);
		if (this.builder != null) {
			if (state.getBlock() == Blocks.AIR && !this.builder.addAir) {
				return null;
			} // not place air
			if (sbd.state != null) {
				if (!this.builder.replaceAir && sbd.state.getBlock() != Blocks.AIR && sbd.state.getBlock().canSpawnInBlock()) {
					return null;
				} // not place solid
				@SuppressWarnings("deprecation")
				Material mat = sbd.state.getBlock().getMaterial(sbd.state);
				if (mat.isReplaceable() && this.builder.isSolid) {
					return null;
				} // not solid place
			}
		}
		this.world.setBlockState(pos, state, 2);
		if (state.getBlock() instanceof ITileEntityProvider) {
			TileEntity tile = this.world.getTileEntity(pos);
			if (tile != null) {
				NBTTagCompound comp = this.getTileEntity(x, y, z, pos);
				if (comp != null) {
					if (rotation != 0 && state.getBlock() instanceof BlockSkull && comp.hasKey("Rot", 1)) {
						byte d = comp.getByte("Rot");
						for (int i = 0; i < rotation; ++i) {
							d += (byte) 4;
						}
						d %= (byte) 16;
						comp.setByte("Rot", d);
					}
					tile.readFromNBT(comp);
				}
			}
		}
		this.world.setBlockState(pos, state, 2);
		return sbd;
	}

	public BlockPos rotatePos(int x, int y, int z, int rotation) {
		switch (rotation) {
		case 1:
			return new BlockPos(this.schema.getLength() - z - 1, y, x);
		case 2:
			return new BlockPos(this.schema.getWidth() - x - 1, y, this.schema.getLength() - z - 1);
		case 3:
			return new BlockPos(z, y, this.schema.getWidth() - x - 1);
		default:
			return new BlockPos(x, y, z);
		}
	}

	public void setBuilder(ICommandSender sender) {
		this.sender = sender;
		this.isBuilding = true;
		this.buildingPercentage = 0;
		this.isBlock = false;
		if (sender instanceof EntityPlayer && ((EntityPlayer) sender).getHeldItemMainhand().getItem() instanceof ItemPlacer) {
			this.builder = ItemBuilder.getBuilder(((EntityPlayer) sender).getHeldItemMainhand(), (EntityPlayer) sender);
		}
	}

	public void spawn(NBTTagCompound entityNbt) {
		Entity entity = EntityList.createEntityFromNBT(entityNbt, this.world);
		if (entity == null) {
			return;
		}
		UUID uuid = entity.getUniqueID();
		while (uuid != null) {
			boolean has = false;
			for (Entity e : this.world.loadedEntityList) {
				if (e.getUniqueID().equals(entity.getUniqueID())) {
					uuid = UUID.randomUUID();
					entity.setUniqueId(uuid);
					has = true;
					break;
				}
			}
			if (has) {
				continue;
			}
			uuid = null;
		}
		entity = SchematicWrapper.rotatePos(entity, this.rotation / 90, this.start, this.schema.getOffset());
		this.world.spawnEntity(entity);
		if (entity instanceof EntityNPCInterface) {
			((EntityNPCInterface) entity).reset(50);
		}
	}

}
