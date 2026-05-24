package xyz.meowing.zen.events

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import xyz.meowing.zen.api.EntityDetection
import xyz.meowing.zen.api.ItemAbility
import xyz.meowing.zen.api.PartyTracker.PartyMember
import java.util.concurrent.atomic.AtomicLong

abstract class Event

abstract class CancellableEvent : Event() {
    private var cancelled = false
    fun cancel() { cancelled = true }
    fun isCancelled() = cancelled
}

/** Replaces net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType */
enum class HudElementType {
    ALL, EXPERIENCE, ARMOR, HEALTH, FOOD, AIR, HOTBAR, CROSSHAIRS, BOSSHEALTH,
    POTION, JUMP_BAR, PLAYER_LIST, TEXT, SUBTITLES, FPSOVERLAY, DEBUGOVERLAY, ITEM_NAME
}

/** Replaces net.minecraftforge.event.entity.player.PlayerInteractEvent.Action */
enum class InteractAction { RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK, RIGHT_CLICK_AIR }

class HurtCamEvent(val partialTicks: Float) : CancellableEvent()
class SidebarUpdateEvent(val lines: List<String>) : Event()
class TablistEvent(val packet: PlayerListS2CPacket) : Event()
class ItemTooltipEvent(val lines: MutableList<String>, val itemStack: ItemStack) : CancellableEvent()

abstract class InternalEvent {
    abstract class NeuAPI {
        class Load : Event()
    }

    abstract class GuiMouse {
        class Click(val mouseX: Int, val mouseY: Int, val button: Int) : CancellableEvent()
        class Release(val mouseX: Int, val mouseY: Int, val button: Int) : Event()
        class Scroll(val horizontal: Double, val vertical: Double) : Event()
        class Move(val mouseX: Int, val mouseY: Int) : Event()
    }

    class GuiKey(val keyName: String?, val key: Int, val character: Char, val scanCode: Int) : CancellableEvent()
}

abstract class MouseEvent {
    class Click(val mouseX: Double, val mouseY: Double, val button: Int) : Event()
    class Release(val mouseX: Double, val mouseY: Double, val button: Int) : Event()
    class Scroll(val horizontal: Double, val vertical: Double) : Event()
    class Move(val mouseX: Double, val mouseY: Double) : Event()
}

abstract class KeyEvent {
    class Press(val keyCode: Int) : Event()
    class Release(val keyCode: Int) : Event()
}

abstract class EntityEvent {
    class Join(val entity: Entity) : CancellableEvent()
    class Leave(val entity: Entity) : Event()
    class Attack(val entityPlayer: PlayerEntity, val target: Entity) : Event()
    class Metadata(val packet: EntityTrackerUpdateS2CPacket, val entity: Entity, val name: String) : CancellableEvent()
    class Spawn(val packet: EntitySpawnS2CPacket, val entity: Entity, val name: String) : CancellableEvent()
    class Interact(val action: InteractAction, val pos: BlockPos?) : Event()
    class ArrowHit(val shooterName: String, val hitEntity: Entity) : Event()
    class ItemToss(val stack: ItemStack) : CancellableEvent()
}

abstract class TickEvent {
    class Client : Event()
    class Server : Event()
}

abstract class RenderEvent {
    class World(val partialTicks: Float) : Event()
    /**
     * [model] removed – ModelBase was 1.8.9 only; access the model from the renderer if needed.
     */
    class EntityModel(
        val entity: LivingEntity,
        val limbSwing: Float, val limbSwingAmount: Float, val ageInTicks: Float,
        val headYaw: Float, val headPitch: Float, val scaleFactor: Float
    ) : Event()
    /** [scaledWidth]/[scaledHeight] replace the removed ScaledResolution */
    class Text(val partialTicks: Float, val scaledWidth: Int, val scaledHeight: Int) : CancellableEvent()
    class HUD(val elementType: HudElementType, val partialTicks: Float, val scaledWidth: Int, val scaledHeight: Int) : CancellableEvent()
    class BlockHighlight(val blockPos: BlockPos, val partialTicks: Float) : CancellableEvent()
    /** [entity] is the entity being teleported (was EnderTeleportEvent) */
    class EndermanTP(val entity: LivingEntity) : CancellableEvent()
    class GuardianLaser(val entity: Entity, val target: Entity) : Event()

