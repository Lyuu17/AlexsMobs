package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.util.TendonWhipUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityTendonSegment extends Entity {

    private static final TrackedData<Optional<UUID>> CREATOR_ID = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> FROM_ID = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_COUNT = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CURRENT_TARGET_ID = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> PROGRESS = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DAMAGE = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> RETRACTING = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_CLAW = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_GLINT = DataTracker.registerData(EntityTendonSegment.class, TrackedDataHandlerRegistry.BOOLEAN);
    private List<Entity> previouslyTouched = new ArrayList<>();
    private boolean hasTouched = false;
    private boolean hasChained = false;
    public float prevProgress = 0;
    public static final float MAX_EXTEND_TIME = 3F;

    public EntityTendonSegment(EntityType<?> type, World level) {
        super(type, level);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(CREATOR_ID, Optional.empty());
        this.dataTracker.startTracking(FROM_ID, -1);
        this.dataTracker.startTracking(TARGET_COUNT, 0);
        this.dataTracker.startTracking(CURRENT_TARGET_ID, -1);
        this.dataTracker.startTracking(PROGRESS, 0F);
        this.dataTracker.startTracking(DAMAGE, 5F);
        this.dataTracker.startTracking(RETRACTING, false);
        this.dataTracker.startTracking(HAS_CLAW, true);
        this.dataTracker.startTracking(HAS_GLINT, false);
    }

    @Override
    public void tick() {
        float progress = this.getProgress();
        this.prevProgress = progress;
        if(this.age < 1){
            onJoinWorld();
        }else if(this.age == 1){
            if(!this.getWorld().isClient){
                this.playSound(AMSoundRegistry.TENDON_WHIP.get(),1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }
        }
        super.tick();
        Entity creator = getCreatorEntity();
        Entity current = getToEntity();
        if(progress < MAX_EXTEND_TIME && !this.isRetracting()){
            this.setProgress(progress + 1);
        }
        if(progress > 0F && this.isRetracting()){
            this.setProgress(progress - 1);
        }
        if(progress == 0F && this.isRetracting()){
            Entity from = this.getFromEntity();
            if(from instanceof EntityTendonSegment tendonSegment){
                tendonSegment.setRetracting(true);
                updateLastTendon(tendonSegment);
            }else{
                updateLastTendon(null);
            }

            this.remove(RemovalReason.DISCARDED);
        }
        if (creator instanceof LivingEntity) {
            if (current != null) {
                var target = new Vec3d(current.getX(), current.getBodyY(0.4F), current.getZ());
                var lerp = target.subtract(this.getPos());
                this.setVelocity(lerp.multiply(0.5F));
                if(!this.getWorld().isClient){
                    if(!hasTouched && progress >= MAX_EXTEND_TIME){
                        hasTouched = true;
                        Entity entity = getCreatorEntity();
                        if(entity instanceof LivingEntity){
                            if(current != creator && current.damage(getDamageSources().mobProjectile(this, (LivingEntity)entity), (float) getDamageFor((LivingEntity)creator, (LivingEntity)entity))){
                                this.applyDamageEffects((LivingEntity) creator, entity);
                            }
                        }
                    }
                }
            }
        }
        Vec3d vector3d = this.getVelocity();
        if(!this.getWorld().isClient){
            if(!hasChained){
                if(this.getTargetsHit() > 3){
                    this.setRetracting(true);
                }else if(creator instanceof LivingEntity && this.getProgress() >= MAX_EXTEND_TIME) {
                    Entity closestValid = null;
                    for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(8.0D))) {
                        if (!entity.equals(creator) && !previouslyTouched.contains(entity) && isValidTarget((LivingEntity) creator, entity) && this.canSee(entity)) {
                            if (closestValid == null || this.distanceTo(entity) < this.distanceTo(closestValid)) {
                                closestValid = entity;
                            }
                        }
                    }
                    if(closestValid != null){
                        createChain(closestValid);
                        hasChained = true;
                    }else{
                        this.setRetracting(true);
                    }
                }
            }
        }
        final double d0 = this.getX() + vector3d.x;
        final double d1 = this.getY() + vector3d.y;
        final double d2 = this.getZ() + vector3d.z;
        this.setVelocity(vector3d.multiply(0.99F));
        this.setPos(d0, d1, d2);
    }

    private boolean canSee(Entity entity) {
        if (entity.getWorld() != this.getWorld()) {
            return false;
        } else {
            var vec3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
            var vec3d2 = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
            if (vec3d2.distanceTo(vec3d) > (double)128.0F) {
                return false;
            } else {
                return this.getWorld().raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() == HitResult.Type.MISS;
            }
        }
    }

    private boolean isValidTarget(LivingEntity creator, Entity entity) {
        if(!creator.isTeammate(entity) && !entity.isTeammate(creator) && entity instanceof MobEntity){
            return true;
        }
        return creator.getAttacking() != null && creator.getAttacking().getUuid().equals(entity.getUuid()) || creator.getAttacker() != null && creator.getAttacker().getUuid().equals(entity.getUuid());
    }

    private double getDamageFor(LivingEntity creator, LivingEntity entity) {
        ItemStack stack = creator.getStackInHand(Hand.MAIN_HAND).isOf(AMItemRegistry.TENDON_WHIP.get()) ? creator.getStackInHand(Hand.MAIN_HAND) : creator.getStackInHand(Hand.OFF_HAND);
        double dmg = this.getBaseDamage();
        if(stack.isOf(AMItemRegistry.TENDON_WHIP.get())){
            dmg += EnchantmentHelper.getAttackDamage(stack, entity.getGroup());
        }
        return dmg;
    }

    private double getDamageForItem(ItemStack itemStack) {
        var map = itemStack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (!map.isEmpty()) {
            double d = 0;
            for (var mod : map.get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                d += mod.getValue();
            }
            return d;
        }
        return 0;
    }

    private void updateLastTendon(EntityTendonSegment lastTendon){
        Entity creator = getCreatorEntity();
        if(creator == null){
            if (this.getCreatorEntityUUID() == null) {
                return;
            }

            creator = getWorld().getPlayerByUuid(this.getCreatorEntityUUID());
        }
        if(creator instanceof LivingEntity){
            TendonWhipUtil.setLastTendon((LivingEntity)creator, lastTendon);
        }
    }

    private void createChain(Entity closestValid) {
        this.dataTracker.set(HAS_CLAW, false);
        var child = AMEntityRegistry.TENDON_SEGMENT.get().create(this.getWorld());
        child.previouslyTouched = new ArrayList<>(previouslyTouched);
        child.previouslyTouched.add(closestValid);
        child.setCreatorEntityUUID(this.getCreatorEntityUUID());
        child.setFromEntityID(this.getId());
        child.setToEntityID(closestValid.getId());
        child.setPos(closestValid.getX(), closestValid.getBodyY(0.4F), closestValid.getZ());
        child.setTargetsHit(this.getTargetsHit() + 1);
        updateLastTendon(child);
        child.setHasGlint(this.hasGlint());
        this.getWorld().spawnEntity(child);
    }

    private void onJoinWorld(){
        Entity creator = getCreatorEntity();
        if(creator == null){
            creator = getWorld().getPlayerByUuid(this.getCreatorEntityUUID());
        }
        Entity prior = getFromEntity();
        if(creator instanceof PlayerEntity player){
            var stack = player.getStackInHand(Hand.MAIN_HAND).isOf(AMItemRegistry.TENDON_WHIP.get()) ? player.getStackInHand(Hand.MAIN_HAND) : player.getStackInHand(Hand.OFF_HAND);
            if(stack.isOf(AMItemRegistry.TENDON_WHIP.get())){
                this.setHasGlint(stack.hasGlint());
            }
            float dmg = 2;
            if(prior instanceof EntityTendonSegment){
                dmg = Math.max(((EntityTendonSegment)prior).getBaseDamage() - 1, 2);
            }else{
                dmg = (float)getDamageForItem(stack);
            }
            this.dataTracker.set(DAMAGE, dmg);
        }
    }

    private float getBaseDamage() {
        return this.dataTracker.get(DAMAGE);
    }

    public UUID getCreatorEntityUUID() {
        return this.dataTracker.get(CREATOR_ID).orElse(null);
    }

    public void setCreatorEntityUUID(UUID id) {
        this.dataTracker.set(CREATOR_ID, Optional.ofNullable(id));
    }

    public Entity getCreatorEntity() {
        UUID uuid = getCreatorEntityUUID();
        if(uuid != null && !this.getWorld().isClient){
            return ((ServerWorld) getWorld()).getEntity(uuid);
        }
        return null;
    }

    public int getFromEntityID() {
        return this.dataTracker.get(FROM_ID);
    }

    public void setFromEntityID(int id) {
        this.dataTracker.set(FROM_ID, id);
    }

    public Entity getFromEntity() {
        return getFromEntityID() == -1 ? null : this.getWorld().getEntityById(getFromEntityID());
    }

    public int getToEntityID() {
        return this.dataTracker.get(CURRENT_TARGET_ID);
    }

    public void setToEntityID(int id) {
        this.dataTracker.set(CURRENT_TARGET_ID, id);
    }

    public Entity getToEntity() {
        return getToEntityID() == -1 ? null : this.getWorld().getEntityById(getToEntityID());
    }

    public int getTargetsHit() {
        return this.dataTracker.get(TARGET_COUNT);
    }

    public void setTargetsHit(int i) {
        this.dataTracker.set(TARGET_COUNT, i);
    }

    public float getProgress() {
        return this.dataTracker.get(PROGRESS);
    }

    public void setProgress(float progress) {
        this.dataTracker.set(PROGRESS, progress);
    }

    public boolean isRetracting() {
        return this.dataTracker.get(RETRACTING);
    }

    public void setRetracting(boolean retract) {
        this.dataTracker.set(RETRACTING, retract);
    }

    public boolean hasGlint() {
        return this.dataTracker.get(HAS_GLINT);
    }

    public void setHasGlint(boolean glint) {
        this.dataTracker.set(HAS_GLINT, glint);
    }

    public boolean hasClaw() {
        return this.dataTracker.get(HAS_CLAW);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound p_20052_) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound p_20139_) {
    }

    public boolean isCreator(Entity mob) {
        return this.getCreatorEntityUUID() != null && mob.getUuid().equals(this.getCreatorEntityUUID());
    }
}
