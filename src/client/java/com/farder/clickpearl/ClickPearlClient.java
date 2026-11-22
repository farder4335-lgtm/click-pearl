package com.farder.clickpearl;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class ClickPearlClient implements ClientModInitializer {
	public static final String MOD_ID = "clickpearl";

	private static KeyMapping throwPearlKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		// Default: mouse side button (button 4). Rebind in Options > Controls > Key Binds.
		throwPearlKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + MOD_ID + ".throw_pearl",
				InputConstants.Type.MOUSE,
				GLFW.GLFW_MOUSE_BUTTON_4,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(ClickPearlClient::onTick);
	}

	private static void onTick(Minecraft mc) {
		boolean pressed = false;
		while (throwPearlKey.consumeClick()) {
			pressed = true;
		}

		LocalPlayer player = mc.player;
		MultiPlayerGameMode gameMode = mc.gameMode;
		if (!pressed || player == null || gameMode == null || mc.gui.screen() != null) {
			return;
		}

		Inventory inventory = player.getInventory();

		// 1) Ender pearl already in the main hand.
		if (player.getMainHandItem().is(Items.ENDER_PEARL)) {
			use(player, gameMode, InteractionHand.MAIN_HAND);
			return;
		}

		// 2) Search the hotbar: select the pearl slot, throw, then switch back.
		for (int slot = 0; slot < 9; slot++) {
			if (inventory.getItem(slot).is(Items.ENDER_PEARL)) {
				int previous = inventory.getSelectedSlot();
				inventory.setSelectedSlot(slot);
				use(player, gameMode, InteractionHand.MAIN_HAND);
				inventory.setSelectedSlot(previous);
				return;
			}
		}

		// 3) Offhand.
		if (player.getOffhandItem().is(Items.ENDER_PEARL)) {
			use(player, gameMode, InteractionHand.OFF_HAND);
			return;
		}

		player.sendSystemMessage(Component.translatable("message." + MOD_ID + ".no_pearl"));
	}

	private static void use(LocalPlayer player, MultiPlayerGameMode gameMode, InteractionHand hand) {
		// useItem syncs the selected slot to the server before sending the use packet.
		if (gameMode.useItem(player, hand).consumesAction()) {
			player.swing(hand);
		}
	}
}