    abstract class Entity {
        class Pre(val entity: LivingEntity, val x: Double, val y: Double, val z: Double) : CancellableEvent()
        class Post(val entity: LivingEntity, val x: Double, val y: Double, val z: Double) : CancellableEvent()
    }

    abstract class Player {
        class Pre(val player: PlayerEntity, val x: Double, val y: Double, val z: Double, val partialTicks: Float) : CancellableEvent()
        class Post(val player: PlayerEntity, val x: Double, val y: Double, val z: Double, val partialTicks: Float) : CancellableEvent()
    }
}

abstract class PartyEvent {
    class Changed(val type: PartyChangeType, val playerName: String? = null, val members: Map<String, PartyMember>) : Event()
}

enum class PartyChangeType {
    MEMBER_JOINED, MEMBER_LEFT, PLAYER_JOINED, PLAYER_LEFT, LEADER_CHANGED, DISBANDED, LIST, PARTY_FINDER
}

abstract class GuiEvent {
    class Open(val screen: Screen) : Event()
    class Close(val gui: HandledScreen<*>, val container: ScreenHandler) : CancellableEvent()
    class Click(val gui: Screen) : CancellableEvent()
    class Key(val gui: Screen) : CancellableEvent()
    class BackgroundDraw(val gui: Screen) : CancellableEvent()

    abstract class Mouse {
        class Press(val mouseX: Int, val mouseY: Int, val mouseButton: Int, val gui: HandledScreen<*>) : CancellableEvent()
        class Release(val mouseX: Int, val mouseY: Int, val mouseButton: Int, val gui: HandledScreen<*>) : CancellableEvent()
        class Move(val mouseX: Int, val mouseY: Int, val mouseButton: Int, val gui: HandledScreen<*>) : CancellableEvent()
        class Scroll(val mouseX: Int, val mouseY: Int, val scroll: Int, val gui: HandledScreen<*>) : CancellableEvent()
    }

    abstract class Slot {
        class Click(val slot: Slot?, val gui: HandledScreen<*>?, val container: ScreenHandler, val slotId: Int, val clickedButton: Int, val clickType: Int) : CancellableEvent()
        class RenderPre(val slot: Slot, val gui: HandledScreen<*>) : CancellableEvent()
        class RenderPost(val slot: Slot, val gui: HandledScreen<*>) : CancellableEvent()
    }
}

abstract class ChatEvent {
    /** [message] is the chat text; [overlay] is true for action-bar messages */
    class Receive(val message: Text, val overlay: Boolean) : CancellableEvent()
    class Send(val message: String, val chatUtils: Boolean) : CancellableEvent()
}

abstract class PacketEvent {
    class Received(val packet: Packet<*>) : CancellableEvent()
    class Sent(val packet: Packet<*>) : CancellableEvent()
    class ReceivedPost(val packet: Packet<*>) : Event()
    class SentPost(val packet: Packet<*>) : Event()
}

abstract class WorldEvent {
    class Change(val world: World) : Event() {
        companion object {
            private val lastChangeTime = AtomicLong(0L)
            private const val COOLDOWN_MS = 500L

            fun shouldPost(): Boolean {
                val currentTime = System.currentTimeMillis()
                val lastTime = lastChangeTime.get()
                if (currentTime - lastTime < COOLDOWN_MS) return false
                return lastChangeTime.compareAndSet(lastTime, currentTime)
            }
        }
    }
}

abstract class GameEvent {
    class Load : Event()
    class Unload : Event()
    class Disconnect : Event()
    /** [message] is the action bar text */
    class ActionBar(val message: Text) : CancellableEvent()
}

abstract class SkyblockEvent {
    abstract class Slayer {
        class Spawn(val entity: Entity, val entityID: Int, val packet: EntityTrackerUpdateS2CPacket) : Event()
        class Death(val entity: Entity, val entityID: Int) : Event()
        class Cleanup : Event()
        class Fail : Event()
        class QuestStart : Event()
    }

    class ItemAbilityUsed(val ability: ItemAbility.ItemAbility) : Event()
    class EntitySpawn(val skyblockMob: EntityDetection.SkyblockMob) : Event()
    class DamageSplash(val damage: Int, val originalName: String, val entityPos: Vec3d, val packet: EntitySpawnS2CPacket) : CancellableEvent()
}

abstract class AreaEvent {
    class Main(val area: String?) : Event()
    class Sub(val subarea: String?) : Event()
    class Skyblock(val newVal: Boolean) : Event()
}
