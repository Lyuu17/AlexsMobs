package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.renderer.*;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

@Environment(EnvType.CLIENT)
public class AMRendererRegistry {

    public static void registerEntityRenderers() {
        EntityRendererRegistry.register(AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE, RenderAlligatorSnappingTurtle::new);
        EntityRendererRegistry.register(AMEntityRegistry.ANACONDA, RenderAnaconda::new);
        EntityRendererRegistry.register(AMEntityRegistry.ANACONDA_PART, RenderAnacondaPart::new);
        EntityRendererRegistry.register(AMEntityRegistry.BALD_EAGLE, RenderBaldEagle::new);
        EntityRendererRegistry.register(AMEntityRegistry.BISON, RenderBison::new);
        EntityRendererRegistry.register(AMEntityRegistry.BLOBFISH, RenderBlobfish::new);
        EntityRendererRegistry.register(AMEntityRegistry.BLUE_JAY, RenderBlueJay::new);
        EntityRendererRegistry.register(AMEntityRegistry.BONE_SERPENT, RenderBoneSerpent::new);
        EntityRendererRegistry.register(AMEntityRegistry.BONE_SERPENT_PART, RenderBoneSerpentPart::new);
        EntityRendererRegistry.register(AMEntityRegistry.BUNFUNGUS, RenderBunfungus::new);
        EntityRendererRegistry.register(AMEntityRegistry.CACHALOT_WHALE, RenderCachalotWhale::new);
        EntityRendererRegistry.register(AMEntityRegistry.CACHALOT_ECHO, RenderCachalotEcho::new);
        EntityRendererRegistry.register(AMEntityRegistry.CAIMAN, RenderCaiman::new);
        EntityRendererRegistry.register(AMEntityRegistry.CENTIPEDE_BODY, RenderCentipedeBody::new);
        EntityRendererRegistry.register(AMEntityRegistry.CENTIPEDE_HEAD, RenderCentipedeHead::new);
        EntityRendererRegistry.register(AMEntityRegistry.CENTIPEDE_TAIL, RenderCentipedeTail::new);
        EntityRendererRegistry.register(AMEntityRegistry.COCKROACH, RenderCockroach::new);
        EntityRendererRegistry.register(AMEntityRegistry.COCKROACH_EGG, render -> new FlyingItemEntityRenderer<>(render, 1.0F, true));
        EntityRendererRegistry.register(AMEntityRegistry.COMB_JELLY, RenderCombJelly::new);
        EntityRendererRegistry.register(AMEntityRegistry.CRIMSON_MOSQUITO, RenderCrimsonMosquito::new);
        EntityRendererRegistry.register(AMEntityRegistry.CROCODILE, RenderCrocodile::new);
        EntityRendererRegistry.register(AMEntityRegistry.CROW, RenderCrow::new);
        EntityRendererRegistry.register(AMEntityRegistry.ELEPHANT, RenderElephant::new);
        EntityRendererRegistry.register(AMEntityRegistry.EMU, RenderEmu::new);
        EntityRendererRegistry.register(AMEntityRegistry.EMU_EGG, render -> new FlyingItemEntityRenderer<>(render, 1.0F, true));
        EntityRendererRegistry.register(AMEntityRegistry.ENDERIOPHAGE, RenderEnderiophage::new);
        EntityRendererRegistry.register(AMEntityRegistry.ENDERIOPHAGE_ROCKET, render -> new FlyingItemEntityRenderer<>(render, 0.75F, true));
        EntityRendererRegistry.register(AMEntityRegistry.FRILLED_SHARK, RenderFrilledShark::new);
        EntityRendererRegistry.register(AMEntityRegistry.FROSTSTALKER, RenderFroststalker::new);
        EntityRendererRegistry.register(AMEntityRegistry.GIANT_SQUID, RenderGiantSquid::new);
        EntityRendererRegistry.register(AMEntityRegistry.GRIZZLY_BEAR, RenderGrizzlyBear::new);
        EntityRendererRegistry.register(AMEntityRegistry.HEMOLYMPH, RenderHemolymph::new);
        EntityRendererRegistry.register(AMEntityRegistry.ICE_SHARD, RenderIceShard::new);
        EntityRendererRegistry.register(AMEntityRegistry.LAVIATHAN, RenderLaviathan::new);
        EntityRendererRegistry.register(AMEntityRegistry.LOBSTER, RenderLobster::new);
        EntityRendererRegistry.register(AMEntityRegistry.MIMIC_OCTOPUS, RenderMimicOctopus::new);
        EntityRendererRegistry.register(AMEntityRegistry.MOSQUITO_SPIT, RenderMosquitoSpit::new);
        EntityRendererRegistry.register(AMEntityRegistry.MUNGUS, RenderMungus::new);
        EntityRendererRegistry.register(AMEntityRegistry.ORCA, RenderOrca::new);
        EntityRendererRegistry.register(AMEntityRegistry.PLATYPUS, RenderPlatypus::new);
        EntityRendererRegistry.register(AMEntityRegistry.RACCOON, RenderRaccoon::new);
        EntityRendererRegistry.register(AMEntityRegistry.SOUL_VULTURE, RenderSoulVulture::new);
        EntityRendererRegistry.register(AMEntityRegistry.STRADDLEBOARD, RenderStraddleboard::new);
        EntityRendererRegistry.register(AMEntityRegistry.TERRAPIN, RenderTerrapin::new);
        EntityRendererRegistry.register(AMEntityRegistry.TOUCAN, RenderToucan::new);
        EntityRendererRegistry.register(AMEntityRegistry.TRIOPS, RenderTriops::new);
        EntityRendererRegistry.register(AMEntityRegistry.WARPED_MOSCO, RenderWarpedMosco::new);
        EntityRendererRegistry.register(AMEntityRegistry.WARPED_TOAD, RenderWarpedToad::new);
    }
}
