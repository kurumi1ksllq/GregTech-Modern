package com.gregtechceu.gtceu.common.commands;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.ui.ops.ComponentOps;
import com.gregtechceu.gtceu.client.renderdoc.RenderDoc;
import com.gregtechceu.gtceu.client.renderdoc.RenderdocScreen;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.SCPacketShareProspection;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.*;

public class GTClientCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("gtceu")
                .then(literal("share_prospection_data")
                        .then(argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    Collection<GameProfile> players = GameProfileArgument.getGameProfiles(ctx,
                                            "player");
                                    for (GameProfile player : players) {
                                        Thread sendThread = new Thread(new ProspectingShareTask(
                                                ctx.getSource().getPlayer().getUUID(), player.getId()));
                                        sendThread.start();
                                    }
                                    return 1;
                                }))));

        if (RenderDoc.isAvailable()) {
            dispatcher.register(literal("renderdoc").executes(context -> {
                Minecraft.getInstance().setScreen(new RenderdocScreen());
                return 1;
            }).then(literal("comment")
                    .then(argument("capture_index", IntegerArgumentType.integer(0))
                            .then(argument("comment", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        var capture = RenderDoc
                                                .getCapture(IntegerArgumentType.getInteger(context, "capture_index"));
                                        if (capture == null) {
                                            context.getSource().sendFailure(ComponentOps.concat(
                                                    Component.literal(GTCEu.NAME + " > "),
                                                    Component.translatable("no such capture")));
                                            return 0;
                                        }

                                        RenderDoc.setCaptureComments(capture,
                                                StringArgumentType.getString(context, "comment"));
                                        context.getSource().sendSuccess(() -> ComponentOps.concat(
                                                Component.literal(GTCEu.NAME + " > "),
                                                Component.literal("comment updated")),
                                                false);

                                        return 1;
                                    })))));
        }
    }

    private static class ProspectingShareTask implements Runnable {

        private final List<ClientCacheManager.ProspectionInfo> prospectionData;
        private final UUID sender;
        private final UUID reciever;

        public ProspectingShareTask(UUID sender, UUID reciever) {
            prospectionData = ClientCacheManager.getProspectionShareData();
            this.sender = sender;
            this.reciever = reciever;
        }

        @Override
        public void run() {
            boolean first = true;
            for (ClientCacheManager.ProspectionInfo info : prospectionData) {
                GTNetwork.NETWORK.sendToServer(new SCPacketShareProspection(sender, reciever, info.cacheName, info.key,
                        info.isDimCache, info.dim, info.data, first));
                first = false;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
