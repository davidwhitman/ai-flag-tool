package com.github.isturdy.aiflagtool

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.input.InputEventAPI
import data.scripts.util.MagicRender
import org.apache.log4j.Logger
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class AiFlagToolCombatPlugin : BaseEveryFrameCombatPlugin() {
    private var engine: CombatEngineAPI? = null
    private var lastFlags: List<ShipwideAIFlags.AIFlags> = listOf()

    companion object {
        val LOGGER: Logger = Global.getLogger(AiFlagToolCombatPlugin::class.java)

        // Maneuver target is handled specially
        val FLAGS = ShipwideAIFlags.AIFlags.values().filter { flag -> flag != ShipwideAIFlags.AIFlags.MANEUVER_TARGET }

        private val STATUS_KEY = Any()
        private val KEYS = mutableMapOf<String, Any>()
    }

    override fun init(engine: CombatEngineAPI?) {
        if (engine == null) return
        this.engine = engine
    }

    override fun advance(amount: Float, events: List<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        val engine = this.engine
        engine ?: return

        val ship = engine.playerShip
        if (ship.ai == null) return

        val flags = ship.aiFlags
        val setFlags = FLAGS.filter { flag -> flags.hasFlag(flag) }

        val newFlags = setFlags - lastFlags
        val droppedFlags = lastFlags - setFlags
        lastFlags = setFlags
        if (newFlags.isNotEmpty()) LOGGER.debug("New flags: ${newFlags.joinToString(", ") }")
        if (droppedFlags.isNotEmpty()) LOGGER.debug("Dropped flags: ${droppedFlags.joinToString(", ") }")

        for (flag in droppedFlags) {
            engine.addFloatingText(ship.location, flag.toString(), 15.0f, Color.RED, ship, 0.0f, 0.0f)
        }
        for (flag in newFlags) {
            engine.addFloatingText(ship.location, flag.toString(), 15.0f, Color.GREEN, ship, 0.0f, 0.0f)
        }

        val (flagsWithoutData, flagsWithData) = setFlags.partition { flag -> flags.getCustom(flag) == null }

        for (flag in flagsWithData) {
            LOGGER.debug("$flag: ${flags.getCustom(flag)}")
            val key = KEYS.getOrPut(flag.toString()) { Any() }
            engine.maintainStatusForPlayerShip(key, "", "AI Flag", "$flag: ${flags.getCustom(flag)}", false)
        }
        val flagString = flagsWithoutData.joinToString(", ")
        LOGGER.debug("Other set flags: $flagString")
        if (flagsWithoutData.isNotEmpty()) {
            engine.maintainStatusForPlayerShip(
                STATUS_KEY,
                "",
                "Other Flags",
                flagString,
                false
            )
        }

        val maneuverTarget = flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET)
        if (maneuverTarget != null && maneuverTarget is CombatEntityAPI) {
            MagicRender.singleframe(
                Global.getSettings().getSprite("graphics/warroom/waypoint.png"),
                maneuverTarget.location,
                Vector2f(64.0f, 64.0f),
                0.0f,
                Color.RED,
                false
            )
        } else if (maneuverTarget != null) {
            LOGGER.debug("MANEUVER_TARGET not a CombatEntityAPI: $maneuverTarget")
        }

        if (ship.mouseTarget != null) {
            MagicRender.singleframe(
                Global.getSettings().getSprite("graphics/warroom/waypoint.png"),
                ship.mouseTarget,
                Vector2f(32.0f, 32.0f),
                0.0f,
                Color.GREEN,
                false
            )
        }
    }
}