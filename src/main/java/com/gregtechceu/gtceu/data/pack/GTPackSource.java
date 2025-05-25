package com.gregtechceu.gtceu.data.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class GTPackSource implements RepositorySource {

    private final String name;
    private final PackType type;
    private final Pack.Position position;
    private final Pack.ResourcesSupplier resources;

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void loadPacks(Consumer<Pack> onLoad) {
        Pack.Info info = Pack.readPackInfo(name, resources);
        onLoad.accept(Pack.create(name,
                Component.literal(name),
                true,
                resources,
                info,
                type,
                position,
                true,
                PackSource.BUILT_IN));
    }
}
