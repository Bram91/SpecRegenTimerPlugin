package com.bram91.specregen;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class SpecRegenTimerOverlay extends Overlay {
	private final Client client;
	private SpecRegenTimerPlugin plugin;
	private final TooltipManager tooltipManager;
	private final HashMap<String, Integer> weapons;

	@Inject
	private ItemManager itemManager;
	private double currentSpec;
	private int specCount;
	private int maxSpecs;
	private int remainingMinutes;
	private String remainingSecondsString;

	private static final List<Integer> graniteMauls = List.of(
		ItemID.GRANITE_MAUL_PLUS, ItemID.Placeholder.GRANITE_MAUL_PLUS,
		ItemID.GRANITE_MAUL_PRETTY_PLUS, ItemID.Placeholder.GRANITE_MAUL_PRETTY_PLUS
	);

	@Inject
	public SpecRegenTimerOverlay(Client client, SpecRegenTimerPlugin plugin, TooltipManager tooltipManager)
	{
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		this.client = client;
		this.plugin = plugin;
		this.weapons = Weapons.getWeapons();
	}

	@Override
	public Dimension render(Graphics2D graphics2D) {
		Widget widget = client.getWidget(InterfaceID.Orbs.ORB_SPECENERGY);
		if (widget == null)
		{
			return null;
		}
		if (widget.isHidden())
		{
			widget = client.getWidget(InterfaceID.OrbsNomap.ORB_SPECENERGY);
		}
		if (widget == null)
		{
			return null;
		}
		final Rectangle bounds = widget.getBounds();
		final Point mousePosition = client.getMouseCanvasPosition();
		if (bounds.contains(mousePosition.getX(), mousePosition.getY()))
		{
			String tooltip;
			if (currentSpec == 100 || specCount == maxSpecs)
			{
				tooltip = "Available special attacks: " + specCount;
			}
			else
			{
				tooltip = "Time Remaining till next spec: " + remainingMinutes + ":" + remainingSecondsString + "</br>" +
						"Available special attacks: " + specCount;
			}
			tooltipManager.add(new Tooltip(tooltip));

		}
		return null;
	}

	public void updateSpec(){
		//0.2% spec restore per tick
		if (client.getItemContainer(InventoryID.WORN) == null)
		{
			return;
		}
		currentSpec = Math.floor((client.getVarpValue(VarPlayerID.SA_ENERGY) / 10) + (plugin.getSpecialPercentage() * 10));
		int specTarget = 100;
		final Item[] items = client.getItemContainer(InventoryID.WORN).getItems();
		if (items.length <= EquipmentInventorySlot.WEAPON.getSlotIdx())
		{
			return;
		}
		final Item weapon = items[EquipmentInventorySlot.WEAPON.getSlotIdx()];
		final ItemComposition weaponComp = itemManager.getItemComposition(weapon.getId());

		String weaponName;
		//magic shortbow(i) had to be special
		if (weaponComp.getName().equalsIgnoreCase("magic shortbow (i)"))
		{
			weaponName = "magic shortbow (i)";
		}
		else
		{
			weaponName = weaponComp.getName().replaceAll("\\([^()]*\\)","").toLowerCase();
		}

		if (weapons.containsKey(weaponName))
		{
			if (weaponName.equals("granite maul") && graniteMauls.contains(weaponComp.getId()))
			{
				specTarget = 50;
			}
			else
			{
				specTarget = weapons.get(weaponName);
			}
		}
		specCount = (int) (currentSpec / specTarget);

		double remainingSpec = specTarget - (currentSpec % specTarget);

		int remainingTicks = (int) (remainingSpec / 0.2);
		int remainingTime = (int) (remainingTicks * 0.6);
		int remainingSeconds = remainingTime % 60;
		remainingMinutes = (int) Math.floor(remainingTime / 60);

		remainingSecondsString = remainingSeconds + "";
		if (remainingSeconds < 10)
		{
			remainingSecondsString = "0" + remainingSeconds;
		}

		maxSpecs = (int)Math.floor(100 / specTarget);
	}
}
