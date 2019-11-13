package com.neo.headhunter.manager.block;

import com.neo.headhunter.HeadHunter;
import com.neo.headhunter.config.ConfigAccessor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HeadBlockManager extends ConfigAccessor implements Listener {
	private static final List<Material> HEAD_MATERIALS = Arrays.asList(
			Material.CREEPER_HEAD,
			Material.DRAGON_HEAD,
			Material.PLAYER_HEAD,
			Material.ZOMBIE_HEAD,
			Material.SKELETON_SKULL,
			Material.WITHER_SKELETON_SKULL
	);
	
	private static final List<Material> HEAD_BLOCK_MATERIALS = Arrays.asList(
			Material.CREEPER_WALL_HEAD,
			Material.DRAGON_WALL_HEAD,
			Material.PLAYER_WALL_HEAD,
			Material.ZOMBIE_WALL_HEAD,
			Material.SKELETON_WALL_SKULL,
			Material.WITHER_SKELETON_WALL_SKULL
	);
	
	public HeadBlockManager(HeadHunter plugin) {
		super(plugin, true, "placed_heads.yml", "data");
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		ItemStack item = event.getItemInHand();
		if(isHead(item)) {
			SkullMeta meta = (SkullMeta) item.getItemMeta();
			if(meta != null) {
				double value = ((int) (getHeadStackValue(item) / item.getAmount() * 100)) / 100.0;
				String mobPath = plugin.getMobLibrary().getMobPath(item);
				String blockPath = getBlockPath(event.getBlock());
				if (mobPath == null) {
					OfflinePlayer owner = meta.getOwningPlayer();
					if(owner != null) {
						config.set(blockPath, "PLAYER " + owner.getUniqueId().toString() + " " + value);
						saveConfig();
					}
				} else {
					config.set(getBlockPath(event.getBlock()), mobPath.replace(".", ";") + " " + value);
					saveConfig();
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(isHeadBlock(block)) {
			String blockPath = getBlockPath(block);
			String headDataString = config.getString(blockPath);
			if(headDataString != null) {
				block.setType(Material.AIR);
				String[] headData = headDataString.split(" ");
				ItemStack head = null;
				double value = 0;
				if(headData.length == 3) {
					UUID uuid = UUID.fromString(headData[1]);
					value = Double.valueOf(headData[2]);
					head = plugin.getMobLibrary().getPlayerHead(Bukkit.getOfflinePlayer(uuid));
				} else if(headData.length == 2) {
					String mobPath = headData[0].replace(";", ".");
					value = Double.valueOf(headData[1]);
					head = plugin.getMobLibrary().getMobHead(mobPath);
				}
				head = plugin.getDropManager().formatHead(head, value);
				
				if(head != null) {
					if(event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE)
						block.getWorld().dropItemNaturally(block.getLocation(), head);
					config.set(blockPath, null);
					saveConfig();
				}
			}
		}
	}
	
	private String getBlockPath(Block block) {
		int chunkX = block.getX() >> 4, chunkZ = block.getZ() >> 4;
		int offsetX = block.getX() & 0xF, offsetZ = block.getZ() & 0xF;
		return String.format(block.getWorld().getUID().toString() + ".%H%H%H%H", chunkX, chunkZ, offsetX, offsetZ);
	}
	
	private boolean isHead(ItemStack head) {
		return head != null && HEAD_MATERIALS.contains(head.getType());
	}
	
	private boolean isHeadBlock(Block headBlock) {
		return headBlock != null && HEAD_MATERIALS.contains(headBlock.getType());
	}
	
	public double getHeadStackValue(ItemStack head) {
		if(isHead(head)) {
			SkullMeta meta = (SkullMeta) head.getItemMeta();
			if(meta != null) {
				List<String> lore = meta.getLore();
				if(lore != null && !lore.isEmpty()) {
					String priceString = lore.get(0);
					priceString = ChatColor.stripColor(priceString);
					priceString = priceString.replace("Sell Price: $", "");
					if(priceString.matches("\\d+([.]\\d+)?")) {
						return Double.valueOf(priceString) * head.getAmount();
					}
				}
			}
		}
		return 0;
	}
}