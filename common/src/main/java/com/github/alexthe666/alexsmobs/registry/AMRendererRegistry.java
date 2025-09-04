package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.renderer.*;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AMRendererRegistry {

    public static void registerEntityRenderers() {
        EntityRendererRegistry.register(AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE, RenderAlligatorSnappingTurtle::new);
        EntityRendererRegistry.register(AMEntityRegistry.ANACONDA, RenderAnaconda::new);
        EntityRendererRegistry.register(AMEntityRegistry.ANACONDA_PART, RenderAnacondaPart::new);
        EntityRendererRegistry.register(AMEntityRegistry.BUNFUNGUS, RenderBunfungus::new);
        EntityRendererRegistry.register(AMEntityRegistry.CACHALOT_WHALE, RenderCachalotWhale::new);
        EntityRendererRegistry.register(AMEntityRegistry.CACHALOT_ECHO, RenderCachalotEcho::new);
        EntityRendererRegistry.register(AMEntityRegistry.COMB_JELLY, RenderCombJelly::new);
        EntityRendererRegistry.register(AMEntityRegistry.CRIMSON_MOSQUITO, RenderCrimsonMosquito::new);
        EntityRendererRegistry.register(AMEntityRegistry.GIANT_SQUID, RenderGiantSquid::new);
        EntityRendererRegistry.register(AMEntityRegistry.HEMOLYMPH, RenderHemolymph::new);
        EntityRendererRegistry.register(AMEntityRegistry.LAVIATHAN, RenderLaviathan::new);
        EntityRendererRegistry.register(AMEntityRegistry.LOBSTER, RenderLobster::new);
        EntityRendererRegistry.register(AMEntityRegistry.MOSQUITO_SPIT, RenderMosquitoSpit::new);
        EntityRendererRegistry.register(AMEntityRegistry.MUNGUS, RenderMungus::new);
        EntityRendererRegistry.register(AMEntityRegistry.TERRAPIN, RenderTerrapin::new);
        EntityRendererRegistry.register(AMEntityRegistry.TRIOPS, RenderTriops::new);
        EntityRendererRegistry.register(AMEntityRegistry.WARPED_MOSCO, RenderWarpedMosco::new);
        EntityRendererRegistry.register(AMEntityRegistry.WARPED_TOAD, RenderWarpedToad::new);
    }
}
