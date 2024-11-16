/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.command.commands.client.fakeplayer.FakePlayer;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKeepSprint;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiReducedDebugInfo;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoClip;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall;
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallNoGround;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleNoSlowBreak;
import net.ccbluex.liquidbounce.utils.aiming.AimPlan;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {

    @Shadow
    public abstract void tick();

    @Shadow
    public abstract SoundCategory getSoundCategory();

    /**
     * Hook player stride event
     */
    @ModifyVariable(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;strideDistance:F", shift = At.Shift.BEFORE, ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setMovementSpeed(F)V"), to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")), index = 1, ordinal = 0, require = 1, allow = 1)
    private float hookStrideForce(float strideForce) {
        final PlayerStrideEvent event = new PlayerStrideEvent(strideForce);
        EventManager.INSTANCE.callEvent(event);
        return event.getStrideForce();
    }

    /**
     * Hook safe walk event
     *
     * @return
     */
    @ModifyReturnValue(method = "clipAtLedge", at = @At("RETURN"))
    private boolean hookSafeWalk(boolean original) {
        final var event = EventManager.INSTANCE.callEvent(new PlayerSafeWalkEvent());
        return original || event.isSafeWalk();
    }

    /**
     * Hook velocity rotation modification
     * <p>
     * There are a few velocity changes when attacking an entity, which could be easily detected by anti-cheats when a different server-side rotation is applied.
     */
    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookFixRotation(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        AimPlan configurable = rotationManager.getWorkingAimPlan();

        if (configurable == null || !configurable.getApplyVelocityFix() || rotation == null) {
            return original;
        }

        return rotation.getYaw();
    }

    @Inject(method = "hasReducedDebugInfo", at = @At("HEAD"), cancellable = true)
    private void injectReducedDebugInfo(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (ModuleAntiReducedDebugInfo.INSTANCE.getEnabled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z",
            ordinal = 1,
            shift = At.Shift.BEFORE))
    private void hookNoClip(CallbackInfo ci) {
        var clip = ModuleNoClip.INSTANCE;
        if (!this.noClip && clip.getEnabled() && !clip.paused()) {
            this.noClip = true;
        }
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void hookJumpEvent(CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        final PlayerJumpEvent jumpEvent = new PlayerJumpEvent(getJumpVelocity());
        EventManager.INSTANCE.callEvent(jumpEvent);
        if (jumpEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean injectFatigueNoSlow(boolean original) {
        ModuleNoSlowBreak module = ModuleNoSlowBreak.INSTANCE;
        if ((Object) this == MinecraftClient.getInstance().player && module.getEnabled() && module.getMiningFatigue()) {
            return false;
        }

        return original;
    }


    @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean injectWaterNoSlow(boolean original) {
        ModuleNoSlowBreak module = ModuleNoSlowBreak.INSTANCE;
        if ((Object) this == MinecraftClient.getInstance().player && module.getEnabled() && module.getWater()) {
            return false;
        }

        return original;
    }

    @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isOnGround()Z"))
    private boolean injectOnAirNoSlow(boolean original) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            if (ModuleNoSlowBreak.INSTANCE.getEnabled() && ModuleNoSlowBreak.INSTANCE.getOnAir()){
                return true;
            }

            if (ModuleNoFall.INSTANCE.getEnabled() && NoFallNoGround.INSTANCE.isActive()) {
                return false;
            }

            if (ModuleCriticals.INSTANCE.getEnabled() && ModuleCriticals.NoGroundCrit.INSTANCE.isActive()) {
                return false;
            }
        }

        return original;
    }

    /**
     * Head rotations injection hook
     */
    @ModifyExpressionValue(method = "tickNewAi", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookHeadRotations(float original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        ModuleRotations rotations = ModuleRotations.INSTANCE;
        final var pitch = rotations.getRotationPitch();
        Rotation rotation = rotations.displayRotations();

        // Update pitch here
        pitch.key(pitch.valueFloat());
        pitch.value(rotation.getPitch());

        return rotations.shouldDisplayRotations() && rotations.getBodyParts().getHead() ? rotation.getYaw() : original;
    }

    @SuppressWarnings({"UnreachableCode", "ConstantValue"})
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookSlowVelocity(Vec3d instance, double x, double y, double z) {
        if ((Object) this == MinecraftClient.getInstance().player && ModuleKeepSprint.INSTANCE.getEnabled()) {
            x = z = ModuleKeepSprint.INSTANCE.getMotion();
        }

        return instance.multiply(x, y, z);
    }

    @WrapWithCondition(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", ordinal = 0))
    private boolean hookSlowVelocity(PlayerEntity instance, boolean b) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            return !ModuleKeepSprint.INSTANCE.getEnabled() || b;
        }

        return true;
    }

    @ModifyReturnValue(method = "getEntityInteractionRange", at = @At("RETURN"))
    private double hookEntityInteractionRange(double original) {
        if ((Object) this == MinecraftClient.getInstance().player && ModuleReach.INSTANCE.getEnabled()) {
            return ModuleReach.INSTANCE.getCombatReach();
        }

        return original;
    }

    @ModifyReturnValue(method = "getBlockInteractionRange", at = @At("RETURN"))
    private double hookBlockInteractionRange(double original) {
        if ((Object) this == MinecraftClient.getInstance().player && ModuleReach.INSTANCE.getEnabled()) {
            return ModuleReach.INSTANCE.getBlockReach();
        }

        return original;
    }

    @Inject(method = "equipStack", at = @At("HEAD"))
    private void hookPlayerEquipmentChange(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new PlayerEquipmentChangeEvent((PlayerEntity) (Object) this, slot, stack));
    }

    /*
     * Sadly, mixins don't allow capturing parameters when redirecting,
     * so there needs to be an extra injection for every sound.
     */

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 0))
    private void hookPlaySound(Entity target, CallbackInfo ci) {
        liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 1))
    private void hookPlaySound1(Entity target, CallbackInfo ci) {
        liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 2))
    private void hookPlaySound2(Entity target, CallbackInfo ci) {
        liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 3))
    private void hookPlaySound3(Entity target, CallbackInfo ci) {
        liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 4))
    private void hookPlaySound4(Entity target, CallbackInfo ci) {
        liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.ENTITY_PLAYER_ATTACK_WEAK);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", ordinal = 5))
    private void hookPlaySound5(Entity target, CallbackInfo ci) {
        liquid_bounce$playSoundIfFakePlayer(target, SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE);
    }

    /**
     * When the target is a fake player, this method will play a client side sound.
     */
    @Unique
    private void liquid_bounce$playSoundIfFakePlayer(Entity target, SoundEvent soundEvent) {
        if (target instanceof FakePlayer) {
            getWorld().playSound(PlayerEntity.class.cast(this), getX(), getY(), getZ(), soundEvent, getSoundCategory(), 1F, 1F);
        }
    }

}
