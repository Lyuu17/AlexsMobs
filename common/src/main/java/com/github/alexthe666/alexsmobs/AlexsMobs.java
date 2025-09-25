package com.github.alexthe666.alexsmobs;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil;
import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import com.github.alexthe666.alexsmobs.event.CommonEvents;
import com.github.alexthe666.alexsmobs.misc.CapsidRecipeManager;
import com.github.alexthe666.alexsmobs.registry.*;
import com.mrcrayfish.framework.FrameworkSetup;
import com.mrcrayfish.framework.entity.sync.SyncedEntityData;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.utils.GameInstance;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.Date;

public class AlexsMobs {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "alexsmobs";
    public static final String VERSION;
    private static boolean isAprilFools = false;
    private static boolean isHalloween = false;
    private static CapsidRecipeManager capsidRecipeManager;

    static {
        VERSION = Platform.getMod(AlexsMobs.MOD_ID).getVersion();
    }

    public static void init() {
        SyncedEntityData.instance().registerDataKey(FlyingFishBootsUtil.TAG);
        SyncedEntityData.instance().registerDataKey(RainbowUtil.TAG);
        SyncedEntityData.instance().registerDataKey(VineLassoUtil.TAG);
        FrameworkSetup.run();

        CommonEvents.init();

        AMAdvancementTriggerRegistry.init();
        AMBannerRegistry.DEF_REG.register();
        AMBlockRegistry.DEF_REG.register();
        AMBlockEntityRegistry.DEF_REG.register();
        AMEntityRegistry.DEF_REG.register();
        AMFeatureRegistry.DEF_REG.register();
        AMEffectRegistry.EFFECT_DEF_REG.register();
        AMEffectRegistry.POTION_DEF_REG.register();
        AMEnchantmentRegistry.DEF_REG.register();
        AMEntityRegistry.initializeAttributes();
        AMSoundRegistry.DEF_REG.register();
        AMItemRegistry.DEF_REG.register();
        AMItemRegistry.initDispenser();
        AMEffectRegistry.init();
        AMMenuRegistry.init(true);
        AMPacketRegistry.register();
        AMPaintingRegistry.DEF_REG.register();
        AMParticleRegistry.DEF_REG.register();
        AMParticleRegistry.init();
        AMPointOfInterestRegistry.DEF_REG.register();
        AMRecipeRegistry.DEF_REG.register();
        AMTradeRegistry.init();

//        IEventBus modBusEvent = FMLJavaModLoadingContext.get().getModEventBus();
//        modBusEvent.addListener(this::setup);
//        modBusEvent.addListener(this::setupClient);
//        modBusEvent.addListener(this::onModConfigEvent);
//        modBusEvent.addListener(this::setupEntityModelLayers);
//        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
//        final DeferredRegister<Codec<? extends BiomeModifier>> biomeModifiers = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AlexsMobs.MOD_ID);
//        biomeModifiers.register(modBusEvent);
//        biomeModifiers.register("am_mob_spawns", AMMobSpawnBiomeModifier::makeCodec);
//        biomeModifiers.register("am_leafcutter_ant_spawns", AMLeafcutterAntBiomeModifier::makeCodec);
//        final DeferredRegister<Codec<? extends StructureModifier>> structureModifiers = DeferredRegister.create(ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, AlexsMobs.MOD_ID);
//        structureModifiers.register(modBusEvent);
//        structureModifiers.register("am_structure_spawns", AMMobSpawnStructureModifier::makeCodec);
//        modLoadingContext.registerConfig(ModConfig.Type.COMMON, ConfigHolder.COMMON_SPEC, "alexsmobs.toml");
//        PROXY.init();
//        MinecraftForge.EVENT_BUS.register(this);
//        MinecraftForge.EVENT_BUS.register(new ServerEvents());
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA, AlexsMobs.getCapsidRecipeManager());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        isAprilFools = calendar.get(Calendar.MONTH) + 1 == 4 && calendar.get(Calendar.DATE) == 1;
        isHalloween = calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DATE) >= 29;
    }

    public static void process() {

    }

    public static boolean isAprilFools() {
        return isAprilFools || AMConfig.superSecretSettings;
    }

    public static boolean isHalloween() {
        return isHalloween || AMConfig.superSecretSettings;
    }

//    @SubscribeEvent
//    public void onModConfigEvent(final ModConfigEvent event) {
//        final ModConfig config = event.getConfig();
//        // Rebake the configs when they change
//        if (config.getSpec() == ConfigHolder.COMMON_SPEC) {
//            AMConfig.bake(config);
//        }
//        BiomeConfig.init();
//    }
//

    public static <MSG> void sendMSGToServer(MSG message) {
        AMPacketRegistry.CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendMSGToAll(MSG message) {
        var players = GameInstance.getServer().getPlayerManager().getPlayerList();
        for (var player : players) {
            sendNonLocal(message, player);
        }
    }

    public static <MSG> void sendNonLocal(MSG msg, ServerPlayerEntity player) {
        AMPacketRegistry.CHANNEL.sendToPlayer(player, msg);
    }

    public static CapsidRecipeManager getCapsidRecipeManager(){
        if(capsidRecipeManager == null){
            capsidRecipeManager = new CapsidRecipeManager();
        }
        return capsidRecipeManager;
    }

}
